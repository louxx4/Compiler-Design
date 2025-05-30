package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.type.JumpType;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

// block = unique identifier of enclosing block
// loopId = unique identifier of deepest enclosing loop (so for nested loops the inner loop counts)
public record JumpTree(JumpType type, int loopId, int block, Span span) implements ControlTree {

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}