package edu.kit.kastel.vads.compiler.backend.instrsel;

public abstract class Parameter {

    public abstract String print(boolean debug);

    public abstract boolean isSpilled();

}