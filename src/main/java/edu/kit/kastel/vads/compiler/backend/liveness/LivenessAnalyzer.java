package edu.kit.kastel.vads.compiler.backend.liveness;

import edu.kit.kastel.vads.compiler.backend.instrsel.Instruction;

public class LivenessAnalyzer {

    public static void performLA(Instruction[] instructions) {
        for (int i = 0; i < instructions.length; i++) {
            // use(l,t) -> live(l,t)
            instructions[i].live(instructions[i].getUse());
            // succ(l) += (l+1)
            if(i < instructions.length - 1)
                instructions[i].succ(instructions[i+1]);
        }
    }

}