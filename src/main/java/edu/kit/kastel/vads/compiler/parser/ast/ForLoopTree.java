package edu.kit.kastel.vads.compiler.parser.ast;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record ForLoopTree(@Nullable SimpleTree initialization, ExpressionTree condition, 
    @Nullable SimpleTree advancement, StatementTree body, Position start) implements ControlTree {

    @Override
    public Span span() {
        return new Span.SimpleSpan(start(), body().span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}