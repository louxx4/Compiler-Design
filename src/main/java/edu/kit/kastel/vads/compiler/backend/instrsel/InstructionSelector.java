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
import edu.kit.kastel.vads.compiler.ir.util.NodeSupport;

public class InstructionSelector {

    private Integer REG_COUNTER = 0;
    private Integer INSTR_COUNTER = 1;
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
        }
        return builder;
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
            case ProjNode proj -> res = handleProjNode(proj, builder);
            default -> {
                System.out.println("Instruction selection failed: Node was of type " + 
                    node.toString());
                return null;
            }
        }

        return res;
    }

    public void addFunctionPrologue(List<Instruction> instructions, int spilledRegs) {
        // add stack allocation for spilled registers
        if(spilledRegs > 0) {
            instructions.addFirst(new Instruction(0, 
                "sub", new Immediate(spilledRegs * 8), new FixReg("rsp")));
        }
    }

    public void addFunctionEpilogue(List<Instruction> instructions, int spilledRegs) {
        // add stack deallocation for spilled registers
        if(spilledRegs > 0) {
            instructions.addLast(new Instruction(INSTR_COUNTER++, 
                "add", new Immediate(spilledRegs * 8), new FixReg("rsp")));
        }
        instructions.addLast(new Instruction(INSTR_COUNTER++, "ret"));
    }

    public static String getGlobalPrologue() {
        return """
        .global main
        .global _main
        .text
        main:
        call _main
        # move the return value into the first argument for the syscall
        movq %rax, %rdi
        # move the exit syscall number into rax
        movq $0x3C, %rax
        syscall
        _main:
        """;
    }

    private TempReg newTempReg() {
        TempReg t = new TempReg(REG_COUNTER++);
        ALL_TREGS.add(t);
        return t;
    }

    private TempReg handleReturnNode(ReturnNode ret, List<Instruction> builder) {
        Node node = NodeSupport.predecessorSkipProj(ret, ReturnNode.RESULT);
        TempReg res;
        Instruction ins;

        switch(node) {
            case ConstIntNode c -> { 
                res = newTempReg();
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(c.value()), res);
                ins.def(res);
                builder.add(ins); 
            }
            default -> res = maximalMunch(node, builder);
        }

        return res;
    }

    private TempReg handleProjNode(ProjNode proj, List<Instruction> builder) {
        return maximalMunch(proj.predecessor(0), builder);
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
                    ins = new Instruction(INSTR_COUNTER++, 
                        "shl", new Immediate((int) (Math.log(children.val_l) / Math.log(2))), res);
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
                    ins = new Instruction(INSTR_COUNTER++, 
                        "shl", new Immediate((int) (Math.log(children.val_r) / Math.log(2))), res);
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
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(0), new FixReg("rdx"))); //clear rdx
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), new FixReg("rax"))); //move l to %rax
                ins = new Instruction(INSTR_COUNTER++, "mov", new Immediate(children.val_r), res);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++,
                    "idiv", res);
                ins.use(res);
                builder.add(ins); //divide %rax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("rdx"), res);
                ins.def(res);
                builder.add(ins); //get remainder from %rdx
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(0), new FixReg("rdx"))); //clear rdx
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), new FixReg("rax"))); //move l to %rax
                ins = new Instruction(INSTR_COUNTER++,
                    "idiv", t);
                ins.use(t);
                builder.add(ins); //divide %rax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("rdx"), res);
                ins.def(res);
                builder.add(ins); //get remainder from %rdx
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(0), new FixReg("rdx"))); //clear rdx
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, new FixReg("rax"));
                ins.use(t);
                builder.add(ins); //move l to %rax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "idiv", new Immediate(children.val_r))); //divide %rax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("rdx"), res);
                ins.def(res);
                builder.add(ins); //get remainder from %rdx
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(0), new FixReg("rdx"))); //clear rdx
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, new FixReg("rax"));
                ins.use(t1);
                builder.add(ins); //move l to %rax
                ins = new Instruction(INSTR_COUNTER++, 
                    "idiv", t2);
                ins.use(t2);
                builder.add(ins); //divide %rax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("rdx"), res);
                ins.def(res);
                builder.add(ins); //get remainder from %rdx
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
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(0), new FixReg("rdx"))); //clear rdx
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), new FixReg("rax"))); //move l to %rax
                ins = new Instruction(INSTR_COUNTER++, "mov", new Immediate(children.val_r), res);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++,
                    "idiv", res);
                ins.use(res);
                builder.add(ins); //divide %rax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("rax"), res);
                ins.def(res);
                builder.add(ins); //get quotient from %rax
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(0), new FixReg("rdx"))); //clear rdx
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), new FixReg("rax"))); //move l to %rax
                ins = new Instruction(INSTR_COUNTER++, 
                    "idiv", t);
                ins.use(t);
                builder.add(ins); //divide %rax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("rax"), res);
                ins.def(res);
                builder.add(ins); //get quotient from %rax
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(0), new FixReg("rdx"))); //clear rdx
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, new FixReg("rax"));
                ins.use(t);
                builder.add(ins); //move l to %rax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "idiv", new Immediate(children.val_r))); //divide %rax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("rax"), res);
                ins.def(res);
                builder.add(ins); //get quotient from %rax
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(0), new FixReg("rdx"))); //clear rdx
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, new FixReg("rax"));
                ins.use(t1);
                builder.add(ins); //move l to %rax
                ins = new Instruction(INSTR_COUNTER++, 
                    "idiv", t2);
                ins.use(t2);
                builder.add(ins); //divide %rax by r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("rax"), res);
                ins.def(res);
                builder.add(ins); //get quotient from %rax
            }
        }

        return res;
    }


}