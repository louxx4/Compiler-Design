package edu.kit.kastel.vads.compiler.semantic;

import java.util.List;

import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.ConditionalTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForLoopTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfStatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.LogicalOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateBWTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.NotTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileLoopTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

// Checks that the program has no type errors.
// Types of subtrees should never be null, since traversal of tree happens in postorder.
class TypeAnalysis implements NoOpVisitor<TypeContext> {

    enum TypeStatus {
        INT, BOOL, VALID
    }

    private static final List<OperatorType> INT_TO_BOOL_OPERATIONS = List.of(
        OperatorType.LESS, OperatorType.LEQ, OperatorType.GREATER, OperatorType.GEQ);

    @Override
    public Unit visit(AssignmentTree assignmentTree, TypeContext context) {
        TypeStatus t1 = context.get(assignmentTree.lValue());
        TypeStatus t2 = context.get(assignmentTree.expression());
        if(t1 != t2) signalMismatchingOperands(assignmentTree, t1, t2);
        context.put(assignmentTree, TypeStatus.VALID);
        return NoOpVisitor.super.visit(assignmentTree, context);
    }

    @Override
    public Unit visit(BinaryOperationTree binaryOperationTree, TypeContext context) {
        TypeStatus t1 = context.get(binaryOperationTree.lhs());
        if(t1 != TypeStatus.INT) signalInvalidOperand(binaryOperationTree, t1, TypeStatus.INT);
        TypeStatus t2 = context.get(binaryOperationTree.rhs());
        if(t2 != TypeStatus.INT) signalInvalidOperand(binaryOperationTree, t2, TypeStatus.INT);
        context.put(binaryOperationTree, TypeStatus.INT);
        return NoOpVisitor.super.visit(binaryOperationTree, context);
    }

    @Override
    public Unit visit(BlockTree blockTree, TypeContext context) {
        for(StatementTree statement : blockTree.statements()) {
            TypeStatus t = context.get(statement);
            if(t != TypeStatus.VALID) signalInvalidBlock(blockTree, t);
        }
        context.put(blockTree, TypeStatus.VALID);
        return NoOpVisitor.super.visit(blockTree, context);
    }

    @Override
    public Unit visit(BooleanTree booleanTree, TypeContext context) {
        context.put(booleanTree, TypeStatus.BOOL);
        return NoOpVisitor.super.visit(booleanTree, context);
    }
    
    @Override
    public Unit visit(ConditionalTree conditionalTree, TypeContext context) {
        TypeStatus t_expr = context.get(conditionalTree.lhs());
        if(t_expr != TypeStatus.BOOL) signalInvalidExpression(conditionalTree, t_expr, TypeStatus.BOOL);
        TypeStatus t1 = context.get(conditionalTree.if_expression());
        TypeStatus t2 = context.get(conditionalTree.else_expression());
        if(t1 != t2) signalMismatchingExpressions(conditionalTree, t1, t2);
        context.put(conditionalTree, t1); // t1 = t2
        return NoOpVisitor.super.visit(conditionalTree, context);
    }

    //Γ,𝑥 : 𝜏 ⊢ 𝑠 𝑣𝑎𝑙𝑖𝑑  is checked later on
    @Override
    public Unit visit(DeclarationTree declarationTree, TypeContext context) {
        TypeStatus t = context.get(declarationTree.type());
        Name name = declarationTree.name().name();
        int scope = declarationTree.block();
        TypeStatus t_name = context.get(name, scope); // 𝑥 : 𝜏′ ∉ Γ for any 𝜏′
        if(!(t_name == null || t_name == t)) signalMismatchingExpressions(declarationTree, t, t_name);
        context.put(name, t, scope); // x : 𝜏
        if(declarationTree.initializer() != null) {
            TypeStatus t_expr = context.get(declarationTree.initializer());
            if(t_expr != t) signalMismatchingOperands(declarationTree, t, t_expr);
        }
        context.put(declarationTree, TypeStatus.VALID);
        return NoOpVisitor.super.visit(declarationTree, context);
    }

    @Override
    public Unit visit(ForLoopTree forLoopTree, TypeContext context) {
        // initialization may be null
        if(forLoopTree.initialization() != null) {
            TypeStatus t_init = context.get(forLoopTree.initialization());
            if(t_init != TypeStatus.VALID) signalInvalidExpression(forLoopTree, t_init, TypeStatus.VALID);
        }
        // check condition & body
        TypeStatus t_c = context.get(forLoopTree.condition());
        if(t_c != TypeStatus.BOOL) signalInvalidExpression(forLoopTree, t_c, TypeStatus.BOOL);
        TypeStatus t_body = context.get(forLoopTree.condition());
        if(t_body != TypeStatus.VALID) signalInvalidExpression(forLoopTree, t_body, TypeStatus.VALID);
        // advancement may be null
        if(forLoopTree.advancement() != null) {
            TypeStatus t_adv = context.get(forLoopTree.advancement());
            if(t_adv != TypeStatus.VALID) signalInvalidExpression(forLoopTree, t_adv, TypeStatus.VALID);
        }
        context.put(forLoopTree, TypeStatus.VALID);
        return NoOpVisitor.super.visit(forLoopTree, context);
    }

    @Override
    public Unit visit(FunctionTree functionTree, TypeContext context) {
        TypeStatus t = context.get(functionTree.body());
        if(t != TypeStatus.VALID) signalInvalidFunction(functionTree, t);
        context.put(functionTree, TypeStatus.VALID);
        return NoOpVisitor.super.visit(functionTree, context);
    }
    
    @Override
    public Unit visit(IfStatementTree ifStatementTree, TypeContext context) {
        TypeStatus t_expr = context.get(ifStatementTree.expression());
        if(t_expr != TypeStatus.BOOL) signalInvalidOperand(ifStatementTree, t_expr, TypeStatus.BOOL);
        TypeStatus t1 = context.get(ifStatementTree.if_body());
        if(t1 != TypeStatus.VALID) signalInvalidExpression(ifStatementTree, t1, TypeStatus.VALID);
        TypeStatus t2 = context.get(ifStatementTree.else_body());
        if(t2 != TypeStatus.VALID) signalInvalidExpression(ifStatementTree, t2, TypeStatus.VALID);
        context.put(ifStatementTree, TypeStatus.VALID);
        return NoOpVisitor.super.visit(ifStatementTree, context);
    }

    @Override
    public Unit visit(LiteralTree literalTree, TypeContext context) {
        context.put(literalTree, TypeStatus.INT);
        return NoOpVisitor.super.visit(literalTree, context);
    }

    @Override
    public Unit visit(LogicalOperationTree logicalOperationTree, TypeContext context) {
        OperatorType operator = logicalOperationTree.operatorType();
        if (INT_TO_BOOL_OPERATIONS.contains(operator)) {
            TypeStatus t1 = context.get(logicalOperationTree.lhs());
            if(t1 != TypeStatus.INT) signalInvalidOperand(logicalOperationTree, t1, TypeStatus.INT);
            TypeStatus t2 = context.get(logicalOperationTree.rhs());
            if(t2 != TypeStatus.INT) signalInvalidOperand(logicalOperationTree, t2, TypeStatus.INT);
        } else { // RANDOMTYPE_TO_BOOL_OPERATIONS
            TypeStatus t1 = context.get(logicalOperationTree.lhs());
            TypeStatus t2 = context.get(logicalOperationTree.rhs());
            if(t1 != t2) signalMismatchingOperands(logicalOperationTree, t1, t2);
        }
        context.put(logicalOperationTree, TypeStatus.BOOL);
        return NoOpVisitor.super.visit(logicalOperationTree, context);
    }

    @Override
    public Unit visit(NegateBWTree negateBWTree, TypeContext context) {
        TypeStatus t = context.get(negateBWTree.expression());
        if(t != TypeStatus.INT) signalInvalidOperand(negateBWTree, t, TypeStatus.INT);
        context.put(negateBWTree, TypeStatus.INT);
        return NoOpVisitor.super.visit(negateBWTree, context);
    }

    @Override
    public Unit visit(NegateTree negateTree, TypeContext context) {
        TypeStatus t = context.get(negateTree.expression());
        if(t != TypeStatus.INT) signalInvalidOperand(negateTree, t, TypeStatus.INT);
        context.put(negateTree, TypeStatus.INT);
        return NoOpVisitor.super.visit(negateTree, context);
    }    
    
    @Override
    public Unit visit(NotTree notTree, TypeContext context) {
        TypeStatus t = context.get(notTree.expression());
        if(t != TypeStatus.BOOL) signalInvalidOperand(notTree, t, TypeStatus.BOOL);
        context.put(notTree, TypeStatus.BOOL);
        return NoOpVisitor.super.visit(notTree, context);
    }

    @Override
    public Unit visit(ReturnTree returnTree, TypeContext context) {
        TypeStatus t = context.get(returnTree.expression());
        if(t != TypeStatus.INT) signalInvalidExpression(returnTree, t, TypeStatus.INT);
        context.put(returnTree, TypeStatus.VALID);
        return NoOpVisitor.super.visit(returnTree, context);
    }

    @Override
    public Unit visit(WhileLoopTree whileLoopTree, TypeContext context) {
        TypeStatus t_expr = context.get(whileLoopTree.expression());
        if(t_expr != TypeStatus.BOOL) signalInvalidExpression(whileLoopTree, t_expr, TypeStatus.BOOL);
        TypeStatus t = context.get(whileLoopTree.statement());
        if(t != TypeStatus.VALID) signalInvalidExpression(whileLoopTree, t, TypeStatus.VALID);
        context.put(whileLoopTree, TypeStatus.VALID);
        return NoOpVisitor.super.visit(whileLoopTree, context);
    }

    private static void signalInvalidOperand(Tree tree, TypeStatus is, TypeStatus shouldBe) {
        throw new SemanticException("invalid type at " + tree.span() + 
                    ": type of operand is " + is.name() + " but should be " + shouldBe.name());
    }

    private static void signalInvalidFunction(FunctionTree tree, TypeStatus is) {
        throw new SemanticException("invalid function " + tree.name().name() + " at " + tree.span() + 
                    ": type is " + is.name());
    }

    private static void signalInvalidBlock(BlockTree tree, TypeStatus is) {
        throw new SemanticException("invalid block at " + tree.span() + 
                    ": type is " + is.name());
    }

    private static void signalMismatchingOperands(Tree tree, TypeStatus t1, TypeStatus t2) {
        throw new SemanticException("mismatched operand types at" + tree.span() + 
                ": type " + t1.name() + " does not match " + t2.name());
    }

    private static void signalInvalidExpression(Tree tree, TypeStatus is, TypeStatus shouldBe) {
        throw new SemanticException("invalid type at " + tree.span() + 
                    ": type of expression is " + is.name() + " but should be " + shouldBe.name());
    }

    private static void signalMismatchingExpressions(Tree tree, TypeStatus t1, TypeStatus t2) {
        throw new SemanticException("mismatched expression types at" + tree.span() + 
                ": type " + t1.name() + " does not match " + t2.name());
    }
}
