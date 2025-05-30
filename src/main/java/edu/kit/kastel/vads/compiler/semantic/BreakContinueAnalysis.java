package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.JumpTree;
import edu.kit.kastel.vads.compiler.parser.type.JumpType;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

// Checks whether all break/continue statements occur within a loop
public class BreakContinueAnalysis implements NoOpVisitor<BreakContinueAnalysis.JumpUsage> {

    public static class JumpUsage {
        boolean usageOutsideLoop = false;
        JumpType type;
        Span position;
    }

    @Override
    public Unit visit(JumpTree jumpTree, JumpUsage data) {
        //maintain first found illegal usage (don't overwrite it!)
        if(!data.usageOutsideLoop && isInvalidLoopId(jumpTree.loopId())) {
            data.usageOutsideLoop = true;
            data.type = jumpTree.type();
            data.position = jumpTree.span();
        }
        return NoOpVisitor.super.visit(jumpTree, data);
    }

    @Override
    public Unit visit(FunctionTree functionTree, JumpUsage data) {
        if(data.usageOutsideLoop) {
            throw new SemanticException("Function " + functionTree.nameTree().name() + " contains a " + 
                data.type.name() + "-statement outside of a loop (position: " + data.position + ")");
        }
        return NoOpVisitor.super.visit(functionTree, data);
    }

    private static boolean isInvalidLoopId(int id) {
        return id == -1;
    }

}