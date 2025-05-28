package edu.kit.kastel.vads.compiler.parser.ast;

public sealed interface ControlTree extends StatementTree permits IfStatementTree, LoopTree, ReturnTree, JumpTree {
}