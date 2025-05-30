package edu.kit.kastel.vads.compiler.semantic;

import java.util.HashMap;
import java.util.Map;

import edu.kit.kastel.vads.compiler.parser.ast.Tree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;

public class TypeContext {

    private final Map<Tree,TypeAnalysis.TypeStatus> global_context = new HashMap(); //maps trees (except NameTree) to types (is not scope sensitive)
    private final Namespace<TypeAnalysis.TypeStatus>[] scope_context; //maps name to type (is scope sensitive)

    public TypeContext(Namespace<TypeAnalysis.TypeStatus>[] namespaces) {
        this.scope_context = namespaces; 
    }

    public TypeAnalysis.TypeStatus get(Tree tree) {
        return this.global_context.get(tree);
    }

    public TypeAnalysis.TypeStatus get(Name name, int scope) {
        return this.scope_context[scope].get(name);
    }

    public void put(Tree tree, TypeAnalysis.TypeStatus type) {
        this.global_context.put(tree, type);
    }

    public void put(Name name, TypeAnalysis.TypeStatus type, int scope) {
        this.scope_context[scope].put(name, type, (existing, replacement) -> {
            return replacement;
        });
    }
    
}