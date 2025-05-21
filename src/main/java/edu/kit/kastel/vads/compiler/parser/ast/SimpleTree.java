package edu.kit.kastel.vads.compiler.parser.ast;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record SimpleTree(@Nullable AssignmentTree assignment, @Nullable DeclarationTree declaration) implements StatementTree {

    @Override
    public Span span() {
        if(assignment() != null) {
            return assignment().span();
        }
        assert declaration() != null;
        return declaration().span();
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }

}