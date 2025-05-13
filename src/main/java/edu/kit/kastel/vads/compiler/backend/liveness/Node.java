package edu.kit.kastel.vads.compiler.backend.liveness;

import edu.kit.kastel.vads.compiler.backend.instrsel.TempReg;

public class Node {

    private int wt = 0;
    public final TempReg reg; //associated register

    public Node(TempReg associatedReg) {
        this.reg = associatedReg;
    }

    public int getWeight() {
        return this.wt;
    }

    public void setWeight(int wt) {
        this.wt = wt;
    }

}