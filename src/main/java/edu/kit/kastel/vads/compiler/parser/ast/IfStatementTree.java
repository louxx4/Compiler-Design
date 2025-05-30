package edu.kit.kastel.vads.compiler.parser.ast;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.Position;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

// block = unique identifier of enclosing block
public record IfStatementTree(ExpressionTree expression, StatementTree if_body, 
    @Nullable StatementTree else_body, int block_if, int block_else, 
    Position start) implements ControlTree {

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

    public static boolean isValidBlock(int blockId) {
        return blockId != -1;
    }

}