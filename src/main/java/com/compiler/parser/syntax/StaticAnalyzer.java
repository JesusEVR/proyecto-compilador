package com.compiler.parser.syntax;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Production;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.grammar.SymbolType;

/**
 * Calculates the FIRST and FOLLOW sets for a given grammar.
 * Main task of Practice 5.
 */
public class StaticAnalyzer {
    private final Grammar grammar;
    private final Map<Symbol, Set<Symbol>> firstSets;
    private final Map<Symbol, Set<Symbol>> followSets;
    
    // Epsilon symbol
    private static  Symbol epsilon = new Symbol("Îµ", SymbolType.TERMINAL); 
    

    public StaticAnalyzer(Grammar grammar) {
        this.grammar = grammar;
        this.firstSets = new HashMap<>();
        this.followSets = new HashMap<>();
    }

    /**
     * Calculates and returns the FIRST sets for all symbols.
     * @return A map from Symbol to its FIRST set.
     */
    public Map<Symbol, Set<Symbol>> getFirstSets() {
        // Clear previous FIRST sets
        firstSets.clear();
        
        // 1. Initialize FIRST sets
        // For terminals: FIRST(terminal) = {terminal}
        for (Symbol t : grammar.getTerminals()) {
            firstSets.put(t, new HashSet<>());
            firstSets.get(t).add(t);
        }
        
        // For non-terminals: FIRST(non_terminal) = {} (empty set)
        for (Symbol noT : grammar.getNonTerminals()) {
            firstSets.put(noT, new HashSet<>());
        }
        
        // 2. Repeat until no changes
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            
            // For each production A -> X1 X2 ... Xn
            for (Production p : grammar.getProductions()) {
                Symbol A = p.getLeft();
                Set<Symbol> firstA = firstSets.get(A);
                int tamanoini = firstA.size();
                
                if (p.getRight().isEmpty()) {
                    // Empty production A -> ε
                    firstA.add(epsilon);
                } else {
                    // For each symbol Xi on the right-hand side
                    boolean allContainEpsilon = true;
                    
                    for (Symbol Xi : p.getRight()) {
                        Set<Symbol> firstXi = firstSets.get(Xi);
                        
                        // Check that the symbol exists in firstSets
                        if (firstXi == null) {
                            // If the symbol is not in firstSets, initialize it
                            firstXi = new HashSet<>();
                            firstSets.put(Xi, firstXi);
                            
                            // If it's a terminal, add itself
                            if (Xi.type == SymbolType.TERMINAL) {
                                firstXi.add(Xi);
                            }
                        }
                        
                        // a. Add FIRST(Xi) - {ε} to FIRST(A)
                        for (Symbol s : firstXi) {
                            if (!s.equals(epsilon)) {
                                firstA.add(s);
                            }
                        }
                        
                        // b. If ε is not in FIRST(Xi), stop
                        if (!firstXi.contains(epsilon)) {
                            allContainEpsilon = false;
                            break;
                        }
                    }
                    
                    // If ε is in FIRST(Xi) for all i, add ε to FIRST(A)
                    if (allContainEpsilon) {
                        firstA.add(epsilon);
                    }
                }
                
                // Check if there were changes
                if (firstA.size() != tamanoini) {
                    cambio = true;
                }
            }
        }
        
        return new HashMap<>(firstSets);
    }

    /**
     * Calculates and returns the FOLLOW sets for non-terminals.
     * @return A map from Symbol to its FOLLOW set.
     */
    public Map<Symbol, Set<Symbol>> getFollowSets() {
        // First we need to calculate the FIRST sets
        getFirstSets();
        
        // Clear previous FOLLOW sets
        followSets.clear();
        
        // 1. For each non-terminal A, FOLLOW(A) = {}
        for (Symbol nonT : grammar.getNonTerminals()) {
            followSets.put(nonT, new HashSet<>());
        }
        
        // 2. Add $ (end of input) to FOLLOW(S), where S is the start symbol
        followSets.get(grammar.getStartSymbol()).add(new Symbol("$", SymbolType.TERMINAL));
        
        // 3. Repeat until no changes
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            
            // For each production B -> X1 X2 ... Xn
            for (Production p : grammar.getProductions()) {
                Symbol B = p.getLeft();
                
                // For each Xi (where Xi is a non-terminal)
                for (int i = 0; i < p.getRight().size(); i++) {
                    Symbol Xi = p.getRight().get(i);
                    
                    // Only process non-terminals
                    if (Xi.type != SymbolType.NON_TERMINAL) {
                        continue;
                    }
                    
                    Set<Symbol> followXi = followSets.get(Xi);
                    int tamanoIni = followXi.size();
                    
                    // a. For each symbol Xj after Xi (i < j <= n)
                    boolean followEpsilon = true;
                    
                    for (int j = i + 1; j < p.getRight().size(); j++) {
                        Symbol Xj = p.getRight().get(j);
                        Set<Symbol> firstXj = firstSets.get(Xj);
                        
                        // Check that the symbol exists in firstSets
                        if (firstXj == null) {
                            firstXj = new HashSet<>();
                            firstSets.put(Xj, firstXj);
                            if (Xj.type == SymbolType.TERMINAL) {
                                firstXj.add(Xj);
                            }
                        }
                        
                        // Add FIRST(Xj) - {ε} to FOLLOW(Xi)
                        for (Symbol symbol : firstXj) {
                            if (!symbol.equals(epsilon)) {
                                followXi.add(symbol);
                            }
                        }
                        
                        // If ε is not in FIRST(Xj), stop
                        if (!firstXj.contains(epsilon)) {
                            followEpsilon = false;
                            break;
                        }
                    }
                    
                    // b. If ε is in FIRST(Xj) for all j > i, add FOLLOW(B) to FOLLOW(Xi)
                    // Also if Xi is the last symbol in the production
                    if (followEpsilon || i == p.getRight().size() - 1) {
                        followXi.addAll(followSets.get(B));
                    }
                    
                    // Check if there were changes
                    if (followXi.size() != tamanoIni) {
                        cambio = true;
                    }
                }
            }
        }
        
        return new HashMap<>(followSets);
    }
}