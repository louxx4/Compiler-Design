package edu.kit.kastel.vads.compiler.semantic;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.parser.Scope;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfStatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.JumpTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

// Checks that variables are
// - declared before assignment
// - not declared twice (within the same block)
// - not initialized twice
// - assigned before referenced
// (except they are "defined" by a return/break/continue statement)
// Status of variables are always updated locally (within the innermost namespace) first 
// and then propagated outside through visiting the enclosing trees.
// The status in enclosing namespaces needs to be updated, if and only if the variable 
//      (1) was declared before the execution of the inner block and 
//      (2) is initialized in all control paths within the inner block
class VariableStatusAnalysis implements NoOpVisitor<Namespace<VariableStatusAnalysis.VariableStatus>[]> {

    public static Namespace<VariableStatus>[] initializeNamespaces(List<Scope> scopes) {
        Namespace<VariableStatus>[] namespaces = new Namespace[scopes.size()];
        //initialization: create namespace for each scope
        for(Scope scope : scopes) {
            namespaces[scope.getId()] = new Namespace<>();
        }
        //if present, set parent as enclosing namespace
        for(Scope scope : scopes) {
            if(scope.hasParent()) namespaces[scope.getId()].
                setEnclosingNamespace(namespaces[scope.getParent().getId()]);
        }
        return namespaces;
    }
 
    @Override
    public Unit visit(AssignmentTree assignmentTree, Namespace<VariableStatus>[] data) {
        switch (assignmentTree.lValue()) {
            case LValueIdentTree(var nameTree) -> {
                int namespace = assignmentTree.block();
                VariableStatus status;
                if (assignmentTree.operator().type() == Operator.OperatorType.ASSIGN) {
                    status = checkDeclared(nameTree, data[namespace]);
                } else {
                    status = checkInitialized(nameTree, data[namespace]);
                }
                if (status != VariableStatus.INITIALIZED) {
                    // only update when needed, reassignment is totally fine
                    updateStatus(data, namespace, VariableStatus.INITIALIZED, nameTree.name());
                }
            }
        }
        return NoOpVisitor.super.visit(assignmentTree, data);
    }

    @Override
    public Unit visit(BlockTree blockTree, Namespace<VariableStatus>[] data) {
        // propagate initialized-status upwards (if variable is known outside this block)
        Namespace<VariableStatus> namespace = data[blockTree.block()];
        for(Name name : namespace.getKeys()) {
            if(namespace.getExclusive(name) == VariableStatus.INITIALIZED //see (2)
                && namespace.getFromEnclosing(name) == VariableStatus.DECLARED) { //see (1)
                updateStatus(namespace.getEnclosing(), VariableStatus.INITIALIZED, name);
            }
        }
        return NoOpVisitor.super.visit(blockTree, data);
    }

    @Override
    public Unit visit(DeclarationTree declarationTree, Namespace<VariableStatus>[] data) {
        int namespace = declarationTree.block();
        checkUndeclared(declarationTree.nameTree(), data[namespace]);
        VariableStatus status = declarationTree.initializer() == null
            ? VariableStatus.DECLARED
            : VariableStatus.INITIALIZED;
        updateStatus(data, namespace, status, declarationTree.nameTree().name());
        return NoOpVisitor.super.visit(declarationTree, data);
    }

    @Override
    public Unit visit(IdentExpressionTree identExpressionTree, Namespace<VariableStatus>[] data) {
        checkInitialized(identExpressionTree.nameTree(), data[identExpressionTree.block()]);
        return NoOpVisitor.super.visit(identExpressionTree, data);
    }

    @Override
    public Unit visit(IfStatementTree ifStatementTree, Namespace<VariableStatus>[] data) {
        if(IfStatementTree.isValidBlock(ifStatementTree.block_else())) {
            Namespace<VariableStatus> namespaceIf = data[ifStatementTree.block_if()];
            Namespace<VariableStatus> namespaceElse = data[ifStatementTree.block_else()];
            Namespace<VariableStatus> namespaceParent = namespaceIf.getEnclosing();
            Set<Name> varsDefinedInBothBlocks = Set.copyOf(namespaceIf.getKeys());
            varsDefinedInBothBlocks.retainAll(namespaceElse.getKeys());
            for(Name name : varsDefinedInBothBlocks) {
                if(namespaceIf.get(name)   == VariableStatus.INITIALIZED && 
                   namespaceElse.get(name) == VariableStatus.INITIALIZED &&
                   namespaceParent.get(name) != null) { // if null, variable is unknown outside the if-block
                    //variable is initialized in both blocks and was defined before
                    updateStatus(namespaceParent, VariableStatus.INITIALIZED, name);
                }
            }
        }
        return NoOpVisitor.super.visit(ifStatementTree, data);
    }

    @Override
    public Unit visit(JumpTree jumpTree, Namespace<VariableStatus>[] data) {
        defineAll(data[jumpTree.block()]);
        return NoOpVisitor.super.visit(jumpTree, data);
    }
 
    @Override
    public Unit visit(ReturnTree returnTree, Namespace<VariableStatus>[] data) {
        defineAll(data[returnTree.block()]);
        return NoOpVisitor.super.visit(returnTree, data);
    }

    // Sets all variables in the provided namespace to initialized
    private static void defineAll(Namespace<VariableStatus> namespace) {
        for(Name name : namespace.getKeys()) {
            //every usage afterwards is valid, so set all variables to "defined" 
            updateStatus(namespace, VariableStatus.INITIALIZED, name); 
        }
    }

    // Checks whether variable was already declared in the provided namespace or in one of 
    // its enclosing namespaces
    private static VariableStatus checkDeclared(NameTree name, Namespace<VariableStatus> namespace) {
        VariableStatus status = namespace.get(name);
        if (status == null) { // = defined in none of the enclosing namespaces
            throw new SemanticException("Variable " + name + " must be declared before assignment");
        }
        return status;
    }

    // Checks whether variable was already initialized in the provided namespace or in one of 
    // its enclosing namespaces
    private static VariableStatus checkInitialized(NameTree name, Namespace<VariableStatus> namespace) {
        VariableStatus status = namespace.get(name);
        if (status == null || status == VariableStatus.DECLARED) {
            throw new SemanticException("Variable " + name + " must be initialized before use");
        }
        return status;
    }

    private static void checkUndeclared(NameTree name, Namespace<VariableStatus> namespace) {
        if (namespace.get(name) != null) {
            throw new SemanticException("Variable " + name + " is already declared");
        }
    }

    // Updates the status of the provided variable in the namespace with the provided index
    private static void updateStatus(Namespace<VariableStatus>[] data, int namespaceId, 
        VariableStatus status, Name name) {
        updateStatus(data[namespaceId], status, name);
    }

    // Updates the status of the provided variable in the provided namespace
    private static void updateStatus(Namespace<VariableStatus> namespace,
        VariableStatus status, Name name) {
        putStatus(namespace, name, status);
    }

    private static void putStatus(Namespace<VariableStatus> namespace, Name name, VariableStatus status) {
        namespace.put(name, status, (existing, replacement) -> {
            if (existing.ordinal() >= replacement.ordinal()) {
                throw new SemanticException("namespace " + namespace + 
                    ": variable is already " + existing + ". Cannot be " + replacement + " here.");
            }
            return replacement;
        });
    }

    enum VariableStatus {
        DECLARED,
        INITIALIZED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
