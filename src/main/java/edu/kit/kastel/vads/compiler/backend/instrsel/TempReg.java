package edu.kit.kastel.vads.compiler.backend.instrsel;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public class TempReg extends Parameter {

    public final int id;
    public Register register;
    private boolean interferes = false;
    private final RegisterSize size;

    public TempReg (int id, RegisterSize size) {
        this.id = id;
        this.size = size;
    }

    @Override
    public String print(boolean debug) {
        return "%" + (debug ? "t" + this.id : this.register.getName() + this.size.getSuffix());
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

    enum RegisterSize {
        BYTE("b"),
        DOUBLE_WORD("d");

        private final String suffix;

        private RegisterSize(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() {
            return this.suffix;
        }
    }
}