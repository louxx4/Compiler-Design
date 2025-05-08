package edu.kit.kastel.vads.compiler.backend.instrsel;

public class TempReg extends Parameter {

    public final int id;

    public TempReg (int id) {
        this.id = id;
    }

    @Override
    public String print() {
        return "%t" + id;
    }
}