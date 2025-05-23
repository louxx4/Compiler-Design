package edu.kit.kastel.vads.compiler.parser;

import org.jspecify.annotations.Nullable;

public class Scope {

    private @Nullable Scope parent;
    private final int id;

    public Scope(int id) {
        this.id = id;
    }

    public Scope(int id, Scope parent) {
        this(id);
        this.parent = parent;
    }

    public Scope getParent() {
        return this.parent;
    }

    public boolean hasParent() {
        return this.parent != null;
    }

    public int getId() {
        return this.id;
    }

}