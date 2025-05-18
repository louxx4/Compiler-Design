package edu.kit.kastel.vads.compiler.backend.instrsel;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public class TempReg extends Parameter {

    public final int id;
    public Register register;
    private boolean interferes = false;

    public TempReg (int id) {
        this.id = id;
    }

    @Override
    public String print(boolean debug) {
        return "%" + (debug ? "t" + this.id : this.register.getName() + "d");
    }

    public void setRegister(Register register) {
        this.register = register;
    }

    @Override
    public boolean isSpilled() {
        return this.register.isSpilled;
    }
    
    @Override
    public void setSpillingRegister(String spillingRegister) {
        this.register.setName(spillingRegister);
    }

    
    public void setInterfering() {
        this.interferes = true;
    }

    public boolean interferes() {
        return this.interferes;
    }
}