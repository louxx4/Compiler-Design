package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public final class Block extends Node {

    public final BlockType type;
    private String label = null;

    public enum BlockType {
        BASIC, IF_BODY, ELSE_BODY, AFTER_IF, WHILE_BODY
    }

    public Block(IrGraph graph, BlockType type) {
        super(graph);
        this.type = type;
    }

    public Block(IrGraph graph) {
        this(graph, BlockType.BASIC);
    }

    public boolean hasLabel(){
        return (this.label != null);
    }

    public String getLabel() {
        return this.label;
    }

    public void createLabel(int id) {
        this.label = "_" + this.type.name().toLowerCase() + "_" + id;
    }
}
