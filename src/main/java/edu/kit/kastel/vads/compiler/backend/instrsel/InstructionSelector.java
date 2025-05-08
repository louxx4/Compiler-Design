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

    Integer REG_COUNTER = 0;

    // Creates a maximal munch cover of the IR tree.
    // Expects a graph for every function within the program.
    public List<Instruction> performIS(List<IrGraph> irGraphs) {
        List<Instruction> builder = new ArrayList<>();
        for (IrGraph functionGraph : irGraphs) {
            // String funcName = functionGraph.name();
            TempReg funcResult = maximalMunch(functionGraph.endBlock().predecessor(0), builder);
            builder.add(new Instruction("mov", new FixReg("eax"), funcResult));
            builder.add(new Instruction("ret"));
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

        switch(left) {
            case ConstIntNode c -> { 
                res = new TempReg(REG_COUNTER++);
                builder.add(new Instruction("mov", res, new Immediate(c.value()))); 
            }
            case ProjNode _ -> {
                Node right = ret.predecessor(1);
                switch(right) {
                    case ConstIntNode c2 -> {
                        res = new TempReg(REG_COUNTER++);
                        builder.add(new Instruction("mov", res, new Immediate(c2.value())));
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

        switch(children.pattern) {
            case CONST_CONST -> {
                res = new TempReg(REG_COUNTER++);
                builder.add(new Instruction("mov", res, 
                    new Immediate(children.val_l + children.val_r)));
            }
            case CONST_LEFT -> {
                res = maximalMunch(right, builder);
                builder.add(new Instruction("add", res, 
                    new Immediate(children.val_l))); 
            }
            case CONST_RIGHT -> { 
                res = maximalMunch(left, builder);
                builder.add(new Instruction("add", res,
                    new Immediate(children.val_r)));
            }
            default -> {
                res = maximalMunch(left, builder);
                TempReg t = maximalMunch(right, builder);
                builder.add(new Instruction("add", res, t));
            }
        }

        return res;
    }

    private TempReg handleSubNode(SubNode sub, List<Instruction> builder) {
        Node left = sub.predecessor(0);
        Node right = sub.predecessor(1);
        Pair children = new Pair(left, right);
        TempReg res;

        switch(children.pattern) {
            case CONST_CONST -> {
                res = new TempReg(REG_COUNTER++);
                builder.add(new Instruction("mov", res, 
                    new Immediate(children.val_l - children.val_r)));
            }
            case CONST_LEFT -> {
                res = maximalMunch(right, builder);
                builder.add(new Instruction("sub", 
                    new Immediate(children.val_l), res)); 
            }
            case CONST_RIGHT -> { 
                res = maximalMunch(left, builder);
                builder.add(new Instruction("sub", res,
                    new Immediate(children.val_r)));
            }
            default -> {
                res = maximalMunch(left, builder);
                TempReg t = maximalMunch(right, builder);
                builder.add(new Instruction("sub", res, t));
            }
        }

        return res;
    }

    private TempReg handleMulNode(MulNode mul, List<Instruction> builder) {
        Node left = mul.predecessor(0);
        Node right = mul.predecessor(1);
        Pair children = new Pair(left, right);
        TempReg res;

        switch(children.pattern) {
            case CONST_CONST -> {
                res = new TempReg(REG_COUNTER++);
                builder.add(new Instruction("mov", res, 
                    new Immediate(children.val_l * children.val_r)));
            }
            case CONST_LEFT -> {
                res = maximalMunch(right, builder);
                if(children.val_l % 2 == 0) {
                    //optimization: << instead of * (if imm is a power of 2)
                    builder.add(new Instruction("shl", 
                        new Immediate(children.val_l / 2), res)); 
                } else {
                    builder.add(new Instruction("imul", res,
                        new Immediate(children.val_l))); 
                }
            }
            case CONST_RIGHT -> {
                res = maximalMunch(left, builder);
                if(children.val_r % 2 == 0) {
                    //optimization: << instead of * (if imm is a power of 2)
                    builder.add(new Instruction("shl",
                        new Immediate(children.val_r / 2), res)); 
                } else {
                    builder.add(new Instruction("imul", res,
                        new Immediate(children.val_r))); 
                }
            }
            default -> {
                res = maximalMunch(left, builder);
                TempReg t = maximalMunch(right, builder);
                builder.add(new Instruction("imul", res, t));
            }
        }

        return res;
    }

    private TempReg handleModNode(ModNode mod, List<Instruction> builder) {
        Node left = mod.predecessor(0);
        Node right = mod.predecessor(1);
        Pair children = new Pair(left, right);
        TempReg res = new TempReg(REG_COUNTER++);

        switch(children.pattern) {
            case CONST_CONST -> {
                builder.add(new Instruction("mov", res, 
                    new Immediate(children.val_l % children.val_r)));
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                builder.add(new Instruction("mov", 
                    new FixReg("eax"), new Immediate(children.val_l))); //move l to %eax
                builder.add(new Instruction("idiv", t)); //divide %eax by r
                builder.add(new Instruction("mov", res, new FixReg("edx"))); //get remainder from %edx
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                builder.add(new Instruction("mov", new FixReg("eax"), t)); //move l to %eax
                builder.add(new Instruction("idiv", new Immediate(children.val_r))); //divide %eax by r
                builder.add(new Instruction("mov", res, new FixReg("edx"))); //get remainder from %edx
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                builder.add(new Instruction("mov", new FixReg("eax"), t1)); //move l to %eax
                builder.add(new Instruction("idiv", t2)); //divide %eax by r
                builder.add(new Instruction("mov", res, new FixReg("edx"))); //get remainder from %edx
            }
        }

        return res;
    }

    private TempReg handleDivNode(DivNode div, List<Instruction> builder) {
        Node left = div.predecessor(0);
        Node right = div.predecessor(1);
        Pair children = new Pair(left, right);
        TempReg res = new TempReg(REG_COUNTER++);

        switch(children.pattern) {
            case CONST_CONST -> {
                builder.add(new Instruction("mov", res, 
                    new Immediate(children.val_l / children.val_r)));
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                builder.add(new Instruction("mov", 
                    new FixReg("eax"), new Immediate(children.val_l))); //move l to %eax
                builder.add(new Instruction("idiv", t)); //divide %eax by r
                builder.add(new Instruction("mov", res, new FixReg("eax"))); //get quotient from %eax
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                builder.add(new Instruction("mov", new FixReg("eax"), t)); //move l to %eax
                builder.add(new Instruction("idiv", new Immediate(children.val_r))); //divide %eax by r
                builder.add(new Instruction("mov", res, new FixReg("eax"))); //get quotient from %eax
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                builder.add(new Instruction("mov", new FixReg("eax"), t1)); //move l to %eax
                builder.add(new Instruction("idiv", t2)); //divide %eax by r
                builder.add(new Instruction("mov", res, new FixReg("eax"))); //get quotient from %eax
            }
        }

        return res;
    }


}