package edu.kit.kastel.vads.compiler.backend.liveness;

import java.util.List;

import edu.kit.kastel.vads.compiler.backend.instrsel.Instruction;
import edu.kit.kastel.vads.compiler.backend.instrsel.TempReg;

public class LivenessAnalyzer {

    public static Instruction[] performLA(Instruction[] instructions) {
        for (int i = instructions.length - 1; i >= 0; i--) {
            // use(l,t) -> live(l,t)
            instructions[i].live(instructions[i].getUse()); //apply rule K1
            // succ(l) += (l+1)
            if(i < instructions.length - 1)
                instructions[i].succ(instructions[i+1]);
        }
        return infereLiveness(instructions); //apply rule K2
    }

    // Apply liveness inference rule (K2) to instructions until no change occurs.
    // After saturation all instructions hold their live variables (here: temporary registers).
    public static Instruction[] infereLiveness(Instruction[] instructions) {
        boolean saturated = false;
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

        return instructions;
    }

}