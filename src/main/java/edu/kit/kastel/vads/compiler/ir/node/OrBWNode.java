package edu.kit.kastel.vads.compiler.ir.node;

public final class OrBWNode extends BinaryOperationNode {
    
    public OrBWNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
