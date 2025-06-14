package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public final class Block extends Node {

    public final BlockType type;

    public enum BlockType {
        BASIC, IF_BODY, ELSE_BODY
    }

    public Block(IrGraph graph, BlockType type) {
        super(graph);
        this.type = type;
    }

    public Block(IrGraph graph) {
        this(graph, BlockType.BASIC);
    }

}
