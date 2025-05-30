package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

// block = unique identifier of enclosing block
public record ReturnTree(ExpressionTree expression, int block, Position start) implements ControlTree {
    @Override
    public Span span() {
        return new Span.SimpleSpan(start(), expression().span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
