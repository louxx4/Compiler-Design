package edu.kit.kastel.vads.compiler.backend.instrsel;

public class BooleanValue extends Parameter {

    public final boolean value;
    public static final int ASM_TRUE = 1;
    public static final int ASM_FALSE = 0; 

    public BooleanValue(boolean value) {
        super();
        this.value = value;
    }

    @Override
    public String print(boolean debug) {
        return "$" + (this.value == true 
            ? ASM_TRUE
            : ASM_FALSE);
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