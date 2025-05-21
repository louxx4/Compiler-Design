package edu.kit.kastel.vads.compiler.parser.ast;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record IfStatementTree(ExpressionTree expression, StatementTree if_statement, @Nullable StatementTree else_statement, Position start) implements StatementTree {

    @Override
    public Span span() {
        if (else_statement() != null) {
            return new Span.SimpleSpan(start(), else_statement().span().end());
        }
        return new Span.SimpleSpan(start(), if_statement().span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}