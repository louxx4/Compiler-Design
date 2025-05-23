package edu.kit.kastel.vads.compiler.semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;

public class Namespace<T> {

    private final Map<Name, T> content;
    private @Nullable Namespace<T> enclosing; //e.g. for nested blocks

    public Namespace() {
        this.content = new HashMap<>();
    }

    public void setEnclosingNamespace(Namespace<T> enclosing) {
        this.enclosing = enclosing;
    }

    public void put(NameTree name, T value, BinaryOperator<T> merger) {
        this.content.merge(name.name(), value, merger);
    }

    public @Nullable T get(NameTree name) {
        T entry = this.content.get(name.name());
        if(entry == null && this.enclosing != null) {
            return this.enclosing.get(name);
        } else {
            return entry;
        }
    }
}
