package edu.kit.kastel.vads.compiler.backend.instrsel;

public class Immediate extends Parameter {

    public final int value;

    public Immediate(int value) {
        super();
        this.value = value;
    }

    @Override
    public String print() {
        return "$" + this.value;
    }

}