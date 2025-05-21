package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;

public record Operator(OperatorType type, Span span) implements Token {

    @Override
    public boolean isOperator(OperatorType operatorType) {
        return type() == operatorType;
    }

    @Override
    public String asString() {
        return type().toString();
    }

    public enum OperatorType {
        ASSIGN_MINUS("-="),
        MINUS("-"),
        ASSIGN_PLUS("+="),
        PLUS("+"),
        MUL("*"),
        ASSIGN_MUL("*="),
        ASSIGN_DIV("/="),
        DIV("/"),
        ASSIGN_MOD("%="),
        MOD("%"),
        ASSIGN("="),
        LESS("<"),
        LEQ("<="),
        GREATER(">"),
        GEQ(">="),
        EQ("=="),
        NEQ("!="),
        AND("&&"),
        OR("||"),
        NOT("!"),
        AND_BW("&"), //bitwise
        OR_BW("|"),  //bitwise
        XOR_BW("^"), //bitwise
        NOT_BW("^"), //bitwise
        SHL("<<"),
        SHR(">>"),
        ASSIGN_AND("&="),
        ASSIGN_XOR("^="),
        ASSIGN_OR("|="),
        ASSIGN_SHR("<<="),
        ASSIGN_SHL(">>=")
        ;

        private final String value;

        OperatorType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
