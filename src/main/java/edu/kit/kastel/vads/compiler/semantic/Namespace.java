package edu.kit.kastel.vads.compiler.semantic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    public void put(Name name, T value, BinaryOperator<T> merger) {
        this.content.merge(name, value, merger);
    }

    public @Nullable T get(NameTree name) {
        return get(name.name());
    }

    // returns the innermost status
    public @Nullable T get(Name name) {
        T entry = getExclusive(name);
        return (entry == null
            ? getFromEnclosing(name)
            : entry);
    }

    // returns the status exclusively inside this namespace (not outside)
    public @Nullable T getExclusive(Name name) {
        return this.content.get(name);
    }

    // returns the innermost status from one of the enclosing namespaces
    public @Nullable T getFromEnclosing(Name name) {
        return (this.enclosing == null
            ? null
            : this.enclosing.get(name));
    }

    public @Nullable Namespace<T> getEnclosing() {
        return this.enclosing;
    }

    // returns the innermost enclosing namespace, that holds 
    // a status for the provided variable
    public @Nullable Namespace<T> getEnclosingWithStatus(Name name) {
        if(this.enclosing == null) return null;
        else if(this.enclosing.get(name) != null) return this.enclosing;
        else return this.enclosing.getEnclosingWithStatus(name);
    }

    public Set<Name> getKeys() {
        return this.content.keySet();
    }
}
