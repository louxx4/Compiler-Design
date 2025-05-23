package edu.kit.kastel.vads.compiler.parser.ast;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

// block = unique identifier of enclosing block
public record DeclarationTree(TypeTree type, NameTree name, @Nullable ExpressionTree initializer, 
                                int block) implements SimpleTree {
    @Override
    public Span span() {
        if (initializer() != null) {
            return type().span().merge(initializer().span());
        }
        return type().span().merge(name().span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
