package edu.kit.kastel.vads.compiler.backend.regalloc;

public class Register {
    private String name;
    private int stackOffset;
    public final boolean isSpilled;

    public Register(String name) {
        this.name = name;
        this.isSpilled = false;
    }

    public Register(int stackOffset) {
        this.stackOffset = stackOffset;
        this.isSpilled = true;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public int getStackOffset() {
        return this.stackOffset;
    }

}
