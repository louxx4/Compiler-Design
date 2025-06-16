package edu.kit.kastel.vads.compiler.ir;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.BinaryOperator;

import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfo;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfoHelper;
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
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileLoopTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

/// SSA translation as described in
/// [`Simple and Efficient Construction of Static Single Assignment Form`](https://compilers.cs.uni-saarland.de/papers/bbhlmz13cc.pdf).
///
/// This implementation also tracks side effect edges that can be used to avoid reordering of operations that cannot be
/// reordered.
///
/// We recommend to read the paper to better understand the mechanics implemented here.
public class SsaTranslation {
    private final FunctionTree function;
    private final GraphConstructor constructor;

    public SsaTranslation(FunctionTree function, Optimizer optimizer) {
        this.function = function;
        this.constructor = new GraphConstructor(optimizer, function.nameTree().name().asString());
    }

    public IrGraph translate() {
        var visitor = new SsaTranslationVisitor();
        this.function.accept(visitor, this);
        this.constructor.checkAllPhis();
        return this.constructor.graph();
    }

    private void writeVariable(Name variable, Block block, Node value) {
        this.constructor.writeVariable(variable, block, value);
    }

    private Node readVariable(Name variable, Block block) {
        return this.constructor.readVariable(variable, block);
    }

    private Block currentBlock() {
        return this.constructor.currentBlock();
    }

    private static class SsaTranslationVisitor implements Visitor<SsaTranslation, Optional<Node>> {

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private static final Optional<Node> NOT_AN_EXPRESSION = Optional.empty();

        private final Deque<DebugInfo> debugStack = new ArrayDeque<>();

        private void pushSpan(Tree tree) {
            this.debugStack.push(DebugInfoHelper.getDebugInfo());
            DebugInfoHelper.setDebugInfo(new DebugInfo.SourceInfo(tree.span()));
        }

        private void popSpan() {
            DebugInfoHelper.setDebugInfo(this.debugStack.pop());
        }

        @Override
        public Optional<Node> visit(AssignmentTree assignmentTree, SsaTranslation data) {
            pushSpan(assignmentTree);
            BinaryOperator<Node> desugar = switch (assignmentTree.operator().type()) {
                case ASSIGN_MINUS -> data.constructor::newSub;
                case ASSIGN_PLUS -> data.constructor::newAdd;
                case ASSIGN_MUL -> data.constructor::newMul;
                case ASSIGN_DIV -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
                case ASSIGN_MOD -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));
                case ASSIGN_AND -> data.constructor::newAnd;
                case ASSIGN_OR -> data.constructor::newOr;
                case ASSIGN_XOR -> data.constructor::newXor;
                case ASSIGN_SHL -> data.constructor::newShl;
                case ASSIGN_SHR -> data.constructor::newShr;
                case ASSIGN -> null;
                default ->
                    throw new IllegalArgumentException("not an assignment operator " + assignmentTree.operator());
            };

            switch (assignmentTree.lValue()) {
                case LValueIdentTree(var name) -> {
                    Node rhs = assignmentTree.expression().accept(this, data).orElseThrow();
                    if (desugar != null) {
                        rhs = desugar.apply(data.readVariable(name.name(), data.currentBlock()), rhs);
                    }
                    data.writeVariable(name.name(), data.currentBlock(), rhs);
                }
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(BinaryOperationTree binaryOperationTree, SsaTranslation data) {
            pushSpan(binaryOperationTree);
            Node lhs = binaryOperationTree.lhs().accept(this, data).orElseThrow();
            Node rhs = binaryOperationTree.rhs().accept(this, data).orElseThrow();
            Node res = switch (binaryOperationTree.operatorType()) {
                case MINUS -> data.constructor.newSub(lhs, rhs);
                case PLUS -> data.constructor.newAdd(lhs, rhs);
                case MUL -> data.constructor.newMul(lhs, rhs);
                case DIV -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
                case MOD -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));
                default ->
                    throw new IllegalArgumentException("not a binary expression operator " + binaryOperationTree.operatorType());
            };
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(BlockTree blockTree, SsaTranslation data) {
            pushSpan(blockTree);
            for (StatementTree statement : blockTree.statements()) {
                statement.accept(this, data);
                // skip everything after a return in a block
                if (statement instanceof ReturnTree) {
                    break;
                }
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(DeclarationTree declarationTree, SsaTranslation data) {
            pushSpan(declarationTree);
            if (declarationTree.initializer() != null) {
                Node rhs = declarationTree.initializer().accept(this, data).orElseThrow();
                data.writeVariable(declarationTree.nameTree().name(), data.currentBlock(), rhs);
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(FunctionTree functionTree, SsaTranslation data) {
            pushSpan(functionTree);
            Node start = data.constructor.newStart();
            data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(start));
            functionTree.body().accept(this, data);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(IdentExpressionTree identExpressionTree, SsaTranslation data) {
            pushSpan(identExpressionTree);
            Node value = data.readVariable(identExpressionTree.nameTree().name(), data.currentBlock());
            popSpan();
            return Optional.of(value);
        }

        @Override
        public Optional<Node> visit(LiteralTree literalTree, SsaTranslation data) {
            pushSpan(literalTree);
            Node node = data.constructor.newConstInt((int) literalTree.parseValue().orElseThrow());
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(LValueIdentTree lValueIdentTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(NameTree nameTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(NegateTree negateTree, SsaTranslation data) {
            pushSpan(negateTree);
            Node node = negateTree.expression().accept(this, data).orElseThrow();
            Node res = data.constructor.newSub(data.constructor.newConstInt(0), node);
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(ProgramTree programTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Node> visit(ReturnTree returnTree, SsaTranslation data) {
            pushSpan(returnTree);
            Node node = returnTree.expression().accept(this, data).orElseThrow();
            Node ret = data.constructor.newReturn(node);
            data.constructor.graph().endBlock().addPredecessor(ret);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(TypeTree typeTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        private Node projResultDivMod(SsaTranslation data, Node divMod) {
            // make sure we actually have a div or a mod, as optimizations could
            // have changed it to something else already
            if (!(divMod instanceof DivNode || divMod instanceof ModNode)) {
                return divMod;
            }
            Node projSideEffect = data.constructor.newSideEffectProj(divMod);
            data.constructor.writeCurrentSideEffect(projSideEffect);
            return data.constructor.newResultProj(divMod);
        }

        @Override
        public Optional<Node> visit(IfStatementTree ifStatementTree, SsaTranslation data) {
            pushSpan(ifStatementTree);
            Node condition = ifStatementTree.expression().accept(this, data).orElseThrow();
            //create if projections
            ProjNode projTrue = data.constructor.newProj(condition, ProjNode.BooleanProjectionInfo.TRUE);
            ProjNode projFalse = data.constructor.newProj(condition, ProjNode.BooleanProjectionInfo.FALSE);
            projTrue.setSibling(projFalse);
            projFalse.setSibling(projTrue);
            data.constructor.sealBlock(data.constructor.currentBlock());
            //create if/else body blocks
            Block ifBody = data.constructor.newBlock(Block.BlockType.IF_BODY, projTrue);
            ifStatementTree.if_body().accept(this, data);
            Node jumpIf = data.constructor.newJump();
            data.constructor.sealBlock(ifBody);
            Block elseBody = data.constructor.newBlock(Block.BlockType.ELSE_BODY, projFalse);
            if (ifStatementTree.else_body() != null) {
                ifStatementTree.else_body().accept(this, data);
            }
            Node jumpElse = data.constructor.newJump();
            data.constructor.sealBlock(elseBody);
            data.constructor.newBlock(jumpIf, jumpElse); //following block
            data.constructor.sealBlock(data.constructor.currentBlock());
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ConditionalTree conditionalTree, SsaTranslation data) {
            pushSpan(conditionalTree);
            Node condition = conditionalTree.lhs().accept(this, data).orElseThrow();
            //create if projections
            ProjNode projTrue = data.constructor.newProj(condition, ProjNode.BooleanProjectionInfo.TRUE);
            ProjNode projFalse = data.constructor.newProj(condition, ProjNode.BooleanProjectionInfo.FALSE);
            projTrue.setSibling(projFalse);
            projFalse.setSibling(projTrue);
            //create if/else body blocks
            Block ifBody = data.constructor.newBlock(Block.BlockType.IF_BODY, projTrue);
            Node ifValue = conditionalTree.if_expression().accept(this, data).orElseThrow();
            Node jumpIf = data.constructor.newJump();
            data.constructor.sealBlock(ifBody);
            Block elseBody = data.constructor.newBlock(Block.BlockType.ELSE_BODY, projFalse);
            Node elseValue = conditionalTree.else_expression().accept(this, data).orElseThrow();
            Node jumpElse = data.constructor.newJump();
            data.constructor.sealBlock(elseBody);
            data.constructor.newBlock(jumpIf, jumpElse); //following block
            data.constructor.sealBlock(data.constructor.currentBlock());
            //create join node
            Node phi = data.constructor.newPhi();
            phi.addPredecessor(ifValue);
            phi.addPredecessor(elseValue);
            popSpan();
            return Optional.of(phi);
        }

        @Override
        public Optional<Node> visit(WhileLoopTree whileLoopTree, SsaTranslation data) {
            pushSpan(whileLoopTree);
            Node condition = whileLoopTree.expression().accept(this, data).orElseThrow();
            //create if projections for condition checking
            ProjNode projTrue = data.constructor.newProj(condition, ProjNode.BooleanProjectionInfo.TRUE);
            ProjNode projFalse = data.constructor.newProj(condition, ProjNode.BooleanProjectionInfo.FALSE);
            projTrue.setSibling(projFalse);
            projFalse.setSibling(projTrue);
            //create body block
            Block whileBody = data.constructor.newBlock(Block.BlockType.WHILE_BODY, projTrue);
            whileLoopTree.statement().accept(this, data);
            Node jumpWhile = data.constructor.newJump();
            condition.addPredecessor(jumpWhile);
            data.constructor.sealBlock(whileBody);
            data.constructor.sealBlock(condition.block());
            //create jump outside
            Block elseBody = data.constructor.newBlock(Block.BlockType.ELSE_BODY, projFalse);
            Node jumpEnd = data.constructor.newJump();
            data.constructor.sealBlock(elseBody);
            data.constructor.newBlock(jumpEnd, whileBody); //following block
            data.constructor.sealBlock(data.constructor.currentBlock());
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(ForLoopTree forLoopTree, SsaTranslation data) {
            pushSpan(forLoopTree);
            Node condition = forLoopTree.condition().accept(this, data).orElseThrow();
            Node initialization = null;
            if (forLoopTree.initialization() != null) {
                initialization = forLoopTree.initialization().accept(this, data).orElseThrow();
            }
            Node advancement = null;
            if (forLoopTree.advancement() != null) {
                advancement = forLoopTree.advancement().accept(this, data).orElseThrow();
            }
            Node body = forLoopTree.body().accept(this, data).orElseThrow();
            Node node = data.constructor.newForLoop(condition, initialization, advancement, body);
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(BooleanTree booleanTree, SsaTranslation data) {
            pushSpan(booleanTree);
            Node node = data.constructor.newConstBool(booleanTree.value());
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(NegateBWTree negateBWTree, SsaTranslation data) {
            pushSpan(negateBWTree);
            Node rightNode = negateBWTree.expression().accept(this, data).orElseThrow();
            Node node = data.constructor.newNegateBW(rightNode);
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(JumpTree jumpTree, SsaTranslation data) {
            pushSpan(jumpTree);
            Node res = switch (jumpTree.type()) {
                case CONTINUE -> data.constructor.newContinue();
                case BREAK -> data.constructor.newBreak();
            };
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(NotTree notTree, SsaTranslation data) {
            pushSpan(notTree);
            Node rightNode = notTree.expression().accept(this, data).orElseThrow();
            Node node = data.constructor.newNot(rightNode);
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(LogicalOperationTree logicalOperationTree, SsaTranslation data) {
            pushSpan(logicalOperationTree);
            Node lhs = logicalOperationTree.lhs().accept(this, data).orElseThrow();
            Node rhs = logicalOperationTree.rhs().accept(this, data).orElseThrow();
            Node res = switch (logicalOperationTree.operatorType()) {
                case LESS -> data.constructor.newLess(lhs, rhs);
                case LEQ -> data.constructor.newLeq(lhs, rhs);
                case GREATER -> data.constructor.newGreater(lhs, rhs);
                case GEQ -> data.constructor.newGeq(lhs, rhs);
                case EQ -> data.constructor.newEq(lhs, rhs);
                case NEQ -> data.constructor.newNeq(lhs, rhs);
                case AND -> data.constructor.newAnd(lhs, rhs);
                case OR -> data.constructor.newOr(lhs, rhs);
                default ->
                        throw new IllegalArgumentException("not a logical expression operator " + logicalOperationTree.operatorType());
            };
            popSpan();
            return Optional.of(res);
        }
    }


}