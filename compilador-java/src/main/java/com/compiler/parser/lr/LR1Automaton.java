package com.compiler.parser.lr;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;

import com.compiler.parser.grammar.Grammar;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.grammar.SymbolType;
import com.compiler.parser.grammar.Production;

/**
 * Builds the canonical collection of LR(1) items (the DFA automaton).
 * Items contain a lookahead symbol.
 */
public class LR1Automaton {
    private final Grammar grammar;
    private final List<Set<LR1Item>> states = new ArrayList<>();
    private final Map<Integer, Map<Symbol, Integer>> transitions = new HashMap<>();
    private String augmentedLeftName = null;

    public LR1Automaton(Grammar grammar) {
        this.grammar = Objects.requireNonNull(grammar);
    }

    public List<Set<LR1Item>> getStates() { return states; }
    public Map<Integer, Map<Symbol, Integer>> getTransitions() { return transitions; }

    Symbol epsilon = new Symbol("ε", SymbolType.TERMINAL);
    Map<Symbol, Set<Symbol>> firstSets = new HashMap<>();

    /**
     * CLOSURE for LR(1): standard algorithm using FIRST sets to compute lookaheads for new items.
     */
    private Set<LR1Item> closure(Set<LR1Item> items) {
        // TODO: Implement the CLOSURE algorithm for a set of LR(1) items.
        //  Initialize a new set `closure` with the given `items`.
        Set<LR1Item> closure = new HashSet<>(items);
        // Create a worklist (like a Queue or List) and add all items from `items` to it.
        Queue<LR1Item> worklist = new LinkedList<>(items);
        
        // While the worklist is not empty:
        while (!worklist.isEmpty()) {
            LR1Item item = worklist.poll();
            Symbol B = item.getSymbolAfterDot();
            
            // Only process if B is a non-terminal
            if (B != null && B.type == SymbolType.NON_TERMINAL) {
                // Get the rest of the production after B (beta)
                List<Symbol> beta = new ArrayList<>();
                for (int i = item.dotPosition + 1; i < item.production.right.size(); i++) {
                    beta.add(item.production.right.get(i));
                }
                
                // Add the lookahead to beta
                beta.add(item.lookahead);
                
                // Compute FIRST(beta + lookahead)
                Set<Symbol> firstBeta = computeFirstOfSequence(beta, firstSets, epsilon);
                
                // For each production B -> gamma
                for (Production prod : grammar.getProductions()) {
                    if (prod.left.equals(B)) {
                        // For each terminal in FIRST(beta + lookahead)
                        for (Symbol terminal : firstBeta) {
                            if (!terminal.equals(epsilon)) {
                                LR1Item newItem = new LR1Item(prod, 0, terminal);
                                if (!closure.contains(newItem)) {
                                    closure.add(newItem);
                                    worklist.add(newItem);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Return the `closure` set.
        return closure;
    }

    /**
     * Compute FIRST of a sequence of symbols.
     */
    private Set<Symbol> computeFirstOfSequence(List<Symbol> seq, Map<Symbol, Set<Symbol>> firstSets, Symbol epsilon) {
        // TODO: Implement the logic to compute the FIRST set for a sequence of symbols.
        // Initialize an empty result set.
        Set<Symbol> result = new HashSet<>();
        // If the sequence is empty, add epsilon to the result and return.
        if (seq == null || seq.isEmpty()) {
            result.add(epsilon);
            return result;
        }
        
        boolean todasConEpsilon = true;
        
        // Iterate through the symbols `X` in the sequence:
        for (int i = 0; i < seq.size(); i++) {
            // Get `FIRST(X)` from the pre-calculated `firstSets`.
            Symbol X = seq.get(i);
            Set<Symbol> firstX = firstSets.get(X);
            
            if (firstX == null) {
                todasConEpsilon = false;
                break;
            }
            
            // Add all non-epsilon symbols from FIRST(X)
            for (Symbol s : firstX) {
                if (!s.equals(epsilon)) {
                    result.add(s);
                }
            }
            
            // If X doesn't have epsilon in its FIRST set, stop
            if (!firstX.contains(epsilon)) {
                todasConEpsilon = false;
                break;
            }
            
            // If this is the last symbol and all previous had epsilon
            if (i == seq.size() - 1 && todasConEpsilon) {
                result.add(epsilon);
            }
        }
        // Return the result set.
        return result;
    }


    /**
     * GOTO for LR(1): moves dot over symbol and takes closure.
     */
    private Set<LR1Item> goTo(Set<LR1Item> state, Symbol symbol) {
        // TODO: Implement the GOTO function.
        // Initialize an empty set `movedItems`.
        Set<LR1Item> movedItems = new HashSet<>();
    
        // For each item `[A -> α • X β, a]` in the input `state`:
        for (LR1Item item : state) {
            Symbol afterDot = item.getSymbolAfterDot();
            //If `X` is equal to the input `symbol`:
            if (afterDot != null && afterDot.equals(symbol)) {
                // Move the dot one position to the right
                LR1Item newItem = new LR1Item(item.production, item.dotPosition + 1, item.lookahead);
                movedItems.add(newItem);
            }
        }
        
        if (movedItems.isEmpty()) {
            return new HashSet<>();
        }
        // Return the `closure` of `movedItems`.
        return closure(movedItems);
    }

    /**
     * Build the LR(1) canonical collection: states and transitions.
     */
    public void build() {
        // TODO: Implement the construction of the canonical collection of LR(1) item sets (the DFA).
        // Clear any existing states and transitions.
        states.clear();
        transitions.clear();
        
        // Calculate FIRST sets
        calcFirstS();
        
        // Create the augmented grammar: Add a new start symbol S' and production S' -> S.
        Symbol ogStart = grammar.getStartSymbol();
        augmentedLeftName = ogStart.name + "'";
        Symbol augmentedStart = new Symbol(augmentedLeftName, SymbolType.NON_TERMINAL);
        
        List<Symbol> augmentedRight = new ArrayList<>();
        augmentedRight.add(ogStart);
        Production augmentedProduction = new Production(augmentedStart, augmentedRight);
        
        // Create initial item: [S' -> • S, $]
        LR1Item initialItem = new LR1Item(augmentedProduction, 0, new Symbol("$", SymbolType.TERMINAL));
        Set<LR1Item> initialItemSet = new HashSet<>();
        initialItemSet.add(initialItem);
        
        // State I0: closure of initial item
        Set<LR1Item> state0 = closure(initialItemSet);
        states.add(state0);
        
        // Worklist
        Queue<Integer> worklist = new LinkedList<>();
        worklist.add(0);
        
        // Map to track which states we have already seen
        Map<Set<LR1Item>, Integer> mapaEstados = new HashMap<>();
        mapaEstados.put(state0, 0);
        
        // Collect all grammar symbols
        Set<Symbol> allSymbols = new HashSet<>();
        allSymbols.addAll(grammar.getTerminals());
        allSymbols.addAll(grammar.getNonTerminals());
        
        // While the worklist is not empty:
        while (!worklist.isEmpty()) {
            int indiceEstado = worklist.poll();
            Set<LR1Item> currentState = states.get(indiceEstado);
            
            // Try transitions on all symbols
            for (Symbol symbol : allSymbols) {
                Set<LR1Item> sigEstado = goTo(currentState, symbol);
                
                if (!sigEstado.isEmpty()) {
                    Integer indiceEstadoSig = mapaEstados.get(sigEstado);
                    
                    if (indiceEstadoSig == null) {
                        // New state
                        indiceEstadoSig = states.size();
                        states.add(sigEstado);
                        mapaEstados.put(sigEstado, indiceEstadoSig);
                        worklist.add(indiceEstadoSig);
                    }
                    
                    // Add transition
                    transitions.computeIfAbsent(indiceEstado, k -> new HashMap<>()).put(symbol, indiceEstadoSig);
                }
            }
        }
    }

    /**
     * Calculates FIRST sets for all symbols in the grammar
     */
    private void calcFirstS() {

        Symbol simboloFinal = new Symbol("$", SymbolType.TERMINAL);
        
        // Initialize FIRST sets
        for (Symbol terminal : grammar.getTerminals()) {
            Set<Symbol> first = new HashSet<>();
            first.add(terminal);
            firstSets.put(terminal, first);
        }
        
        for (Symbol nonTerminal : grammar.getNonTerminals()) {
            firstSets.put(nonTerminal, new HashSet<>());
        }
        
        // Add dollar sign
        if (simboloFinal  != null) {
            Set<Symbol> endFirst = new HashSet<>();
            endFirst.add(simboloFinal);
            firstSets.put(simboloFinal , endFirst);
        }
        
        // Iteratively compute FIRST sets
        boolean cambio;

        do {
            cambio = false;
            for (Production p : grammar.getProductions()) {
                Symbol lhs = p.left;
                Set<Symbol> firstLhs = firstSets.get(lhs);
                int tamanoIni = firstLhs.size();
                
                if (p.right.isEmpty() || (p.right.size() == 1 && p.right.get(0).name.equals("ε"))) {
                    // Production is A -> ε
                    firstLhs.add(epsilon);
                } else {
                    // Add FIRST of the RHS
                    boolean todasConEpsilon = true;
                    for (Symbol s : p.right) {
                        Set<Symbol> firstSym = firstSets.get(s);
                        if (firstSym != null) {
                            for (Symbol simbolo : firstSym) {
                                if (!s.equals(epsilon)) {
                                    firstLhs.add(simbolo);
                                }
                            }
                            if (!firstSym.contains(epsilon)) {
                                todasConEpsilon = false;
                                break;
                            }
                        } else {
                            todasConEpsilon = false;
                            break;
                        }
                    }
                    if (todasConEpsilon) {
                        firstLhs.add(epsilon);
                    }
                }
                
                if (firstLhs.size() > tamanoIni) {
                    cambio = true;
                }
            }
        } while (cambio);
    }


    public String getAugmentedLeftName() { return augmentedLeftName; }
}