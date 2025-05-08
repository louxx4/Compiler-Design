package edu.kit.kastel.vads.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.backend.aasm.CodeGenerator;
import edu.kit.kastel.vads.compiler.backend.instrsel.Instruction;
import edu.kit.kastel.vads.compiler.backend.instrsel.InstructionSelector;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.SsaTranslation;
import edu.kit.kastel.vads.compiler.ir.optimize.LocalValueNumbering;
import edu.kit.kastel.vads.compiler.lexer.Lexer;
import edu.kit.kastel.vads.compiler.parser.ParseException;
import edu.kit.kastel.vads.compiler.parser.Parser;
import edu.kit.kastel.vads.compiler.parser.TokenSource;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.semantic.SemanticAnalysis;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Invalid arguments: Expected one input file and two output files");
            System.exit(3);
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Path asmOutput = Path.of(args[2]);
        ProgramTree program = lexAndParse(input);
        try {
            new SemanticAnalysis(program).analyze();
        } catch (SemanticException e) {
            e.printStackTrace();
            System.exit(7);
            return;
        }
        // using ssa as intermediate representation
        List<IrGraph> graphs = new ArrayList<>();
        for (FunctionTree function : program.topLevelTrees()) {
            SsaTranslation translation = new SsaTranslation(function, new LocalValueNumbering());
            graphs.add(translation.translate());
        }

        // TODO: generate assembly and invoke gcc instead of generating abstract assembly
    
        InstructionSelector is = new InstructionSelector();
        List<Instruction> instructions = is.performIS(graphs);
        StringBuilder sb = new StringBuilder();
        for(Instruction i : instructions) {
            sb.append(i.print()).append("\n");
        }
        Files.writeString(asmOutput, sb.toString());

        String s = new CodeGenerator().generateCode(graphs);
        Files.writeString(output, s);
    }

    private static ProgramTree lexAndParse(Path input) throws IOException {
        try {
            Lexer lexer = Lexer.forString(Files.readString(input));
            TokenSource tokenSource = new TokenSource(lexer);
            Parser parser = new Parser(tokenSource);
            return parser.parseProgram();
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(42);
            throw new AssertionError("unreachable");
        }
    }
}