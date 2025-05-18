package edu.kit.kastel.vads.compiler.backend.instrsel;

public class InstructionInfo {
    private boolean visited = false;
    private TempReg register;

    public InstructionInfo() {

    }

    public void visit() {
        this.visited = true;
    }

    public boolean wasVisited() {
        return this.visited;
    }

    public void setRegister(TempReg register) {
        this.register = register;
    }

    public TempReg getRegister() {
        return this.register;
    }
}