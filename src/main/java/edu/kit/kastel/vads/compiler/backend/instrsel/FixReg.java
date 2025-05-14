package edu.kit.kastel.vads.compiler.backend.instrsel;

public class FixReg extends Parameter {

    public final String name;

    public FixReg (String name) {
        this.name = name;
    }

    @Override
    public String print(boolean debug) {
        return "%" + this.name;
    }

    @Override
    public boolean isSpilled() {
        return false;
    }

}