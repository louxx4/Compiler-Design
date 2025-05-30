package edu.kit.kastel.vads.compiler.parser;

import java.util.List;

import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.ConditionalTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForLoopTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfStatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.JumpTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.LogicalOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateBWTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.NotTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileLoopTree;

/// This is a utility class to help with debugging the parser.
public class Printer {

    private final Tree ast;
    private final StringBuilder builder = new StringBuilder();
    private boolean requiresIndent;
    private int indentDepth;

    public Printer(Tree ast) {
        this.ast = ast;
    }

    public static String print(Tree ast) {
        Printer printer = new Printer(ast);
        printer.printRoot();
        return printer.builder.toString();
    }

    private void printRoot() {
        printTree(this.ast);
    }

    private void printTree(Tree tree) {
        switch (tree) {
            case BlockTree(List<StatementTree> statements, _, _) -> {
                print("{");
                lineBreak();
                this.indentDepth++;
                for (StatementTree statement : statements) {
                    printTree(statement);
                }
                this.indentDepth--;
                print("}");
            }
            case FunctionTree(var returnType, var name, var body) -> {
                printTree(returnType);
                space();
                printTree(name);
                print("()");
                space();
                printTree(body);
            }
            case NameTree(var name, _) -> print(name.asString());
            case ProgramTree(var topLevelTrees, _) -> {
                for (FunctionTree function : topLevelTrees) {
                    printTree(function);
                    lineBreak();
                }
            }
            case TypeTree(var type, _) -> print(type.asString());
            case BinaryOperationTree(var lhs, var rhs, var op) -> {
                print("(");
                printTree(lhs);
                print(")");
                space();
                this.builder.append(op);
                space();
                print("(");
                printTree(rhs);
                print(")");
            }
            case LiteralTree(var value, _, _) -> this.builder.append(value);
            case NegateTree(var expression, _) -> {
                print("-(");
                printTree(expression);
                print(")");
            }
            case AssignmentTree(var lValue, var op, var expression, _) -> {
                printTree(lValue);
                space();
                this.builder.append(op);
                space();
                printTree(expression);
                semicolon();
            }
            case DeclarationTree(var type, var name, var initializer, _) -> {
                printTree(type);
                space();
                printTree(name);
                if (initializer != null) {
                    print(" = ");
                    printTree(initializer);
                }
                semicolon();
            }
            case ReturnTree(var expr, _, _) -> {
                print("return ");
                printTree(expr);
                semicolon();
            }
            case LValueIdentTree(var name) -> printTree(name);
            case IdentExpressionTree(var name, _) -> printTree(name);
            case BooleanTree(var bool, _) -> print(String.valueOf(bool));
            case ConditionalTree(var lhs, var expr1, var expr2) -> {
                printTree(lhs);
                print(" ? ");
                printTree(expr1);
                print(" : ");
                printTree(expr2);
                semicolon();
            }
            case ForLoopTree(var init, var cond, var step, var body, _, _) -> {
                print("for(");
                printTree(init);
                printTree(cond);
                printTree(step);
                print(") {");
                lineBreak();
                this.indentDepth++;
                printTree(body);
                this.indentDepth--;
                print("}\n");
            }
            case WhileLoopTree(var cond, var body, _, _) -> {
                print("while(");
                printTree(cond);
                print(") {");
                lineBreak();
                this.indentDepth++;
                printTree(body);
                this.indentDepth--;
                print("}\n");
            }
            case NotTree(var expr, _) -> {
                print("!(");
                printTree(expr);
                print(")");
            }
            case NegateBWTree(var expr, _) -> {
                print("~(");
                printTree(expr);
                print(")");
            }
            case IfStatementTree(var expr, var if_body, var else_body,_ , _, _) -> {
                print("if(");
                printTree(expr);
                print(") {");
                lineBreak();
                this.indentDepth++;
                printTree(if_body);
                print("}");
                lineBreak();
                this.indentDepth--;
                print("else {");
                lineBreak();
                this.indentDepth++;
                printTree(else_body);
                print("}");
                lineBreak();
                this.indentDepth--;
            }
            case LogicalOperationTree(var lhs, var rhs, var op) -> {
                print("(");
                printTree(lhs);
                print(")");
                space();
                this.builder.append(op);
                space();
                print("(");
                printTree(rhs);
                print(")");
            }
            case JumpTree(var type, _, _, _) -> {
                print(type.asString());
                semicolon();
            }
        }
    }

    private void print(String str) {
        if (this.requiresIndent) {
            this.requiresIndent = false;
            this.builder.append(" ".repeat(4 * this.indentDepth));
        }
        this.builder.append(str);
    }

    private void lineBreak() {
        this.builder.append("\n");
        this.requiresIndent = true;
    }

    private void semicolon() {
        this.builder.append(";");
        lineBreak();
    }

    private void space() {
        this.builder.append(" ");
    }

}
