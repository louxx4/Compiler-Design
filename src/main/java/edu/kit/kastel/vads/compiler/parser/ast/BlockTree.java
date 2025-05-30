package edu.kit.kastel.vads.compiler.parser.ast;

import java.util.List;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

// block = unique identifier of the scope that this block defines
public record BlockTree(List<StatementTree> statements, int block, Span span) implements StatementTree {

    public BlockTree {
        statements = List.copyOf(statements);
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
