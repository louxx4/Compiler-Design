package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record ConditionalTree(ExpressionTree lhs, ExpressionTree if_expression, ExpressionTree else_expression) implements ExpressionTree {

    @Override
    public Span span() {
        return new Span.SimpleSpan(lhs().span().start(), else_expression().span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
