package edu.kit.kastel.vads.compiler.ir.node;

public final class AndBWNode extends BinaryOperationNode {
    
    public AndBWNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
