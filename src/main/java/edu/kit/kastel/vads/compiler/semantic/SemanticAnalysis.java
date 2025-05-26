package edu.kit.kastel.vads.compiler.semantic;

import java.util.HashMap;

import edu.kit.kastel.vads.compiler.parser.Scope;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.visitor.RecursivePostorderVisitor;

public class SemanticAnalysis {

    private final ProgramTree program;

    public SemanticAnalysis(ProgramTree program) {
        this.program = program;
    }

    public void analyze() {
        //check integer ranges
        this.program.accept(new RecursivePostorderVisitor<>(new IntegerLiteralRangeAnalysis()), new Namespace<>());

        //check for return
        this.program.accept(new RecursivePostorderVisitor<>(new ReturnAnalysis()), new ReturnAnalysis.ReturnState());

        //check variable scopes
        Namespace<VariableStatusAnalysis.VariableStatus>[] namespaces = new Namespace[this.program.scopes().size()];
        //initialization: create namespace for each scope
        for(Scope scope : this.program.scopes()) {
            namespaces[scope.getId()] = new Namespace<>();
        }
        //if present, set parent as enclosing namespace
        for(Scope scope : this.program.scopes()) {
            if(scope.hasParent()) namespaces[scope.getId()].
                setEnclosingNamespace(namespaces[scope.getParent().getId()]);
        }

        //check variable initialization/declaration
        this.program.accept(new RecursivePostorderVisitor<>(new VariableStatusAnalysis()), namespaces);

        //check types
        this.program.accept(new RecursivePostorderVisitor<>(new TypeAnalysis()), new TypeContext(this.program.scopes().size()));
    }

}
