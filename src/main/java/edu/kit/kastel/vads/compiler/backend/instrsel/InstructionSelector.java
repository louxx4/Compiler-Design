package edu.kit.kastel.vads.compiler.backend.instrsel;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;

public class InstructionSelector {

    private Integer REG_COUNTER = 0;
    private Integer INSTR_COUNTER = 0;
    public List<TempReg> ALL_TREGS = new ArrayList<>(); //holds all temporary registers (used for easier recalloc)

    // Creates a maximal munch cover of the IR tree.
    // Expects a graph for every function within the program.
    public List<Instruction> performIS(List<IrGraph> irGraphs) {
        List<Instruction> builder = new ArrayList<>();
        for (IrGraph functionGraph : irGraphs) {
            // String funcName = functionGraph.name();
            TempReg funcResult = maximalMunch(functionGraph.endBlock().predecessor(0), builder);
            Instruction ins = new Instruction(INSTR_COUNTER++, "mov", funcResult, new FixReg("rax"));
            ins.use(funcResult);
            builder.add(ins);
            builder.add(new Instruction(INSTR_COUNTER++, "ret"));
        }
        return builder;
    }

    private TempReg newTempReg() {
        TempReg t = new TempReg(REG_COUNTER++);
        ALL_TREGS.add(t);
        return t;
    }

    // Performs recursive maximal munch on a function graph, storing the resulting 
    // instruction sequence within a sorted list.
    public TempReg maximalMunch(Node node, List<Instruction> builder) {
        TempReg res;
        
        switch (node) {
            case AddNode add -> res = handleAddNode(add, builder);
            case SubNode sub -> res = handleSubNode(sub, builder);
            case MulNode mul -> res = handleMulNode(mul, builder);
            case ModNode mod -> res = handleModNode(mod, builder);
            case DivNode div -> res = handleDivNode(div, builder);
            case ReturnNode ret -> res = handleReturnNode(ret, builder);
            default -> {
                System.out.println("Node was of type " + node.toString());
                return null;
            }
        }

        return res;
    }

    private TempReg handleReturnNode(ReturnNode ret, List<Instruction> builder) {
        Node left = ret.predecessor(0);
        TempReg res;
        Instruction ins;

        switch(left) {
            case ConstIntNode c -> { 
                res = newTempReg();
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(c.value()), res);
                ins.def(res);
                builder.add(ins); 
            }
            case ProjNode _ -> {
                Node right = ret.predecessor(1);
                switch(right) {
                    case ConstIntNode c2 -> {
                        res = newTempReg();
                        ins = new Instruction(INSTR_COUNTER++, 
                            "mov", new Immediate(c2.value()), res);
                        ins.def(res);
                        builder.add(ins);
                    }
                    default -> res = maximalMunch(right, builder);
                }
            }
            default -> res = maximalMunch(left, builder);
        }

        return res;
    }

    private TempReg handleAddNode(AddNode add, List<Instruction> builder) {
        Node left = add.predecessor(0);
        Node right = add.predecessor(1);
        Pair children = new Pair(left, right);
        TempReg res;
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                res = newTempReg();
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l + children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                res = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "add", new Immediate(children.val_l), res);
                ins.def(res);
                ins.use(res);
                builder.add(ins); 
            }
            case CONST_RIGHT -> { 
                res = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "add", new Immediate(children.val_r), res);
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
            default -> {
                res = maximalMunch(left, builder);
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, "add", t, res);
                ins.def(res);
                ins.use(t);
                ins.use(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleSubNode(SubNode sub, List<Instruction> builder) {
        Node left = sub.predecessor(0);
        Node right = sub.predecessor(1);
        Pair children = new Pair(left, right);
        TempReg res;
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                res = newTempReg();
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l - children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                res = newTempReg();
                TempReg t = maximalMunch(right, builder); // t <- right
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), res); // res <- imm
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sub", t, res); // res = res - t 
                ins.def(res);
                ins.use(t);
                ins.use(res);
                builder.add(ins); 
            }
            case CONST_RIGHT -> { 
                res = maximalMunch(left, builder); // res <- left
                ins = new Instruction(INSTR_COUNTER++, 
                    "sub", new Immediate(children.val_r), res); // res = res - imm
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
            default -> {
                res = maximalMunch(left, builder);
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, "sub", t, res);
                ins.def(res);
                ins.use(t);
                ins.use(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleMulNode(MulNode mul, List<Instruction> builder) {
        Node left = mul.predecessor(0);
        Node right = mul.predecessor(1);
        Pair children = new Pair(left, right);
        TempReg res;
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                res = newTempReg();
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l * children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                res = maximalMunch(right, builder);
                if((Math.log(children.val_l) / Math.log(2)) % 1 == 0) {
                    //optimization: << instead of * (if imm is a power of 2)
                    builder.add(new Instruction(INSTR_COUNTER++, 
                        "mov", new Immediate((int) (Math.log(children.val_l) / Math.log(2))), new FixReg("cl")));
                    ins = new Instruction(INSTR_COUNTER++, 
                        "shl", res);
                    ins.def(res);
                    ins.use(res);
                    builder.add(ins); 
                } else {
                    ins = new Instruction(INSTR_COUNTER++, 
                        "imul", new Immediate(children.val_l), res);
                    ins.def(res);
                    ins.use(res);
                    builder.add(ins); 
                }
            }
            case CONST_RIGHT -> {
                res = maximalMunch(left, builder);
                if((Math.log(children.val_r) / Math.log(2)) % 1 == 0) {
                    //optimization: << instead of * (if imm is a power of 2)
                    builder.add(new Instruction(INSTR_COUNTER++, 
                        "mov", new Immediate((int) (Math.log(children.val_r) / Math.log(2))), new FixReg("cl")));
                    ins = new Instruction(INSTR_COUNTER++, 
                        "shl", res);
                    ins.def(res);
                    ins.use(res);
                    builder.add(ins); 
                } else {
                    ins = new Instruction(INSTR_COUNTER++, 
                        "imul", new Immediate(children.val_r), res);
                    ins.def(res);
                    ins.use(res);
                    builder.add(ins); 
                }
            }
            default -> {
                res = maximalMunch(left, builder);
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, "imul", t, res);
                ins.def(res);
                ins.use(t);
                ins.use(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleModNode(ModNode mod, List<Instruction> builder) {
        Node left = mod.predecessor(0);
        Node right = mod.predecessor(1);
        Pair children = new Pair(left, right);
        TempReg res = newTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l % children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), new FixReg("eax"))); //move l to %eax
                ins = new Instruction(INSTR_COUNTER++,
                    "idiv", t);
                ins.use(t);
                builder.add(ins); //divide %eax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("edx"), res);
                ins.def(res);
                builder.add(ins); //get remainder from %edx
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, new FixReg("eax"));
                ins.use(t);
                builder.add(ins); //move l to %eax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "idiv", new Immediate(children.val_r))); //divide %eax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("edx"), res);
                ins.def(res);
                builder.add(ins); //get remainder from %edx
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, new FixReg("eax"));
                ins.use(t1);
                builder.add(ins); //move l to %eax
                ins = new Instruction(INSTR_COUNTER++, 
                    "idiv", t2);
                ins.use(t2);
                builder.add(ins); //divide %eax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("edx"), res);
                ins.def(res);
                builder.add(ins); //get remainder from %edx
            }
        }

        return res;
    }

    private TempReg handleDivNode(DivNode div, List<Instruction> builder) {
        Node left = div.predecessor(0);
        Node right = div.predecessor(1);
        Pair children = new Pair(left, right);
        TempReg res = newTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l / children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), new FixReg("eax"))); //move l to %eax
                ins = new Instruction(INSTR_COUNTER++, 
                    "idiv", t);
                ins.use(t);
                builder.add(ins); //divide %eax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("eax"), res);
                ins.def(res);
                builder.add(ins); //get quotient from %eax
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, new FixReg("eax"));
                ins.use(t);
                builder.add(ins); //move l to %eax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "idiv", new Immediate(children.val_r))); //divide %eax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("eax"), res);
                ins.def(res);
                builder.add(ins); //get quotient from %eax
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, new FixReg("eax"));
                ins.use(t1);
                builder.add(ins); //move l to %eax
                ins = new Instruction(INSTR_COUNTER++, 
                    "idiv", t2);
                ins.use(t2);
                builder.add(ins); //divide %eax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("eax"), res);
                ins.def(res);
                builder.add(ins); //get quotient from %eax
            }
        }

        return res;
    }


}