package edu.kit.kastel.vads.compiler.parser.ast;

public sealed interface LoopTree extends ControlTree permits WhileLoopTree, ForLoopTree {
}
