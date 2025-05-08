package edu.kit.kastel.vads.compiler.backend.instrsel;

public final class Instruction<S extends Parameter,T extends Parameter> {
    private String operation;
    private int parameterCount;
    private S left;
    private T right;

    public Instruction(String operation) {
        this.operation = operation;
        this.parameterCount = 0;
    }

    public Instruction(String operation, S left) {
        this.operation = operation;
        this.left = left;
        this.parameterCount = 1;
    }

    public Instruction(String operation, S left, T right) {
        this.operation = operation;
        this.left = left;
        this.right = right;
        this.parameterCount = 2;
    }

    public String print() {
        return switch(this.parameterCount) {
            case 0  -> operation;
            case 1  -> operation + " " + (left == null ? "null" : left.print());
            default -> operation + " " + (left == null ? "null" : left.print()) + ", " + (right == null ? "null" : right.print());
        };
    }
}