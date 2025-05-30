package edu.kit.kastel.vads.compiler.semantic;

import java.util.List;

import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfStatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

/// Checks that functions return.
class ReturnAnalysis implements NoOpVisitor<List<Tree>> {

    @Override
    public Unit visit(BlockTree blockTree, List<Tree> returningTrees) {
        for(StatementTree statement : blockTree.statements()) {
            if(returningTrees.contains(statement)) {
                returningTrees.add(blockTree);
                break;
            }
        }
        return NoOpVisitor.super.visit(blockTree, returningTrees);
    }
    
    @Override
    public Unit visit(IfStatementTree ifStatementTree, List<Tree> returningTrees) {
        if(returningTrees.contains(ifStatementTree.if_body()) && 
           returningTrees.contains(ifStatementTree.else_body())) {
            returningTrees.add(ifStatementTree);
        }
        return NoOpVisitor.super.visit(ifStatementTree, returningTrees);
    }

    @Override
    public Unit visit(ReturnTree returnTree, List<Tree> returningTrees) {
        returningTrees.add(returnTree);
        return NoOpVisitor.super.visit(returnTree, returningTrees);
    }

    @Override
    public Unit visit(FunctionTree functionTree, List<Tree> returningTrees) {
        if (!returningTrees.contains(functionTree.body())) {
            throw new SemanticException("function " + functionTree.nameTree().name().asString() + 
                " does not return");
        }
        returningTrees.add(functionTree);
        return NoOpVisitor.super.visit(functionTree, returningTrees);
    }
}
