package edu.kit.kastel.vads.compiler.backend.instrsel;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public class TempReg extends Parameter {

    public final int id;
    public Register register;

    public TempReg (int id) {
        this.id = id;
    }

    @Override
    public String print(boolean debug) {
        return "%" + (debug ? "t" + this.id : this.register.getName());
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
}