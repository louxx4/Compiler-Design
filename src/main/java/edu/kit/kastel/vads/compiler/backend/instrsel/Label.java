package edu.kit.kastel.vads.compiler.backend.instrsel;

public class Label extends Parameter {

    public final String value;

    public Label(String value) {
        super();
        this.value = value;
    }

    @Override
    public String print(boolean debug) {
        return this.value;
    }

    @Override
    public boolean isSpilled() {
        return false;
    }

    @Override
    public void setSpillingRegister(String spillingRegister) {
        //ignore
    }

}