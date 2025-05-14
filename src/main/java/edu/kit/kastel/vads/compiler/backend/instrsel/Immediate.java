package edu.kit.kastel.vads.compiler.backend.instrsel;

public class Immediate extends Parameter {

    public final int value;

    public Immediate(int value) {
        super();
        this.value = value;
    }

    @Override
    public String print(boolean debug) {
        return String.valueOf(this.value);
    }

    @Override
    public boolean isSpilled() {
        return false;
    }

}