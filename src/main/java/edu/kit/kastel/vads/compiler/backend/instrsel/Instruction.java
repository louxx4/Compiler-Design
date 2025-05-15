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
        List<String> lines = new ArrayList<>();
        String prefix = (debugMode ? String.valueOf(getLabel()) + ": " : ""); // adds line numbering for debugging
        String suffixSpilling = "(due to spilling)"; // adds comment for debugging

        // if neccessary, add spilling operation (load from stack)
        if(left != null && left.isSpilled()) {
            left.setSpillingRegister(RegisterAllocator.SPILLING_REG_1);
            String line = prefix + getSpillingLoadInstruction((TempReg) left, debugMode);
            if(debugMode) line += getIdentation(line) + suffixSpilling;
            lines.add(line);
        }
        if(right != null && right.isSpilled()) {
            right.setSpillingRegister(RegisterAllocator.SPILLING_REG_2);
            String line = prefix + getSpillingLoadInstruction((TempReg) right, debugMode);
            if(debugMode) line += getIdentation(line) + suffixSpilling;
            lines.add(line);
        }

        // print operation
        String op = prefix + 
            switch(this.parameterCount) {
                case 0  -> operation;
                case 1  -> operation + " " + 
                    (left == null ? "null" : left.print(debugMode));
                default -> operation + " " + 
                    (left == null ? "null" : left.print(debugMode)) + ", " + 
                    (right == null ? "null" : right.print(debugMode));
            };
        op += (debugMode ? getIdentation(op) + "{live: " + getLive().stream().map(t -> t.print(debugMode))
            .collect(Collectors.joining(",")) + "}" : "");
        lines.add(op);

        // if neccessary, add spilling operation (save to stack)
        if(left != null && left.isSpilled()) {
            String line = prefix + getSpillingSaveInstruction((TempReg) left, debugMode);
            if(debugMode) line += getIdentation(line) + suffixSpilling;
            lines.add(line);
        }
        if(right != null && right.isSpilled()) {
            String line = prefix + getSpillingSaveInstruction((TempReg) right, debugMode);
            if(debugMode) line += getIdentation(line) + suffixSpilling;
            lines.add(line);
        }

        return String.join("\n", lines);
    }

    private static String getIdentation(String line) {
        return " ".repeat(30 - line.length());
    }

    private static String getSpillingLoadInstruction(TempReg t, boolean debugMode) {
        // stack -> spill_reg
        return "mov " + t.register.getStackOffset() + "(%rsp), " + t.print(debugMode);
    }

    private static String getSpillingSaveInstruction(TempReg t, boolean debugMode) {
        // spill_reg -> stack
        return "mov "+ t.print(debugMode) + ", " + t.register.getStackOffset() + "(%rsp)";
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