package edu.kit.kastel.vads.compiler.backend.instrsel;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.AndBWNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstBoolNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.EqualsNode;
import edu.kit.kastel.vads.compiler.ir.node.EqualsNotNode;
import edu.kit.kastel.vads.compiler.ir.node.GreaterEqualNode;
import edu.kit.kastel.vads.compiler.ir.node.GreaterNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.NotBWNode;
import edu.kit.kastel.vads.compiler.ir.node.NotNode;
import edu.kit.kastel.vads.compiler.ir.node.OrBWNode;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.ShiftLeftNode;
import edu.kit.kastel.vads.compiler.ir.node.ShiftRightNode;
import edu.kit.kastel.vads.compiler.ir.node.SmallerEqualNode;
import edu.kit.kastel.vads.compiler.ir.node.SmallerNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;
import edu.kit.kastel.vads.compiler.ir.node.XorNode;
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
            Instruction ins = new Instruction(INSTR_COUNTER++, "mov", funcResult, new FixReg("eax"));
            ins.use(funcResult);
            builder.add(ins);
            builder.add(new Instruction(INSTR_COUNTER++, "cltq"));
        }
        return builder;
    }

    // Performs recursive maximal munch on a function graph, storing the resulting 
    // instruction sequence within a sorted list.
    public TempReg maximalMunch(Node node, List<Instruction> builder) {
        TempReg res;

        if(node.instructionInfo.wasVisited()) {
            res = node.instructionInfo.getRegister();
        } else {
            switch (node) {
                case AddNode add                -> res = handleAddNode(add, builder);
                case SubNode sub                -> res = handleSubNode(sub, builder);
                case MulNode mul                -> res = handleMulNode(mul, builder);
                case ModNode mod                -> res = handleModNode(mod, builder);
                case DivNode div                -> res = handleDivNode(div, builder);
                case ReturnNode ret             -> res = handleReturnNode(ret, builder);
                case ProjNode proj              -> res = handleProjNode(proj, builder);
                case EqualsNode eq              -> res = handleEqualsNode(eq, builder);
                case EqualsNotNode neq          -> res = handleEqualsNotNode(neq, builder);
                case AndBWNode andBW            -> res = handleAndBWNode(andBW, builder);
                case OrBWNode orBW              -> res = handleOrBWNode(orBW, builder);
                case XorNode xor                -> res = handleXorNode(xor, builder);
                case NotNode not                -> res = handleNotNode(not, builder);
                case NotBWNode notBW            -> res = handleNotBWNode(notBW, builder);
                case ShiftLeftNode shiftLeft    -> res = handleShiftLeftNode(shiftLeft, builder);
                case ShiftRightNode shiftRight  -> res = handleShiftRightNode(shiftRight, builder);
                case GreaterNode greater        -> res = handleGreaterNode(greater, builder);
                case GreaterEqualNode greaterEq -> res = handleGreaterEqualNode(greaterEq, builder);
                case SmallerNode smaller        -> res = handleSmallerNode(smaller, builder);
                case SmallerEqualNode smallerEq -> res = handleSmallerEqualNode(smallerEq, builder);
                default -> {
                    System.out.println("Instruction selection failed: Node was of type " + 
                        node.toString());
                    return null;
                }
            }
            node.instructionInfo.setRegister(res); //save result register at Node
            node.instructionInfo.visit(); //mark as visited
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
        return newTempReg(TempReg.RegisterSize.DOUBLE_WORD);
    }

    private TempReg newBoolTempReg() {
        return newTempReg(TempReg.RegisterSize.BYTE);
    }

    private TempReg newTempReg(TempReg.RegisterSize size) {
        TempReg t = new TempReg(REG_COUNTER++, size);
        ALL_TREGS.add(t);
        return t;
    }

    private TempReg handleGreaterNode(GreaterNode greater, List<Instruction> builder) {
        Node left = greater.predecessor(GreaterNode.LEFT);
        Node right = greater.predecessor(GreaterNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newBoolTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new BooleanValue(children.val_l > children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", new Immediate(children.val_l), t); // left > right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setg", res); // left > right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new Immediate(children.val_r)); // left > right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setg", res); // left > right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t1, t2); // left > right?
                ins.use(t1);
                ins.use(t2);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setg", res); // left > right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleGreaterEqualNode(GreaterEqualNode greaterEq, List<Instruction> builder) {
        Node left = greaterEq.predecessor(GreaterEqualNode.LEFT);
        Node right = greaterEq.predecessor(GreaterEqualNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newBoolTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new BooleanValue(children.val_l >= children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", new Immediate(children.val_l), t); // left >= right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setge", res); // left >= right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new Immediate(children.val_r)); // left >= right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setge", res); // left >= right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t1, t2); // left >= right?
                ins.use(t1);
                ins.use(t2);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setge", res); // left >= right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleSmallerEqualNode(SmallerEqualNode smallerEq, List<Instruction> builder) {
        Node left = smallerEq.predecessor(SmallerEqualNode.LEFT);
        Node right = smallerEq.predecessor(SmallerEqualNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newBoolTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new BooleanValue(children.val_l <= children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", new Immediate(children.val_l), t); // left <= right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setle", res); // left <= right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new Immediate(children.val_r)); // left <= right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setle", res); // left <= right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t1, t2); // left <= right?
                ins.use(t1);
                ins.use(t2);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setle", res); // left <= right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleSmallerNode(SmallerNode smaller, List<Instruction> builder) {
        Node left = smaller.predecessor(SmallerNode.LEFT);
        Node right = smaller.predecessor(SmallerNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newBoolTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new BooleanValue(children.val_l < children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", new Immediate(children.val_l), t); // left < right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setl", res); // left < right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new Immediate(children.val_r)); // left < right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setl", res); // left < right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t1, t2); // left < right?
                ins.use(t1);
                ins.use(t2);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setl", res); // left < right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleShiftLeftNode(ShiftLeftNode shiftLeft, List<Instruction> builder) {
        Node left = shiftLeft.predecessor(ShiftLeftNode.LEFT);
        Node right = shiftLeft.predecessor(ShiftLeftNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> { // const << const
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), res); // res <- l
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sal", new Immediate(children.val_r), res); // res = (res << r)
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
            case CONST_LEFT -> { // const << right
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, new FixReg("cl")); // %cl <- r
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", res, new Immediate(children.val_l)); // res <- l
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sal", new FixReg("cl"), res); // res = (res << %cl)
                ins.def(res);
                ins.use(res);
                builder.add(ins); 
            }
            case CONST_RIGHT -> { // left << const
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, res); // res <- l
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sal", new Immediate(children.val_r), res); // res = (res << const)
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
            default -> { // left << right
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, res); // res <- l
                ins.use(t1);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t2, new FixReg("cl")); // %cl <- r
                ins.use(t2);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sal", new FixReg("cl"), res); // // res = (res << %cl)
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleShiftRightNode(ShiftRightNode shiftRight, List<Instruction> builder) {
        Node left = shiftRight.predecessor(ShiftRightNode.LEFT);
        Node right = shiftRight.predecessor(ShiftRightNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> { // const >> const
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), res); // res <- l
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sar", new Immediate(children.val_r), res); // res = (res >> r)
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
            case CONST_LEFT -> { // const >> right
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, new FixReg("cl")); // %cl <- r
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", res, new Immediate(children.val_l)); // res <- l
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sar", new FixReg("cl"), res); // res = (res >> %cl)
                ins.def(res);
                ins.use(res);
                builder.add(ins); 
            }
            case CONST_RIGHT -> { // left >> const
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, res); // res <- l
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sar", new Immediate(children.val_r), res); // res = (res >> const)
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
            default -> { // left >> right
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, res); // res <- l
                ins.use(t1);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t2, new FixReg("cl")); // %cl <- r
                ins.use(t2);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sar", new FixReg("cl"), res); // // res = (res >> %cl)
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
        }

        return res;
    }
    
    private TempReg handleNotNode(NotNode not, List<Instruction> builder) {
        Node right = not.predecessor(NotNode.RIGHT);
        TempReg res = newBoolTempReg();
        Instruction ins;

        switch(right) {
            case ConstBoolNode c -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new BooleanValue(!c.value()), res);
                ins.def(res);
                builder.add(ins);
            }
            default -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new BooleanValue(true)); // t == true ?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setne", res); // t == true ? res = 0 : res = 1;
                ins.def(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleNotBWNode(NotBWNode notBW, List<Instruction> builder) {
        Node right = notBW.predecessor(NotBWNode.RIGHT);
        TempReg res = newTempReg();
        Instruction ins;

        switch(right) {
            case ConstIntNode c -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(~c.value()), res);
                ins.def(res);
                builder.add(ins);
            }
            default -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, res); // res <- t
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "not", res); // res ~= res
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
        }
        
        return res;
    }

    private TempReg handleAndBWNode(AndBWNode andBW, List<Instruction> builder) {
        Node left = andBW.predecessor(AndBWNode.LEFT);
        Node right = andBW.predecessor(AndBWNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l & children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, res); // res <- r
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "and", new Immediate(children.val_l), res); // res &= l
                ins.def(res);
                ins.use(res);
                builder.add(ins); 
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, res); // res <- l
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "and", new Immediate(children.val_r), res); // res &= r
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, res); // res <- l
                ins.use(t1);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, "and", t2, res); // res &= r
                ins.def(res);
                ins.use(t2);
                ins.use(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleOrBWNode(OrBWNode orBW, List<Instruction> builder) {
        Node left = orBW.predecessor(OrBWNode.LEFT);
        Node right = orBW.predecessor(OrBWNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l | children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, res); // res <- r
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "or", new Immediate(children.val_l), res); // res |= l
                ins.def(res);
                ins.use(res);
                builder.add(ins); 
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, res); // res <- l
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "or", new Immediate(children.val_r), res); // res |= r
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, res); // res <- l
                ins.use(t1);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "or", t2, res); // res |= r
                ins.def(res);
                ins.use(t2);
                ins.use(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleXorNode(XorNode xor, List<Instruction> builder) {
        Node left = xor.predecessor(XorNode.LEFT);
        Node right = xor.predecessor(XorNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l ^ children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, res); // res <- r
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "xor", new Immediate(children.val_l), res); // res ^= l
                ins.def(res);
                ins.use(res);
                builder.add(ins); 
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, res); // res <- l
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "xor", new Immediate(children.val_r), res); // res ^= r
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, res); // res <- l
                ins.use(t1);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "xor", t2, res); // res ^= r
                ins.def(res);
                ins.use(t2);
                ins.use(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleEqualsNode(EqualsNode eq, List<Instruction> builder) {
        Node left = eq.predecessor(EqualsNode.LEFT);
        Node right = eq.predecessor(EqualsNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newBoolTempReg();
        Instruction ins;

        switch(children.pattern) {
            case BOOL_BOOL -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new BooleanValue(children.b_l == children.b_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new BooleanValue(children.val_l == children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case BOOL_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new BooleanValue(children.b_l)); // left == right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sete", res); // left == right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new Immediate(children.val_l)); // left == right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sete", res); // left == right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case BOOL_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new BooleanValue(children.b_r)); // left == right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sete", res); // left == right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new Immediate(children.val_r)); // left == right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sete", res); // left == right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case STMT_STMT -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t1, t2); // left == right?
                ins.use(t1);
                ins.use(t2);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sete", res); // left == right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleEqualsNotNode(EqualsNotNode neq, List<Instruction> builder) {
        Node left = neq.predecessor(EqualsNotNode.LEFT);
        Node right = neq.predecessor(EqualsNotNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newBoolTempReg();
        Instruction ins;

        switch(children.pattern) {
            case BOOL_BOOL -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new BooleanValue(children.b_l != children.b_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new BooleanValue(children.val_l != children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case BOOL_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new BooleanValue(children.b_l)); // left != right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setne", res); // left != right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new Immediate(children.val_l)); // left != right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setne", res); // left != right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case BOOL_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new BooleanValue(children.b_r)); // left != right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setne", res); // left != right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t, new Immediate(children.val_r)); // left != right?
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setne", res); // left != right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
            case STMT_STMT -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "cmp", t1, t2); // left != right?
                ins.use(t1);
                ins.use(t2);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "setne", res); // left != right ? res = 1 : res = 0;
                ins.def(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleReturnNode(ReturnNode ret, List<Instruction> builder) {
        Node resNode = NodeSupport.predecessorSkipProj(ret, ReturnNode.RESULT);
        Node sideEffect = NodeSupport.predecessorSkipProj(ret, ReturnNode.SIDE_EFFECT);
        TempReg res;
        Instruction ins;
        
        if(!(sideEffect.equals(resNode) || sideEffect instanceof StartNode)) {
            maximalMunch(sideEffect, builder); //ignore result (only trigger side effect)
        }

        switch(resNode) {
            case ConstIntNode c -> { 
                res = newTempReg();
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(c.value()), res);
                ins.def(res);
                builder.add(ins); 
            }
            default -> res = maximalMunch(resNode, builder);
        }

        return res;
    }

    private TempReg handleProjNode(ProjNode proj, List<Instruction> builder) {
        return maximalMunch(proj.predecessor(ProjNode.IN), builder);
    }

    private TempReg handleAddNode(AddNode add, List<Instruction> builder) {
        Node left = add.predecessor(AddNode.LEFT);
        Node right = add.predecessor(AddNode.RIGHT);
        Pair children = new Pair(left, right);
        TempReg res = newTempReg();
        Instruction ins;

        switch(children.pattern) {
            case CONST_CONST -> {
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l + children.val_r), res);
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, res); // res <- r
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "add", new Immediate(children.val_l), res); // res += l
                ins.def(res);
                ins.use(res);
                builder.add(ins); 
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, res); // res <- l
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "add", new Immediate(children.val_r), res); // res += r
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, res); // res <- l
                ins.use(t1);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "add", t2, res); // res += r
                ins.def(res);
                ins.use(t2);
                ins.use(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleSubNode(SubNode sub, List<Instruction> builder) {
        Node left = sub.predecessor(SubNode.LEFT);
        Node right = sub.predecessor(SubNode.RIGHT);
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
                TempReg t = maximalMunch(right, builder); // t <- r
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), res); // res <- l
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sub", t, res); // res -= t 
                ins.def(res);
                ins.use(t);
                ins.use(res);
                builder.add(ins); 
            }
            case CONST_RIGHT -> { 
                res = newTempReg();
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++,
                    "mov", t, res); // res <- l
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "sub", new Immediate(children.val_r), res); // res -= r
                ins.def(res);
                ins.use(res);
                builder.add(ins);
            }
            default -> {
                res = newTempReg();
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++,
                    "mov", t1, res); // res <- l
                ins.use(t1);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, "sub", t2, res); // res -= r
                ins.def(res);
                ins.use(t2);
                ins.use(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleMulNode(MulNode mul, List<Instruction> builder) {
        Node left = mul.predecessor(MulNode.LEFT);
        Node right = mul.predecessor(MulNode.RIGHT);
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
                res = newTempReg();
                TempReg t = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++,
                    "mov", t, res); // res <- r
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                if((Math.log(children.val_l) / Math.log(2)) % 1 == 0) {
                    //optimization: << instead of * (if imm is a power of 2)
                    ins = new Instruction(INSTR_COUNTER++, 
                        "shl", new Immediate((int) (Math.log(children.val_l) / Math.log(2))), res); //shift res
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
                res = newTempReg();
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++,
                    "mov", t, res); // res <- l
                ins.use(t);
                ins.def(res);
                builder.add(ins);
                if((Math.log(children.val_r) / Math.log(2)) % 1 == 0) {
                    //optimization: << instead of * (if imm is a power of 2)
                    ins = new Instruction(INSTR_COUNTER++, 
                        "shl", new Immediate((int) (Math.log(children.val_r) / Math.log(2))), res); //shift res
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
                res = newTempReg();
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++,
                    "mov", t1, res); // res <- l
                ins.use(t1);
                ins.def(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "imul", t2, res); // res *= r
                ins.def(res);
                ins.use(t2);
                ins.use(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleModNode(ModNode mod, List<Instruction> builder) {
        Node left = mod.predecessor(ModNode.LEFT);
        Node right = mod.predecessor(ModNode.RIGHT);
        Node sideEffect = NodeSupport.predecessorSkipProj(mod, ModNode.SIDE_EFFECT);
        Pair children = new Pair(left, right);
        TempReg res = newTempReg();
        Instruction ins;

        if(!(sideEffect.equals(left) || sideEffect.equals(right) || sideEffect instanceof StartNode)){ 
            maximalMunch(sideEffect, builder); //ignore result (only trigger side effect)
        }

        switch(children.pattern) {
            case CONST_CONST -> {
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), new FixReg("eax"))); //move l to %eax
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_r), res); //move r to res
                ins.def(res);
                builder.add(ins);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cltq")); //extend to rax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cqto")); //clear rdx
                ins = new Instruction(INSTR_COUNTER++,
                    "idiv", res); //divide %edx:%eax by res
                ins.use(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("edx"), res); //get remainder from %edx
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), new FixReg("eax"))); //move l to %eax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cltq")); //extend to rax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cqto")); //clear rdx
                ins = new Instruction(INSTR_COUNTER++,
                    "idiv", t); //divide %eax:%edx by r
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("edx"), res); //get remainder from %edx
                ins.def(res);
                builder.add(ins);
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, new FixReg("eax")); //move l to %eax
                ins.use(t);
                builder.add(ins);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cltq")); //extend to rax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cqto")); //clear rdx
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_r), res); //move r to res
                ins.def(res);
                builder.add(ins);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "idiv", res)); //divide %edx:%eax by res
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("edx"), res); //get remainder from %edx
                ins.def(res);
                builder.add(ins);
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, new FixReg("eax")); //move l to %eax
                ins.use(t1);
                builder.add(ins);                
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cltq")); //extend to rax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cqto")); //clear rdx
                ins = new Instruction(INSTR_COUNTER++, 
                    "idiv", t2); //divide %eax:%edx by r
                ins.use(t2);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("edx"), res); //get remainder from %edx
                ins.def(res);
                builder.add(ins);
            }
        }

        return res;
    }

    private TempReg handleDivNode(DivNode div, List<Instruction> builder) {
        Node left = div.predecessor(DivNode.LEFT);
        Node right = div.predecessor(DivNode.RIGHT);
        Node sideEffect = NodeSupport.predecessorSkipProj(div, DivNode.SIDE_EFFECT);

        Pair children = new Pair(left, right);
        TempReg res = newTempReg();
        Instruction ins;

        if(!(sideEffect.equals(left) || sideEffect.equals(right) || sideEffect instanceof StartNode)){ 
            maximalMunch(sideEffect, builder); //ignore result (only trigger side effect)
        }

        switch(children.pattern) {
            case CONST_CONST -> {
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), new FixReg("eax"))); //move l to %eax
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_r), res); //move r to res
                ins.def(res);
                builder.add(ins);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cltq")); //extend to rax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cqto")); //clear rdx
                ins = new Instruction(INSTR_COUNTER++,
                    "idiv", res); //divide %eax:%edx by r
                ins.use(res);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("eax"), res); //get quotient from %eax
                ins.def(res);
                builder.add(ins);
            }
            case CONST_LEFT -> {
                TempReg t = maximalMunch(right, builder);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_l), new FixReg("eax"))); //move l to %eax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cltq")); //extend to rax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cqto")); //clear rdx
                ins = new Instruction(INSTR_COUNTER++, 
                    "idiv", t); //divide %eax:%edx by r
                ins.use(t);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("eax"), res); //get quotient from %eax
                ins.def(res);
                builder.add(ins);
            }
            case CONST_RIGHT -> {
                TempReg t = maximalMunch(left, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t, new FixReg("eax")); //move l to %eax
                ins.use(t);
                builder.add(ins);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cltq")); //extend to rax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cqto")); //clear rdx
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new Immediate(children.val_r), res); //move r to res
                ins.def(res);
                builder.add(ins);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "idiv", res)); //divide %eax:%edx by res
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("eax"), res); //get quotient from %eax
                ins.def(res);
                builder.add(ins);
            }
            default -> {
                TempReg t1 = maximalMunch(left, builder);
                TempReg t2 = maximalMunch(right, builder);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", t1, new FixReg("eax")); //move l to %eax
                ins.use(t1);
                builder.add(ins);
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cltq")); //extend to rax
                builder.add(new Instruction(INSTR_COUNTER++, 
                    "cqto")); //clear rdx
                ins = new Instruction(INSTR_COUNTER++, 
                    "idiv", t2); //divide %eax:%edx by r
                ins.use(t2);
                builder.add(ins);
                ins = new Instruction(INSTR_COUNTER++, 
                    "mov", new FixReg("eax"), res); //get quotient from %eax
                ins.def(res);
                builder.add(ins);
            }
        }

        return res;
    }


}