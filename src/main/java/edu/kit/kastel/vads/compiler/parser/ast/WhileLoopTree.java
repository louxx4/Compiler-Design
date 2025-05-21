package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record WhileLoopTree(ExpressionTree expression, StatementTree statement, Position start) implements ControlTree {

    @Override
    public Span span() {
        return new Span.SimpleSpan(start(), statement.span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}