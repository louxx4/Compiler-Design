package edu.kit.kastel.vads.compiler.backend.regalloc;

public class Register {
    public final String name;
    public final boolean isSpilled;

    public Register(String name, boolean isSpilled) {
        this.name = name;
        this.isSpilled = isSpilled;
    }

}
