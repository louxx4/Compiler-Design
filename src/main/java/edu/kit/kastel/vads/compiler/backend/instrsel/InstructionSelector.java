package edu.kit.kastel.vads.compiler.backend.instrsel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.EmptyNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;

public class InstructionSelector {

    // Creates a maximal munch cover of the IR tree.
    // Expects a graph for every function within the program.
    public static void performIS(List<IrGraph> irGraphs) {
        List<Instruction> builder = new ArrayList<>();
        for (IrGraph functionGraph : irGraphs) {
            String funcName = functionGraph.name();
            maximalMunch(functionGraph.startBlock(), new HashSet<Node>(), builder);
        }
    }

    // Performs recursive maximal munch on a function graph, storing the resulting 
    // instruction sequence within a sorted list.
    public static TempReg maximalMunch(Node node, Set<Node> visited, List<Instruction> builder) {
        visited.add(node);
        TempReg res = new TempReg();
        
        switch (node) {
            case AddNode add -> {
                Node left = add.predecessor(0);
                Node right = add.predecessor(1);
                Pair children = new Pair(left, right);
                switch(children.pattern) {
                    case CONST_CONST -> builder.add(
                        new Instruction("mov", res, 
                            new Immediate(children.val_l + children.val_r)));
                    case CONST_LEFT -> builder.add(
                        new Instruction("add", res, 
                            new Immediate(children.val_l)));
                    case CONST_RIGHT -> builder.add(
                        new Instruction("add", res,
                            new Immediate(children.val_r)));
                    default -> {
                        res = maximalMunch(left, visited, builder);
                        TempReg t = maximalMunch(right, visited, builder);
                        builder.add(new Instruction("add", res, t));
                    }
                }
            }
            default -> {
                return null;
            }
        }

        return res;
    }

}