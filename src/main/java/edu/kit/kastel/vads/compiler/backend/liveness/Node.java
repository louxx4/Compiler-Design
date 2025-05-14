package edu.kit.kastel.vads.compiler.backend.liveness;

import edu.kit.kastel.vads.compiler.backend.instrsel.TempReg;

public class Node implements Comparable<Node> {

    private int wt = 0;
    private int color = -1; //color used for register allocation
    public final TempReg reg; //associated register

    public Node(TempReg associatedReg) {
        this.reg = associatedReg;
    }

    public int getWeight() {
        return this.wt;
    }

    public void incrWt() {
        this.wt++;
    }

    public void setColor(int color){
        this.color = color;
    }

    public int getColor() {
        return this.color;
    }

    public boolean isColored() {
        return (color == -1);
    }

    @Override
    public int compareTo(Node o) {
        if (this.wt > o.wt) return 1;
        else if(this.wt == o.wt) return 0;
        else return -1;
    }

}