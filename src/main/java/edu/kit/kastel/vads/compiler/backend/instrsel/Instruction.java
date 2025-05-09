package edu.kit.kastel.vads.compiler.backend.instrsel;

import java.util.List;

public final class Instruction<S extends Parameter,T extends Parameter> {
    private String operation;
    private int parameterCount, label;
    private S left;
    private T right;

    //predicates for liveness analysis
    private List<TempReg> use, def, live; 
    private List<Instruction> succ;

    public Instruction(int label, String operation) {
        this.label = label;
        this.operation = operation;
        this.parameterCount = 0;
    }

    public Instruction(int label, String operation, S left) {
        this.label = label;
        this.operation = operation;
        this.left = left;
        this.parameterCount = 1;
    }

    public Instruction(int label, String operation, S left, T right) {
        this.label = label;
        this.operation = operation;
        this.left = left;
        this.right = right;
        this.parameterCount = 2;
    }

    public String print() {
        return String.valueOf(getLabel()) + ": " + 
            switch(this.parameterCount) {
                case 0  -> operation;
                case 1  -> operation + " " + (left == null ? "null" : left.print());
                default -> operation + " " + (left == null ? "null" : left.print()) + ", " + (right == null ? "null" : right.print());
            };
    }

    public int getLabel() {
        return this.label;
    }

    public void use(TempReg t) {
        this.use.add(t);
    }

    public void def(TempReg t){
        this.def.add(t);
    }

    public void succ(Instruction i) {
        this.succ.add(i);
    }

    public void live(TempReg t) {
        this.live.add(t);
    }

    public void live(List<TempReg> lt) {
        this.live.addAll(lt);
    }

    public List<Instruction> getSucc() {
        return this.succ;
    }

    public List<TempReg> getUse() {
        return this.use;
    }

    public List<TempReg> getDef() {
        return this.def;
    }
}