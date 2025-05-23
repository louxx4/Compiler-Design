package edu.kit.kastel.vads.compiler.parser.ast;

public sealed interface StatementTree extends Tree permits BlockTree, SimpleTree, ControlTree {
}
