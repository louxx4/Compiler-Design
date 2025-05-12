package edu.kit.kastel.vads.compiler.backend.instrsel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class Instruction<S extends Parameter,T extends Parameter> {
    private String operation;
    private int parameterCount, label;
    private S left;
    private T right;

    //predicates for liveness analysis
    private List<TempReg> use = new ArrayList<>(), def = new ArrayList<>(), live = new ArrayList<>(); 
    private List<Instruction> succ = new ArrayList<>();

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
        String line = String.valueOf(getLabel()) + ": " + 
            switch(this.parameterCount) {
                case 0  -> operation;
                case 1  -> operation + " " + 
                    (left == null ? "null" : left.print());
                default -> operation + " " + 
                    (left == null ? "null" : left.print()) + ", " + 
                    (right == null ? "null" : right.print());
            };
        String ident = " ".repeat(30 - line.length());
        return line + ident + "{live: " + getLive().stream().map(t -> t.print()).collect(Collectors.joining(",")) + "}";
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

    public List<TempReg> getLive() {
        return this.live;
    }

    public boolean isUndef(TempReg t) {
        return !this.def.contains(t);
    }

    public boolean isLive(TempReg t) {
        return this.live.contains(t);
    }
}