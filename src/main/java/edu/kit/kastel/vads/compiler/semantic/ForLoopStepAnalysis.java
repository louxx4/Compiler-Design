package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForLoopTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

// Checks that the step/advancement part of a for loop is no declaration
public class ForLoopStepAnalysis implements NoOpVisitor<ForLoopStepAnalysis.StepType> {

    public static class StepType {
        boolean hasDeclarationStep = false;
        Span position;
    }

    @Override
    public Unit visit(ForLoopTree forLoopTree, StepType data) {
        if(forLoopTree.advancement() instanceof DeclarationTree) {
            data.hasDeclarationStep = true;
            data.position = forLoopTree.span();
        }
        return NoOpVisitor.super.visit(forLoopTree, data);
    }

    @Override
    public Unit visit(FunctionTree functionTree, StepType data) {
        if(data.hasDeclarationStep) {
            throw new SemanticException("Function " + functionTree.nameTree().name().asString() + 
                " contains a for loop whose step is a declaration (position: " + data.position + ")");
        }
        return NoOpVisitor.super.visit(functionTree, data);
    }

}