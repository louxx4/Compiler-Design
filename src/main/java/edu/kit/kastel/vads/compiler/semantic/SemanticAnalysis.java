package edu.kit.kastel.vads.compiler.semantic;

import java.util.ArrayList;
import java.util.List;

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
        this.program.accept(new RecursivePostorderVisitor<>(
            new IntegerLiteralRangeAnalysis()), 
            new Namespace<>());

        //check for return
        this.program.accept(new RecursivePostorderVisitor<>(
            new ReturnAnalysis()), 
            new ArrayList<>());

        //check that step in for loop is no declaration
        this.program.accept(new RecursivePostorderVisitor<>(
            new ForLoopStepAnalysis()), 
            new ForLoopStepAnalysis.StepType());

        //check that all break/continue statements are inside of a loop
        this.program.accept(new RecursivePostorderVisitor<>(
            new BreakContinueAnalysis()), 
            new BreakContinueAnalysis.JumpUsage());

        //check variable initialization/declaration
        this.program.accept(new RecursivePostorderVisitor<>(
            new VariableStatusAnalysis()), 
            initializeNamespaces(
                VariableStatusAnalysis.getNamespaces(this.program.scopes().size()), 
                this.program.scopes()
            ));

        //check types
        this.program.accept(new RecursivePostorderVisitor<>(
            new TypeAnalysis()), 
            new TypeContext(
                initializeNamespaces(
                    TypeAnalysis.getNamespaces(this.program.scopes().size()), 
                    this.program.scopes()
                )
            )
        );
    }

    private static <T> Namespace<T>[] initializeNamespaces(Namespace<T>[] namespaces, List<Scope> scopes) {
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

}
