package edu.kit.kastel.vads.compiler.parser.ast;

import java.util.List;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

// namespaces = holds amount of namespaces used within the program
public record ProgramTree(List<FunctionTree> topLevelTrees, int namespaces) implements Tree {
    public ProgramTree {
        assert !topLevelTrees.isEmpty() : "must be non-empty";
        topLevelTrees = List.copyOf(topLevelTrees);
    }
    @Override
    public Span span() {
        var first = topLevelTrees.getFirst();
        var last = topLevelTrees.getLast();
        return new Span.SimpleSpan(first.span().start(), last.span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
