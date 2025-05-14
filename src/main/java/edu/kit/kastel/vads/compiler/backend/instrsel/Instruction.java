package edu.kit.kastel.vads.compiler.backend.instrsel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;

public final class Instruction<S extends Parameter,T extends Parameter> {
    private final String operation;
    private final int parameterCount, label;
    private S left;
    private T right;

    //predicates for liveness analysis
    private final List<TempReg> use = new ArrayList<>(), def = new ArrayList<>(), live = new ArrayList<>(); 
    private final List<Instruction> succ = new ArrayList<>();

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

    public String print(boolean debugMode) {
        String line = (debugMode ? String.valueOf(getLabel()) + ": " : "");
        if(left != null && left.isSpilled()) 
            line += getSpillingInstruction(left, RegisterAllocator.SPILLING_REG_1);
        if(right != null && right.isSpilled()) 
            line += getSpillingInstruction(right, RegisterAllocator.SPILLING_REG_2);
        line += switch(this.parameterCount) {
                    case 0  -> operation;
                    case 1  -> operation + " " + 
                        (left == null ? "null" : left.print());
                    default -> operation + " " + 
                        (left == null ? "null" : left.print()) + ", " + 
                        (right == null ? "null" : right.print());
                };
        String ident = " ".repeat(30 - line.length());
        return line + (debugMode ? ident + "{live: " + 
            getLive().stream().map(t -> t.print()).collect(Collectors.joining(",")) + "}" : "");
    }

    private static String getSpillingInstruction(Parameter t, String register) {
        //TODO: add spilling instruction
        return "spilling... \n";
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