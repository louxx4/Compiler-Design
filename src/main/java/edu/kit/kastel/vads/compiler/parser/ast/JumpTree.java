package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.type.JumpType;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record JumpTree(JumpType type, int loopId, Span span) implements ControlTree {

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}