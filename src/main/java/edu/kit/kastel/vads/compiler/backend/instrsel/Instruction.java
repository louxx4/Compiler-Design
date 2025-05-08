package edu.kit.kastel.vads.compiler.backend.instrsel;

public final class Instruction<S extends Parameter,T extends Parameter> {
    private String operation;
    private S left;
    private T right;

    public Instruction(String operation) {
        this.operation = operation;
    }

    public Instruction(String operation, S left) {
        this(operation);
        this.left = left;
    }

    public Instruction(String operation, S left, T right) {
        this(operation, left);
        this.right = right;
    }
}