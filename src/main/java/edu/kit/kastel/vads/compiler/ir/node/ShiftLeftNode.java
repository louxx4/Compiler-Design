package edu.kit.kastel.vads.compiler.ir.node;

public final class ShiftLeftNode extends BinaryOperationNode {
    
    public ShiftLeftNode(Block block, Node left, Node right) {
        super(block, left, right);
    }
}
