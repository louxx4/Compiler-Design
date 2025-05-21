package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.type.JumpType;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record JumpTree(JumpType type, Span _span) implements ControlTree {

    @Override
    public Span span() {
        return _span();
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}