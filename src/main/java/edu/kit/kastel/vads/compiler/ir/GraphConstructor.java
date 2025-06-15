package edu.kit.kastel.vads.compiler.ir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.JumpType;

class GraphConstructor {

    private final Optimizer optimizer;
    private final IrGraph graph;
    private final Map<Name, Map<Block, Node>> currentDef = new HashMap<>();
    private final Map<Block, Map<Name, Phi>> incompletePhis = new HashMap<>();
    private final Map<Block, Node> currentSideEffect = new HashMap<>();
    private final Map<Block, Phi> incompleteSideEffectPhis = new HashMap<>();
    private final Set<Block> sealedBlocks = new HashSet<>();
    private Block currentBlock;

    public GraphConstructor(Optimizer optimizer, String name) {
        this.optimizer = optimizer;
        this.graph = new IrGraph(name);
        this.currentBlock = this.graph.startBlock();
        // the start block never gets any more predecessors
        sealBlock(this.currentBlock);
    }

    public Node newStart() {
        assert currentBlock() == this.graph.startBlock() : "start must be in start block";
        return new StartNode(currentBlock());
    }

    public Node newAdd(Node left, Node right) {
        return this.optimizer.transform(new AddNode(currentBlock(), left, right));
    }
    public Node newSub(Node left, Node right) {
        return this.optimizer.transform(new SubNode(currentBlock(), left, right));
    }

    public Node newMul(Node left, Node right) {
        return this.optimizer.transform(new MulNode(currentBlock(), left, right));
    }

    public Node newDiv(Node left, Node right) {
        return this.optimizer.transform(new DivNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newMod(Node left, Node right) {
        return this.optimizer.transform(new ModNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newReturn(Node result) {
        return new ReturnNode(currentBlock(), readCurrentSideEffect(), result);
    }

    public Node newConstInt(int value) {
        // always move const into start block, this allows better deduplication
        // and resultingly in better value numbering
        return this.optimizer.transform(new ConstIntNode(this.graph.startBlock(), value));
    }

    public Node newConstBool(boolean value) {
        return this.optimizer.transform(new ConstBoolNode(this.graph.startBlock(), value));
    }

    public Node newNot(Node right) {
        return this.optimizer.transform(new NotNode(currentBlock(), right));
    }

    public Node newAnd(Node left, Node right) {
        return this.optimizer.transform(new AndBWNode(currentBlock(), left, right));
    }

    public Node newOr(Node left, Node right) {
        return this.optimizer.transform(new OrBWNode(currentBlock(), left, right));
    }

    public Node newXor(Node left, Node right) {
        return this.optimizer.transform(new XorNode(currentBlock(), left, right));
    }

    public Node newShl(Node left, Node right) {
        return this.optimizer.transform(new ShiftLeftNode(currentBlock(), left, right));
    }

    public Node newShr(Node left, Node right) {
        return this.optimizer.transform(new ShiftRightNode(currentBlock(), left, right));
    }

    public Node newEq(Node left, Node right) {
        return this.optimizer.transform(new EqualsNode(currentBlock(), left, right));
    }

    public Node newNeq(Node left, Node right) {
        return this.optimizer.transform(new EqualsNotNode(currentBlock(), left, right));
    }

    public Node newGeq(Node left, Node right) {
        return this.optimizer.transform(new GreaterEqualNode(currentBlock(), left, right));
    }

    public Node newGreater(Node left, Node right) {
        return this.optimizer.transform(new GreaterNode(currentBlock(), left, right));
    }

    public Node newLeq(Node left, Node right) {
        return this.optimizer.transform(new SmallerEqualNode(currentBlock(), left, right));
    }

    public Node newLess(Node left, Node right) {
        return this.optimizer.transform(new SmallerNode(currentBlock(), left, right));
    }

    public Node newNegateBW(Node right) {
        return this.optimizer.transform(new NotBWNode(currentBlock(), right));
    }

    public Block newBlock(Block.BlockType type, Node... predecessors) {
        Block block = newBlock(type);
        for(Node p : predecessors) {
            block.addPredecessor(p);
        }
        return block;
    }

    public Block newBlock(Block.BlockType type) {
        Block block = new Block(this.graph, type);
        this.currentBlock = block;
        return block;
    }

    public Block newBlock() {
        return newBlock(Block.BlockType.BASIC);
    }

    public Block newBlock(Node... predecessors) {
        return newBlock(Block.BlockType.BASIC, predecessors);
    }

    public Node newJump() {
        return this.optimizer.transform(new JumpNode(currentBlock()));
    }

    public ProjNode newProj(Node in, ProjNode.ProjectionInfo info) {
        return (ProjNode) this.optimizer.transform(new ProjNode(currentBlock(), in, info));
    }

    public Node newIfNode(Node condition) {
        return this.optimizer.transform(new IfNode(currentBlock(), condition));
    }

    public Node newIfEndNode(Node... predecessors) {
        return this.optimizer.transform(new IfEndNode(currentBlock(), predecessors));
    }

    public Node newWhileLoop(Node condition, Node body) {
        Node ifNode = this.optimizer.transform(new IfNode(currentBlock(), condition));
        Node projTrue = this.optimizer.transform(new ProjNode(currentBlock, ifNode, ProjNode.BooleanProjectionInfo.TRUE));
        Node projFalse = this.optimizer.transform(new ProjNode(currentBlock, ifNode, ProjNode.BooleanProjectionInfo.FALSE));

        Node jumpTrueNode = this.optimizer.transform(new JumpNode(currentBlock(), projTrue));
        body.addPredecessor(jumpTrueNode);

        Node jumpFalseNode = this.optimizer.transform(new JumpNode(currentBlock(), projFalse));
        Node ifEndNode = this.optimizer.transform(new IfEndNode(currentBlock(), body, jumpFalseNode));

        Node jumpNode = this.optimizer.transform(new JumpNode(currentBlock(), body));
        ifNode.addPredecessor(jumpNode);

        return ifNode;
    }

    public Node newForLoop(Node initialization, Node condition, Node advancement, Node body) {
        if (initialization != null) {
            condition.addPredecessor(initialization);

        }
        Node ifNode = this.optimizer.transform(new IfNode(currentBlock(), condition));
        Node projTrue = this.optimizer.transform(new ProjNode(currentBlock, ifNode, ProjNode.BooleanProjectionInfo.TRUE));
        Node projFalse = this.optimizer.transform(new ProjNode(currentBlock, ifNode, ProjNode.BooleanProjectionInfo.FALSE));

        Node jumpTrueNode = this.optimizer.transform(new JumpNode(currentBlock(), projTrue));
        body.addPredecessor(jumpTrueNode);

        Node jumpFalseNode = this.optimizer.transform(new JumpNode(currentBlock(), projFalse));
        Node ifEndNode = this.optimizer.transform(new IfEndNode(currentBlock(), body, jumpFalseNode));

        Node jumpNode;
        if (advancement != null) {
            advancement.addPredecessor(body);
            jumpNode = this.optimizer.transform(new JumpNode(currentBlock(), advancement));
        } else {
            jumpNode = this.optimizer.transform(new JumpNode(currentBlock(), body));
        }
        ifNode.addPredecessor(jumpNode);

        return ifNode;
    }

    public Node newContinue() {
        return this.optimizer.transform(new LoopJumpNode(JumpType.CONTINUE, currentBlock()));
    }

    public Node newBreak() {
        return this.optimizer.transform(new LoopJumpNode(JumpType.BREAK, currentBlock()));
    }

    public Node newSideEffectProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.SIDE_EFFECT);
    }

    public Node newResultProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.RESULT);
    }

    public Block currentBlock() {
        return this.currentBlock;
    }

    public Phi newPhi() {
        // don't transform phi directly, it is not ready yet
        return new Phi(currentBlock());
    }

    public IrGraph graph() {
        return this.graph;
    }

    void writeVariable(Name variable, Block block, Node value) {
        this.currentDef.computeIfAbsent(variable, _ -> new HashMap<>()).put(block, value);
    }

    Node readVariable(Name variable, Block block) {
        Node node = this.currentDef.getOrDefault(variable, Map.of()).get(block);
        if (node != null) {
            return node;
        }
        return readVariableRecursive(variable, block);
    }


    private Node readVariableRecursive(Name variable, Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = newPhi();
            this.incompletePhis.computeIfAbsent(block, _ -> new HashMap<>()).put(variable, (Phi) val);
        } else if (block.predecessors().size() == 1) {
            val = readVariable(variable, block.predecessors().getFirst().block());
        } else {
            val = newPhi();
            writeVariable(variable, block, val);
            val = addPhiOperands(variable, (Phi) val);
        }
        writeVariable(variable, block, val);
        return val;
    }

    Node addPhiOperands(Name variable, Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readVariable(variable, pred.block()));
        }
        return tryRemoveTrivialPhi(phi);
    }

    Node tryRemoveTrivialPhi(Phi phi) {
        Node same = null;
        for (Node op : phi.block().predecessors()) {
            if (op == same || op == phi) {
                continue;
            }
            if (same != null) {
                return phi;
            }
            same = op;
        }
        if (same == null) {
            throw new RuntimeException("Phi unreachable or in start block");
        }
        Set<Node> users = new HashSet<Node>(phi.graph().successors(phi));
        users.remove(phi);
        graph.replaceAllBy(phi, same);
        for (Node use : users) {
            if (use instanceof Phi) {
                tryRemoveTrivialPhi((Phi) use);
            }
        }
        return same;
    }

    void sealBlock(Block block) {
        for (Map.Entry<Name, Phi> entry : this.incompletePhis.getOrDefault(block, Map.of()).entrySet()) {
            addPhiOperands(entry.getKey(), entry.getValue());
        }
        this.sealedBlocks.add(block);
    }

    public void writeCurrentSideEffect(Node node) {
        writeSideEffect(currentBlock(), node);
    }

    private void writeSideEffect(Block block, Node node) {
        this.currentSideEffect.put(block, node);
    }

    public Node readCurrentSideEffect() {
        return readSideEffect(currentBlock());
    }

    private Node readSideEffect(Block block) {
        Node node = this.currentSideEffect.get(block);
        if (node != null) {
            return node;
        }
        return readSideEffectRecursive(block);
    }

    private Node readSideEffectRecursive(Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = newPhi();
            Phi old = this.incompleteSideEffectPhis.put(block, (Phi) val);
            assert old == null : "double readSideEffectRecursive for " + block;
        } else if (block.predecessors().size() == 1) {
            val = readSideEffect(block.predecessors().getFirst().block());
        } else {
            val = newPhi();
            writeSideEffect(block, val);
            val = addPhiOperands((Phi) val);
        }
        writeSideEffect(block, val);
        return val;
    }

    Node addPhiOperands(Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readSideEffect(pred.block()));
        }
        return tryRemoveTrivialPhi(phi);
    }

}
