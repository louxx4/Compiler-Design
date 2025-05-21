package edu.kit.kastel.vads.compiler.parser.ast;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record IfStatementTree(ExpressionTree expression, StatementTree if_body, @Nullable StatementTree else_body, Position start) implements ControlTree {

    @Override
    public Span span() {
        if (else_body() != null) {
            return new Span.SimpleSpan(start(), else_body().span().end());
        }
        return new Span.SimpleSpan(start(), if_body().span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}