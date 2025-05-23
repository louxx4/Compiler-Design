package edu.kit.kastel.vads.compiler.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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
import edu.kit.kastel.vads.compiler.parser.ast.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.ConditionalTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForLoopTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfStatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.JumpTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.LogicalOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateBWTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.NotTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.SimpleTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileLoopTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.type.JumpType;

public class Parser {
    private final TokenSource tokenSource;
    private final Stack<Scope> currentScope = new Stack(); //manages the current scope
    private final List<Scope> scopes = new ArrayList<>(); //will hold tree of block/function scopes

    public Parser(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }

    public ProgramTree parseProgram() {
        List functions = List.of(parseFunction());
        ProgramTree programTree = new ProgramTree(functions, this.scopes);
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
        //adjust scope
        enterNewScope();
        //parse block
        Separator bodyOpen = this.tokenSource.expectSeparator(SeparatorType.BRACE_OPEN);
        List<StatementTree> statements = new ArrayList<>();
        while (!(this.tokenSource.peek() instanceof Separator sep && sep.type() == SeparatorType.BRACE_CLOSE)) {
            statements.add(parseStatement());
        }
        Separator bodyClose = this.tokenSource.expectSeparator(SeparatorType.BRACE_CLOSE);
        //readjust scope
        leaveScope();
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
        } else if (next.isKeyword(KeywordType.CONTINUE)) {
            statement = parseContinue();
        } else if (next.isKeyword(KeywordType.BREAK)) {
            statement = parseBreak();
        } else if (next.isSeparator(SeparatorType.BRACE_OPEN)) {
            statement = parseBlock();
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
            return parseDeclaration(KeywordType.INT);
        } else if(next.isKeyword(KeywordType.BOOL)) {
            return parseDeclaration(KeywordType.BOOL);
        } else {
            LValueTree lValue = parseLValue();
            Operator assignmentOperator = parseAssignmentOperator();
            ExpressionTree expression = parseExpression();
            return new AssignmentTree(lValue, assignmentOperator, 
                expression, getScopeId());
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
        return new DeclarationTree(new TypeTree(basicType, keyword.span()), name(ident), expr, getScopeId());
    }

    private Operator parseAssignmentOperator() {
        if (this.tokenSource.peek() instanceof Operator op) {
            return switch (op.type()) {
                case ASSIGN, ASSIGN_DIV, ASSIGN_MINUS, ASSIGN_MOD, ASSIGN_MUL, ASSIGN_PLUS, 
                        ASSIGN_AND, ASSIGN_XOR, ASSIGN_OR, ASSIGN_SHL, ASSIGN_SHR -> {
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

    private StatementTree parseContinue() {
        Keyword keyword = this.tokenSource.expectKeyword(KeywordType.CONTINUE);
        return new JumpTree(JumpType.CONTINUE, keyword.span());
    }

    private StatementTree parseBreak() {
        Keyword keyword = this.tokenSource.expectKeyword(KeywordType.BREAK);
        return new JumpTree(JumpType.BREAK, keyword.span());
    }

    //Conditional expression (or higher precedence)
    // a ? b : c ? d : e  IS  a ? b : (c ? d : e)  and NOT  (a ? b : c) ? d : e
    private ExpressionTree parseExpression() {
        ExpressionTree lhs = parseTermPrecedenceLevel1();
        if (this.tokenSource.peek() instanceof Operator(var type, _) && type == OperatorType.QUESTIONMARK) {
            this.tokenSource.consume();
            ExpressionTree if_expr = parseExpression();
            this.tokenSource.expectSeparator(SeparatorType.COLON);
            ExpressionTree else_expr = parseExpression();
            return new ConditionalTree(lhs, if_expr, else_expr);
        } else {
            return lhs;  
        }
    }

    //Logical or (or higher precedence)
    private ExpressionTree parseTermPrecedenceLevel1() {
        ExpressionTree lhs = parseTermPrecedenceLevel2();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _) && type == OperatorType.OR) {
                Span span = this.tokenSource.consume().span();
                lhs = new ConditionalTree(lhs, new BooleanTree(true, span), parseTermPrecedenceLevel2());
            } else {
                return lhs;
            }
        }
    }

    //Logical and (or higher precedence)
    private ExpressionTree parseTermPrecedenceLevel2() {
        ExpressionTree lhs = parseTermPrecedenceLevel3();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _) && type == OperatorType.AND) {
                Span span = this.tokenSource.consume().span();
                lhs = new ConditionalTree(lhs, parseTermPrecedenceLevel3(), new BooleanTree(false, span));
            } else {
                return lhs;
            }
        }
    }

    //Bitwise or (or higher precedence)
    private ExpressionTree parseTermPrecedenceLevel3() {
        ExpressionTree lhs = parseTermPrecedenceLevel4();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _) && type == OperatorType.OR_BW) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseTermPrecedenceLevel4(), type);
            } else {
                return lhs;
            }
        }
    }

    //Bitwise xor (or higher precedence)
    private ExpressionTree parseTermPrecedenceLevel4() {
        ExpressionTree lhs = parseTermPrecedenceLevel5();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _) && type == OperatorType.XOR_BW) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseTermPrecedenceLevel5(), type);
            } else {
                return lhs;
            }
        }
    }

    //Bitwise and (or higher precedence)
    private ExpressionTree parseTermPrecedenceLevel5() {
        ExpressionTree lhs = parseTermPrecedenceLevel6();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _) && type == OperatorType.AND_BW) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseTermPrecedenceLevel6(), type);
            } else {
                return lhs;
            }
        }
    }

    //Overloaded equality, disequality (or higher precedence)
    private ExpressionTree parseTermPrecedenceLevel6() {
        ExpressionTree lhs = parseTermPrecedenceLevel7();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _) && 
                    (type == OperatorType.EQ || type == OperatorType.NEQ)) {
                this.tokenSource.consume();
                lhs = new LogicalOperationTree(lhs, parseTermPrecedenceLevel7(), type);
            } else {
                return lhs;
            }
        }
    }

    //Integer comparison (or higher precedence)
    private ExpressionTree parseTermPrecedenceLevel7() {
        ExpressionTree lhs = parseTermPrecedenceLevel8();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _) && 
                    (type == OperatorType.LESS || type == OperatorType.GREATER ||
                     type == OperatorType.LEQ || type == OperatorType.GEQ)) {
                this.tokenSource.consume();
                lhs = new LogicalOperationTree(lhs, parseTermPrecedenceLevel8(), type);
            } else {
                return lhs;
            }
        }
    }

    //(Arithmetic) shift left, right (or higher precedence)
    private ExpressionTree parseTermPrecedenceLevel8() {
        ExpressionTree lhs = parseTermPrecedenceLevel9();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _) && 
                    (type == OperatorType.SHL || type == OperatorType.SHR)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseTermPrecedenceLevel9(), type);
            } else {
                return lhs;
            }
        }
    }

    //Integer plus, minus (or higher precedence)
    private ExpressionTree parseTermPrecedenceLevel9() {
        ExpressionTree lhs = parseTermPrecedenceLevel10();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _) && 
                    (type == OperatorType.PLUS || type == OperatorType.MINUS)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseTermPrecedenceLevel10(), type);
            } else {
                return lhs;
            }
        }
    }

    //Integer times, divide, modulo (or higher precedence)
    private ExpressionTree parseTermPrecedenceLevel10() {
        ExpressionTree lhs = parseFactor();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _) && 
                    (type == OperatorType.MUL || type == OperatorType.DIV || type == OperatorType.MOD)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseFactor(), type);
            } else {
                return lhs;
            }
        }
    }

    //Logical not, bitwise not, unary minus, parentheses, true, false
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
            case Operator(var type, _) when type == OperatorType.NOT_BW -> {
                Span span = this.tokenSource.consume().span();
                yield new NegateBWTree(parseFactor(), span);
            }
            case Operator(var type, _) when type == OperatorType.NOT -> {
                Span span = this.tokenSource.consume().span();
                yield new NotTree(parseFactor(), span);
            }
            case Identifier ident -> {
                this.tokenSource.consume();
                yield new IdentExpressionTree(name(ident), getScopeId());
            }
            case NumberLiteral(String value, int base, Span span) -> {
                this.tokenSource.consume();
                yield new LiteralTree(value, base, span);
            }
            case Keyword(var type, _) when type == KeywordType.TRUE -> {
                Span span = this.tokenSource.consume().span();
                yield new BooleanTree(true, span);
            }
            case Keyword(var type, _) when type == KeywordType.FALSE -> {
                Span span = this.tokenSource.consume().span();
                yield new BooleanTree(false, span);
            }
            case Token t -> throw new ParseException("invalid factor " + t);
        };
    }

    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }

    private void enterNewScope() {
        Scope scope;
        if(this.currentScope.empty()) scope = new Scope(this.scopes.size());
        else scope = new Scope(this.scopes.size(), this.currentScope.peek());
        this.scopes.add(scope);
        this.currentScope.push(scope);
    }

    private void leaveScope() {
        this.currentScope.pop();
    }

    private int getScopeId() {
        return this.currentScope.peek().getId();
    }
}
