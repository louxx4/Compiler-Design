package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.UselessRegister;

public record VirtualRegister(int id) implements UselessRegister {
    @Override
    public String toString() {
        return "%" + id();
    }
}
