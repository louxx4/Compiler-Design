package edu.kit.kastel.vads.compiler.ir.node;

public sealed abstract class UnaryOperationNode extends Node permits NotNode, NotBWNode {
    public static final int RIGHT = 0;

    protected UnaryOperationNode(Block block, Node right) {
        super(block, right);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UnaryOperationNode unOp)) {
            return false;
        }
        return obj.getClass() == this.getClass()
            && this.predecessor(RIGHT) == unOp.predecessor(RIGHT);
    }

    @Override
    public int hashCode() {
        return predecessorHash(this, RIGHT) ^ this.getClass().hashCode();
    }
}