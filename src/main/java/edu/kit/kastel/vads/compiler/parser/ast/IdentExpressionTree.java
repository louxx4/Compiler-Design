package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

// block = unique identifier of enclosing block 
public record IdentExpressionTree(NameTree nameTree, int block) implements ExpressionTree {
    @Override
    public Span span() {
        return nameTree().span();
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
