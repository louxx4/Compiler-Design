package edu.kit.kastel.vads.compiler.parser.ast;

public sealed interface SimpleTree extends StatementTree permits AssignmentTree, DeclarationTree {}