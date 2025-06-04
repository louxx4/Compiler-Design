package edu.kit.kastel.vads.compiler.ir.node;

public final class EqualsNotNode extends BinaryOperationNode {
    
    public EqualsNotNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
