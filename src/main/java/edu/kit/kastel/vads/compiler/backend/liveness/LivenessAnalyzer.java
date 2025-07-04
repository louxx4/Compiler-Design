package edu.kit.kastel.vads.compiler.backend.liveness;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.kit.kastel.vads.compiler.backend.instrsel.Instruction;
import edu.kit.kastel.vads.compiler.backend.instrsel.TempReg;

public class LivenessAnalyzer {

    public static void performLA(Instruction[] instructions) {
        for (int i = instructions.length - 1; i >= 0; i--) {
            // use(l,t) -> live(l,t)
            instructions[i].live(instructions[i].getUse()); //apply rule K1
            // succ(l) += (l+1)
            if(i < instructions.length - 1)
                instructions[i].succ(instructions[i+1]);
        }
        infereLiveness(instructions); //apply rule K2
    }

    // Apply liveness inference rule (K2) to instructions until no change occurs.
    // After saturation all instructions hold their live variables (here: temporary registers).
    public static void infereLiveness(Instruction[] instructions) {
        boolean saturated;
        do {
            saturated = true;
            for (Instruction l : instructions) {
                // live(l',t) + succ(l,l') + !def(l,t) -> live(l,t)
                for (Instruction l_ : (List<Instruction>) l.getSucc()) {
                    for (TempReg t : (List<TempReg>) l_.getLive()) {
                        if (l.isUndef(t) && !l.isLive(t)) { 
                            saturated = false;
                            l.live(t);
                        }
                    }
                }
            }
        } while (!saturated);
    }

    // Generates the inference graph out of the liveness information set on an instruction sequence.
    public static InterferenceGraph generateInterferenceGraph(List<Instruction> instructions, List<TempReg> tempRegisters) {
        // calculate interference
        for(Instruction ins : instructions) {
            ins.calculateInterference();
        }

        // generate a node for each temporary register
        Node[] allNodes = new Node[tempRegisters.size()];
        for(TempReg t : tempRegisters) {
            allNodes[t.id] = new Node(t);
        }

        // initialize adjacency list
        Set<Integer>[] adjSet = new Set[allNodes.length]; // uses sets to avoid duplicates
        for(int i = 0; i < adjSet.length; i++) {
            adjSet[i] = new HashSet<>();
        }

        // add edges between variables, if their liveness overlaps OR
        // if one is live and the other one is interfering
        for(Instruction ins : instructions) {
            List<TempReg> live = (List<TempReg>) ins.getLive(); // variables that are live in this instruction
            // get all interfering variables
            List<TempReg> interfering = ins.getDef();
            interfering.removeIf(t -> t.interferes());
            for(int i = 0; i < live.size(); i++) {
                int node = live.get(i).id;
                for(int j = 0; j < live.size(); j++) {
                    if(j != i) adjSet[node].add(live.get(j).id); // add neighbour
                }
                for(int j = 0; j < interfering.size(); j++) {
                    int interferingNode = interfering.get(j).id;
                    if(interferingNode != node) adjSet[node].add(interferingNode); // add interfering node
                }
            }

        }

        return new InterferenceGraph(adjSet, allNodes);
    }

}