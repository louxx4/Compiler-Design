package edu.kit.kastel.vads.compiler.semantic;

import java.util.List;

import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.ConditionalTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

// Checks that the program has no type errors.
// Types of subtrees should never be null, since traversal of tree happens in postorder.
class TypeAnalysis implements NoOpVisitor<TypeContext> {

    enum TypeStatus {
        INT, BOOL, VALID
    }

    private static final List<OperatorType> INT_TO_INT_OPERATIONS = List.of(
        OperatorType.PLUS, OperatorType.MINUS, OperatorType.MUL, OperatorType.DIV, OperatorType.MOD,
        OperatorType.AND_BW, OperatorType.XOR_BW, OperatorType.OR_BW, OperatorType.SHL, OperatorType.SHR, OperatorType.NOT_BW);

    private static final List<OperatorType> INT_TO_BOOL_OPERATIONS = List.of(
        OperatorType.LESS, OperatorType.LEQ, OperatorType.GREATER, OperatorType.GEQ);

    private static final List<OperatorType> BOOL_TO_BOOL_OPERATIONS = List.of(
        OperatorType.AND, OperatorType.OR, OperatorType.NOT); 

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
        OperatorType operator = binaryOperationTree.operatorType();
        if(INT_TO_INT_OPERATIONS.contains(operator)) {
            TypeStatus t1 = context.get(binaryOperationTree.lhs());
            if(t1 != TypeStatus.INT) signalInvalidOperand(binaryOperationTree, t1, TypeStatus.INT);
            TypeStatus t2 = context.get(binaryOperationTree.rhs());
            if(t2 != TypeStatus.INT) signalInvalidOperand(binaryOperationTree, t2, TypeStatus.INT);
            context.put(binaryOperationTree, TypeStatus.INT);
        } else if(BOOL_TO_BOOL_OPERATIONS.contains(operator)) {
            TypeStatus t1 = context.get(binaryOperationTree.lhs());
            if(t1 != TypeStatus.BOOL) signalInvalidOperand(binaryOperationTree, t1, TypeStatus.BOOL);
            TypeStatus t2 = context.get(binaryOperationTree.rhs());
            if(t2 != TypeStatus.BOOL) signalInvalidOperand(binaryOperationTree, t2, TypeStatus.BOOL);
            context.put(binaryOperationTree, TypeStatus.BOOL);
        } else if (INT_TO_BOOL_OPERATIONS.contains(operator)) {
            TypeStatus t1 = context.get(binaryOperationTree.lhs());
            if(t1 != TypeStatus.INT) signalInvalidOperand(binaryOperationTree, t1, TypeStatus.INT);
            TypeStatus t2 = context.get(binaryOperationTree.rhs());
            if(t2 != TypeStatus.INT) signalInvalidOperand(binaryOperationTree, t2, TypeStatus.INT);
            context.put(binaryOperationTree, TypeStatus.BOOL);
        } else { // RANDOMTYPE_TO_BOOL_OPERATIONS
            TypeStatus t1 = context.get(binaryOperationTree.lhs());
            TypeStatus t2 = context.get(binaryOperationTree.rhs());
            if(t1 != t2) signalMismatchingOperands(binaryOperationTree, t1, t2);
            context.put(binaryOperationTree, TypeStatus.BOOL);
        }
        return NoOpVisitor.super.visit(binaryOperationTree, context);
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

    private static void signalInvalidOperand(Tree tree, TypeStatus is, TypeStatus shouldBe) {
        throw new SemanticException("invalid type at " + tree.span() + 
                    ": type of operand is " + is.name() + " but should be " + shouldBe.name());
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
