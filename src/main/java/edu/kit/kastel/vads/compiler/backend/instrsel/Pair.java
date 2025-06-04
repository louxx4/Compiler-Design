package edu.kit.kastel.vads.compiler.backend.instrsel;

import edu.kit.kastel.vads.compiler.ir.node.ConstBoolNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;

public class Pair {

    public final Node left, right;
    public final Pattern pattern;
    public int val_l, val_r; //value left, value right (only for constants)
    public boolean b_l, b_r; //boolean value left, value right (only for bool constants)

    public Pair(Node left, Node right) {
        this.left = left;
        this.right = right;
        this.pattern = getPattern();
    }

    private Pattern getPattern() {
        switch (left) {
            case ConstIntNode c1 -> { 
                val_l = c1.value();
                switch (right) {
                    case ConstIntNode c2 -> { 
                        val_r = c2.value();
                        return Pattern.CONST_CONST;
                    }
                    default -> {
                        return Pattern.CONST_LEFT;
                    }
                }
            }
            case ConstBoolNode c1 -> { 
                b_l = c1.value();
                switch (right) {
                    case ConstBoolNode c2 -> { 
                        b_r = c2.value();
                        return Pattern.BOOL_BOOL;
                    }
                    default -> {
                        return Pattern.BOOL_LEFT;
                    }
                }
            }  
            default -> {
                switch(right) {
                    case ConstIntNode c2 -> { 
                        val_r = c2.value();
                        return Pattern.CONST_RIGHT;
                    }
                    case ConstBoolNode c2 -> { 
                        b_r = c2.value();
                        return Pattern.BOOL_RIGHT;
                    }
                    default -> {
                        return Pattern.STMT_STMT;
                    }
                }
            }
        }
    }
}