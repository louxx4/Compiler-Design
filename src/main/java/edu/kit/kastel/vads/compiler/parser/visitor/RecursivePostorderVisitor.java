package edu.kit.kastel.vads.compiler.parser.visitor;

import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.ConditionalTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForLoopTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfStatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.JumpTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.LogicalOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateBWTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.NotTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileLoopTree;

/// A visitor that traverses a tree in postorder
/// @param <T> a type for additional data
/// @param <R> a type for a return type
public class RecursivePostorderVisitor<T, R> implements Visitor<T, R> {
    private final Visitor<T, R> visitor;

    public RecursivePostorderVisitor(Visitor<T, R> visitor) {
        this.visitor = visitor;
    }

    @Override
    public R visit(AssignmentTree assignmentTree, T data) {
        R r = assignmentTree.lValue().accept(this, data);
        r = assignmentTree.expression().accept(this, accumulate(data, r));
        r = this.visitor.visit(assignmentTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(BinaryOperationTree binaryOperationTree, T data) {
        R r = binaryOperationTree.lhs().accept(this, data);
        r = binaryOperationTree.rhs().accept(this, accumulate(data, r));
        r = this.visitor.visit(binaryOperationTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(BlockTree blockTree, T data) {
        R r;
        T d = data;
        for (StatementTree statement : blockTree.statements()) {
            r = statement.accept(this, d);
            d = accumulate(d, r);
        }
        r = this.visitor.visit(blockTree, d);
        return r;
    }
    
    @Override
    public R visit(BooleanTree booleanTree, T data) {
        return this.visitor.visit(booleanTree, data);
    }

    @Override
    public R visit(ConditionalTree conditionalTree, T data) {
        R r = conditionalTree.lhs().accept(this, data);
        r = conditionalTree.if_expression().accept(this, accumulate(data, r));
        r = conditionalTree.else_expression().accept(this, accumulate(data, r));
        r = this.visitor.visit(conditionalTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(DeclarationTree declarationTree, T data) {
        R r = declarationTree.type().accept(this, data);
        r = declarationTree.nameTree().accept(this, accumulate(data, r));
        if (declarationTree.initializer() != null) {
            r = declarationTree.initializer().accept(this, accumulate(data, r));
        }
        r = this.visitor.visit(declarationTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(ForLoopTree forLoopTree, T data) {
        R r = forLoopTree.initialization().accept(this, data);
        r = forLoopTree.condition().accept(visitor, accumulate(data, r));
        r = forLoopTree.advancement().accept(visitor, accumulate(data, r));
        r = forLoopTree.body().accept(visitor, accumulate(data, r));
        r = this.visitor.visit(forLoopTree, accumulate(data, r));
        return r;
    }    

    @Override
    public R visit(FunctionTree functionTree, T data) {
        R r = functionTree.returnType().accept(this, data);
        r = functionTree.nameTree().accept(this, accumulate(data, r));
        r = functionTree.body().accept(this, accumulate(data, r));
        r = this.visitor.visit(functionTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(IdentExpressionTree identExpressionTree, T data) {
        R r = identExpressionTree.nameTree().accept(this, data);
        r = this.visitor.visit(identExpressionTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(IfStatementTree ifStatementTree, T data) {
        R r = ifStatementTree.expression().accept(this, data);
        r = ifStatementTree.if_body().accept(visitor, accumulate(data, r));
        r = ifStatementTree.else_body().accept(visitor, accumulate(data, r));
        r = this.visitor.visit(ifStatementTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(JumpTree jumpTree, T data) {
        return this.visitor.visit(jumpTree, data);
    }

    @Override
    public R visit(LiteralTree literalTree, T data) {
        return this.visitor.visit(literalTree, data);
    }

    @Override
    public R visit(LogicalOperationTree logicalOperationTree, T data) {
        R r = logicalOperationTree.lhs().accept(this, data);
        r = logicalOperationTree.rhs().accept(this, accumulate(data, r));
        r = this.visitor.visit(logicalOperationTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(LValueIdentTree lValueIdentTree, T data) {
        R r = lValueIdentTree.name().accept(this, data);
        r = this.visitor.visit(lValueIdentTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(NameTree nameTree, T data) {
        return this.visitor.visit(nameTree, data);
    }

    @Override
    public R visit(NegateBWTree negateBWTree, T data) {
        R r = negateBWTree.expression().accept(this, data);
        r = this.visitor.visit(negateBWTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(NegateTree negateTree, T data) {
        R r = negateTree.expression().accept(this, data);
        r = this.visitor.visit(negateTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(NotTree notTree, T data) {
        R r = notTree.expression().accept(this, data);
        r = this.visitor.visit(notTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(ProgramTree programTree, T data) {
        R r;
        T d = data;
        for (FunctionTree tree : programTree.topLevelTrees()) {
            r = tree.accept(this, d);
            d = accumulate(data, r);
        }
        r = this.visitor.visit(programTree, d);
        return r;
    }

    @Override
    public R visit(ReturnTree returnTree, T data) {
        R r = returnTree.expression().accept(this, data);
        r = this.visitor.visit(returnTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(TypeTree typeTree, T data) {
        return this.visitor.visit(typeTree, data);
    }

    @Override
    public R visit(WhileLoopTree whileLoopTree, T data) {
        R r = whileLoopTree.expression().accept(this, data);
        r = whileLoopTree.statement().accept(visitor, accumulate(data, r));
        r = this.visitor.visit(whileLoopTree, accumulate(data, r));
        return r;
    }  

    protected T accumulate(T data, R value) {
        return data;
    }
}
