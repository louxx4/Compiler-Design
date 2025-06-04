package edu.kit.kastel.vads.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.vads.compiler.backend.instrsel.Instruction;
import edu.kit.kastel.vads.compiler.backend.instrsel.InstructionSelector;
import edu.kit.kastel.vads.compiler.backend.liveness.InterferenceGraph;
import edu.kit.kastel.vads.compiler.backend.liveness.LivenessAnalyzer;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.SsaTranslation;
import edu.kit.kastel.vads.compiler.ir.optimize.LocalValueNumbering;
import edu.kit.kastel.vads.compiler.ir.util.GraphVizPrinter;
import edu.kit.kastel.vads.compiler.ir.util.YCompPrinter;
import edu.kit.kastel.vads.compiler.lexer.Lexer;
import edu.kit.kastel.vads.compiler.parser.ParseException;
import edu.kit.kastel.vads.compiler.parser.Parser;
import edu.kit.kastel.vads.compiler.parser.TokenSource;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.semantic.SemanticAnalysis;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

//Build with:                   sh build.sh
//Run with:                     sh run.sh foo.c foo
//Generate executable with:     gcc asmfoo.s -o asmexe
//Execute with:                 ./asmexe
public class Main {
    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Invalid arguments: Expected one input file and one output file");
            System.exit(3);
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Path debugOutput = Path.of("debugfoo");
        Path asmOutput = Path.of("asmfoo.s");
        Path graphOutput = Path.of("debuggraph");
        ProgramTree program = lexAndParse(input);
        try {
            new SemanticAnalysis(program).analyze();
        } catch (SemanticException e) {
            e.printStackTrace();
            System.exit(7);
            return;
        }
        List<IrGraph> graphs = new ArrayList<>();
        for (FunctionTree function : program.topLevelTrees()) {
            SsaTranslation translation = new SsaTranslation(function, new LocalValueNumbering());
            graphs.add(translation.translate());
        }

        for (IrGraph graph : graphs) {
            Files.writeString(graphOutput, GraphVizPrinter.print(graph));
        }

        if ("vcg".equals(System.getenv("DUMP_GRAPHS")) || "vcg".equals(System.getProperty("dumpGraphs"))) {
            Path tmp = output.toAbsolutePath().resolveSibling("graphs");
            Files.createDirectory(tmp);
            for (IrGraph graph : graphs) {
                dumpGraph(graph, tmp, "before-codegen");
            }
        }

        // generate assembly
        InstructionSelector is = new InstructionSelector();
        List<Instruction> instructions = is.performIS(graphs);
        LivenessAnalyzer.performLA(instructions.toArray(Instruction[]::new));
        InterferenceGraph interferenceGraph = LivenessAnalyzer.generateInterferenceGraph(instructions, is.ALL_TREGS);
        int spilledRegs = RegisterAllocator.performRegisterAllocation(interferenceGraph);
        is.addFunctionPrologue(instructions, spilledRegs);
        is.addFunctionEpilogue(instructions, spilledRegs);

        StringBuilder sbDebug = new StringBuilder(), sb = new StringBuilder();
        sb.append(InstructionSelector.getGlobalPrologue());

        for(Instruction i : instructions) {
            sbDebug.append(i.print(true)).append("\n"); // debug output
            sb.append(i.print(false)).append("\n"); // real output
        }

        Files.writeString(debugOutput, sbDebug.toString());
        Files.writeString(asmOutput, sb.toString());

        // invoke scc
        Runtime.getRuntime().exec("gcc asmfoo.s -o " + output);

        // template (abstract assembly) output
        //String s = new CodeGenerator().generateCode(graphs);
        //Files.writeString(output, s);
    }

    @SuppressWarnings("CallToPrintStackTrace")
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

    private static void dumpGraph(IrGraph graph, Path path, String key) throws IOException {
        Files.writeString(
            path.resolve(graph.name() + "-" + key + ".vcg"),
            YCompPrinter.print(graph)
        );
    }
}
