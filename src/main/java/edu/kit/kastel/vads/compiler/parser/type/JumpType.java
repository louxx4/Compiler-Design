package edu.kit.kastel.vads.compiler.parser.type;

import java.util.Locale;

public enum JumpType implements Type {
    CONTINUE, BREAK;

    @Override
    public String asString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
