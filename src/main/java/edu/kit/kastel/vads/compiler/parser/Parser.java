package edu.kit.kastel.vads.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Identifier;
import edu.kit.kastel.vads.compiler.lexer.Keyword;
import edu.kit.kastel.vads.compiler.lexer.KeywordType;
import edu.kit.kastel.vads.compiler.lexer.NumberLiteral;
import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.lexer.Separator;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.lexer.Token;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForLoopTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfStatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.SimpTree;
import edu.kit.kastel.vads.compiler.parser.ast.SimpleTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileLoopTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;

public class Parser {
    private final TokenSource tokenSource;

    public Parser(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }

    public ProgramTree parseProgram() {
        ProgramTree programTree = new ProgramTree(List.of(parseFunction()));
        if (this.tokenSource.hasMore()) {
            throw new ParseException("expected end of input but got " + this.tokenSource.peek());
        }
        return programTree;
    }

    private FunctionTree parseFunction() {
        Keyword returnType = this.tokenSource.expectKeyword(KeywordType.INT);
        Identifier identifier = this.tokenSource.expectIdentifier();
        if (!identifier.value().equals("main")) {
            throw new ParseException("expected main function but got " + identifier);
        }
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        BlockTree body = parseBlock();
        return new FunctionTree(
            new TypeTree(BasicType.INT, returnType.span()),
            name(identifier),
            body
        );
    }

    private BlockTree parseBlock() {
        Separator bodyOpen = this.tokenSource.expectSeparator(SeparatorType.BRACE_OPEN);
        List<StatementTree> statements = new ArrayList<>();
        while (!(this.tokenSource.peek() instanceof Separator sep && sep.type() == SeparatorType.BRACE_CLOSE)) {
            statements.add(parseStatement());
        }
        Separator bodyClose = this.tokenSource.expectSeparator(SeparatorType.BRACE_CLOSE);
        return new BlockTree(statements, bodyOpen.span().merge(bodyClose.span()));
    }

    private StatementTree parseStatement() {
        StatementTree statement;
        Token next = this.tokenSource.peek();
        if (next.isKeyword(KeywordType.INT)) {
            statement = parseDeclaration(KeywordType.INT);
        } else if(next.isKeyword(KeywordType.BOOL)) {
            statement = parseDeclaration(KeywordType.BOOL);
        } else if (next.isKeyword(KeywordType.RETURN)) {
            statement = parseReturn();
        } else if (next.isKeyword(KeywordType.IF)) {
            statement = parseIfStatement();
        } else if (next.isKeyword(KeywordType.FOR)) {
            statement = parseForLoopTree();
        } else if (next.isKeyword(KeywordType.WHILE)) {
            statement = parseWhileLoop();
        } else {
            statement = parseSimple();
        }
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        return statement;
    }

    private IfStatementTree parseIfStatement() {
        Keyword _if = this.tokenSource.expectKeyword(KeywordType.IF);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree expr = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        StatementTree if_statement = parseStatement();
        StatementTree else_statement = null;
        if(this.tokenSource.peek().isKeyword(KeywordType.ELSE)) {
            this.tokenSource.expectKeyword(KeywordType.ELSE);
            else_statement = parseStatement();
        }
        return new IfStatementTree(expr, if_statement, else_statement, _if.span().start());
    }

    private WhileLoopTree parseWhileLoop() {
        Keyword _while = this.tokenSource.expectKeyword(KeywordType.WHILE);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree expr = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        StatementTree statement = parseStatement();
        return new WhileLoopTree(expr, statement, _while.span().start());
    }

    private ForLoopTree parseForLoopTree() {
        Keyword _for = this.tokenSource.expectKeyword(KeywordType.FOR);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        SimpleTree init = null, advancement = null;
        if(!this.tokenSource.peek().isSeparator(SeparatorType.SEMICOLON)) {
            init = parseSimple();
        }
        ExpressionTree expr = parseExpression();
        if(!this.tokenSource.peek().isSeparator(SeparatorType.PAREN_CLOSE)) {
            advancement = parseSimple();
        }
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        StatementTree statement = parseStatement();
        return new ForLoopTree(init, expr, advancement, statement, _for.span().start());
    }

    private SimpleTree parseSimple() {
        Token next = this.tokenSource.peek();
        if(next.isKeyword(KeywordType.INT)) {
            DeclarationTree decl = parseDeclaration(KeywordType.INT);
            return new SimpleTree(null, decl);
        } else if(next.isKeyword(KeywordType.BOOL)) {
            DeclarationTree decl = parseDeclaration(KeywordType.BOOL);
            return new SimpleTree(null, decl);
        } else {
            LValueTree lValue = parseLValue();
            Operator assignmentOperator = parseAssignmentOperator();
            ExpressionTree expression = parseExpression();
            return new SimpleTree(new AssignmentTree(lValue, assignmentOperator, expression), null);
        }
    }

    private DeclarationTree parseDeclaration(KeywordType type) {
        Keyword keyword = this.tokenSource.expectKeyword(type);
        BasicType basicType = (type == KeywordType.INT ? BasicType.INT : BasicType.BOOL);
        Identifier ident = this.tokenSource.expectIdentifier();
        ExpressionTree expr = null;
        if (this.tokenSource.peek().isOperator(OperatorType.ASSIGN)) {
            this.tokenSource.expectOperator(OperatorType.ASSIGN);
            expr = parseExpression();
        }
        return new DeclarationTree(new TypeTree(basicType, keyword.span()), name(ident), expr);
    }

    private Operator parseAssignmentOperator() {
        if (this.tokenSource.peek() instanceof Operator op) {
            return switch (op.type()) {
                case ASSIGN, ASSIGN_DIV, ASSIGN_MINUS, ASSIGN_MOD, ASSIGN_MUL, ASSIGN_PLUS -> {
                    this.tokenSource.consume();
                    yield op;
                }
                default -> throw new ParseException("expected assignment but got " + op.type());
            };
        }
        throw new ParseException("expected assignment but got " + this.tokenSource.peek());
    }

    private LValueTree parseLValue() {
        if (this.tokenSource.peek().isSeparator(SeparatorType.PAREN_OPEN)) {
            this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
            LValueTree inner = parseLValue();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
            return inner;
        }
        Identifier identifier = this.tokenSource.expectIdentifier();
        return new LValueIdentTree(name(identifier));
    }

    private StatementTree parseReturn() {
        Keyword ret = this.tokenSource.expectKeyword(KeywordType.RETURN);
        ExpressionTree expression = parseExpression();
        return new ReturnTree(expression, ret.span().start());
    }

    private ExpressionTree parseExpression() {
        ExpressionTree lhs = parseTerm();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _)
                && (type == OperatorType.PLUS || type == OperatorType.MINUS)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseTerm(), type);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseTerm() {
        ExpressionTree lhs = parseFactor();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _)
                && (type == OperatorType.MUL || type == OperatorType.DIV || type == OperatorType.MOD)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseFactor(), type);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseFactor() {
        return switch (this.tokenSource.peek()) {
            case Separator(var type, _) when type == SeparatorType.PAREN_OPEN -> {
                this.tokenSource.consume();
                ExpressionTree expression = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                yield expression;
            }
            case Operator(var type, _) when type == OperatorType.MINUS -> {
                Span span = this.tokenSource.consume().span();
                yield new NegateTree(parseFactor(), span);
            }
            case Identifier ident -> {
                this.tokenSource.consume();
                yield new IdentExpressionTree(name(ident));
            }
            case NumberLiteral(String value, int base, Span span) -> {
                this.tokenSource.consume();
                yield new LiteralTree(value, base, span);
            }
            case Token t -> throw new ParseException("invalid factor " + t);
        };
    }

    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }
}
