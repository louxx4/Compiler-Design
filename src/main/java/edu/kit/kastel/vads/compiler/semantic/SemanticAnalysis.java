package edu.kit.kastel.vads.compiler.semantic;

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
        //check variable scopes
        Namespace<VariableStatusAnalysis.VariableStatus>[] namespaces = new Namespace[this.program.namespaces()];
        this.program.accept(new RecursivePostorderVisitor<>(new VariableStatusAnalysis()), namespaces);
        //check for return
        this.program.accept(new RecursivePostorderVisitor<>(new ReturnAnalysis()), new ReturnAnalysis.ReturnState());
    }

}
