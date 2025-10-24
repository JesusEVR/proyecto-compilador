package com.compiler.parser.lr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.compiler.parser.grammar.Production;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.grammar.SymbolType;

/**
 * Builds the LALR(1) parsing table (ACTION/GOTO).
 * Main task for Practice 9.
 */
public class LALR1Table {
    private final LR1Automaton automaton;

    // merged LALR states and transitions
    private List<Set<LR1Item>> lalrStates = new ArrayList<>();
    private Map<Integer, Map<Symbol, Integer>> lalrTransitions = new HashMap<>();
    
    // ACTION table: state -> terminal -> Action
    public static class Action {
        public enum Type { SHIFT, REDUCE, ACCEPT }
        public final Type type;
        public final Integer state; // for SHIFT
        public final Production reduceProd; // for REDUCE

        private Action(Type type, Integer state, Production prod) {
            this.type = type; this.state = state; this.reduceProd = prod;
        }

        public static Action shift(int s) { return new Action(Type.SHIFT, s, null); }
        public static Action reduce(Production p) { return new Action(Type.REDUCE, null, p); }
        public static Action accept() { return new Action(Type.ACCEPT, null, null); }
        
        @Override
        public String toString() {
            switch (type) {
                case SHIFT: return "SHIFT(" + state + ")";
                case REDUCE: return "REDUCE(" + reduceProd.left.name + " -> ...)";
                case ACCEPT: return "ACCEPT";
                default: return "UNKNOWN";
            }
        }
    }

    private final Map<Integer, Map<Symbol, Action>> action = new HashMap<>();
    private final Map<Integer, Map<Symbol, Integer>> gotoTable = new HashMap<>();
    private final List<String> conflicts = new ArrayList<>();
    private int initialState = 0;

    public LALR1Table(LR1Automaton automaton) {
        this.automaton = automaton;
    }

    /**
     * Builds the LALR(1) parsing table.
     */
    public void build() {
        // TODO: Implement the LALR(1) table construction logic.
        // Step 1: Ensure the underlying LR(1) automaton is built.
        if (automaton.getStates().isEmpty()) {
            automaton.build();
        }

        // Step 2: Merge LR(1) states to create LALR(1) states.
        Map<Set<KernelEntry>, List<Integer>> kernelToStates = new HashMap<>();
        List<Set<LR1Item>> lr1States = automaton.getStates();
        
        //  a. Group LR(1) states that have the same "kernel" (the set of LR(0) items).
        for (int i = 0; i < lr1States.size(); i++) {
            Set<LR1Item> state = lr1States.get(i);
            //Create a map from a kernel (Set<KernelEntry>) to a list of state IDs that share that kernel.
            Set<KernelEntry> kernel = extractKernel(state);
            kernelToStates.computeIfAbsent(kernel, k -> new ArrayList<>()).add(i);
        }

        // Create mapping from old LR(1) state IDs to new LALR(1) state IDs
        Map<Integer, Integer> lr1ToLalr = new HashMap<>();
        int lalrStateId = 0;
        
        //  b. For each group of states with the same kernel:
        for (Map.Entry<Set<KernelEntry>, List<Integer>> entry : kernelToStates.entrySet()) {
            List<Integer> stateGroup = entry.getValue();
            
            // Merge all items from states with the same kernel
            Set<LR1Item> mergedState = new HashSet<>();
            for (int stateId : stateGroup) {
                mergedState.addAll(lr1States.get(stateId));
            }
            
            // Consolidate items: merge lookaheads for items with same production and dot position
            Set<LR1Item> consolidatedState = consolidateItems(mergedState);
            lalrStates.add(consolidatedState);
            
            // Map all old state IDs to the new LALR state ID
            for (int oldId : stateGroup) {
                lr1ToLalr.put(oldId, lalrStateId);
            }
            
            lalrStateId++;
        }
        
        // Map the initial state (state 0 in LR(1) should map to LALR initial state)
        initialState = lr1ToLalr.get(0);

        // Step 3: Build the transitions for the new LALR(1) automaton.
        Map<Integer, Map<Symbol, Integer>> lr1Trans = automaton.getTransitions();
        
        for (Map.Entry<Integer, Map<Symbol, Integer>> entry : lr1Trans.entrySet()) {
            int fromLR1 = entry.getKey();
            int fromLALR = lr1ToLalr.get(fromLR1);
            
            for (Map.Entry<Symbol, Integer> trans : entry.getValue().entrySet()) {
                Symbol symbol = trans.getKey();
                int toLR1 = trans.getValue();
                int toLALR = lr1ToLalr.get(toLR1);
                
                // Add transition in LALR automaton
                lalrTransitions.computeIfAbsent(fromLALR, k -> new HashMap<>()).put(symbol, toLALR);
            }
        }

        // Step 4: Fill the ACTION and GOTO tables based on the LALR automaton.
        fillActionGoto();
    }

    /**
     * Populate the ACTION and GOTO tables based on the LALR states and transitions.
     */
    private void fillActionGoto() {
        // TODO: Populate the ACTION and GOTO tables based on the LALR states and transitions.
        // Clear the action, gotoTable, and conflicts lists.
        action.clear();
        gotoTable.clear();
        conflicts.clear();
        
        Symbol simboloFinal = new Symbol("$", SymbolType.TERMINAL);
        String augmentedName = automaton.getAugmentedLeftName();
        
        // Iterate through each LALR state `s` from 0 to lalrStates.size() 
        for (int s = 0; s < lalrStates.size(); s++) {
            Set<LR1Item> state = lalrStates.get(s);

             // For each state `s`, iterate through its LR1Item
            for (LR1Item it : state) {
                // Get the symbol after the dot, `X = it.getSymbolAfterDot()`.
                Symbol X = it.getSymbolAfterDot();
                
                if (X != null) {
                    // Dot is not at the end - SHIFT action for terminals
                    if (X.type == SymbolType.TERMINAL) {
                        Map<Symbol, Integer> transitions = lalrTransitions.get(s);
                        if (transitions != null && transitions.containsKey(X)) {
                            int targetState = transitions.get(X);
                            Action shift = Action.shift(targetState);
                            
                            // Check for conflicts
                            Action existingAction = action.computeIfAbsent(s, k -> new HashMap<>()).get(X);
                            if (existingAction != null) {
                                conflicts.add("State " + s + ": Shift/Reduce conflict on symbol " + X.name);
                            } else {
                                action.get(s).put(X, shift);
                            }
                        }
                    }
                } else {
                    // Dot is at the end - REDUCE or ACCEPT action
                    Symbol lookahead = it.lookahead;
                    
                    // Check if this is the augmented start production
                    if (augmentedName != null && 
                        it.production.left.name.equals(augmentedName) && 
                        lookahead.equals(simboloFinal)) {
                        // ACCEPT action
                        action.computeIfAbsent(s, k -> new HashMap<>()).put(simboloFinal, Action.accept());
                    } else {
                        // REDUCE action
                        Action reduce = Action.reduce(it.production);
                        
                        // Check for conflicts
                        Action existingAction = action.computeIfAbsent(s, k -> new HashMap<>()).get(lookahead);
                        if (existingAction != null) {
                            if (existingAction.type == Action.Type.SHIFT) {
                                conflicts.add("State " + s + ": Shift/Reduce conflict on symbol " + lookahead.name);
                            } else if (existingAction.type == Action.Type.REDUCE) {
                                conflicts.add("State " + s + ": Reduce/Reduce conflict on symbol " + lookahead.name);
                            }
                        } else {
                            action.get(s).put(lookahead, reduce);
                        }
                    }
                }
            }
        }
        
        // Populate GOTO table for non-terminals
        for (int s = 0; s < lalrStates.size(); s++) {
            Map<Symbol, Integer> transitions = lalrTransitions.get(s);
            if (transitions != null) {
                for (Map.Entry<Symbol, Integer> entry : transitions.entrySet()) {
                    Symbol symbol = entry.getKey();
                    if (symbol.type == SymbolType.NON_TERMINAL) {
                        gotoTable.computeIfAbsent(s, k -> new HashMap<>()).put(symbol, entry.getValue());
                    }
                }
            }
        }
    }

    /**
     * Extracts the kernel (LR(0) core) from an LR(1) state.
     */
    private Set<KernelEntry> extractKernel(Set<LR1Item> state) {
        Set<KernelEntry> kernel = new HashSet<>();
        for (LR1Item item : state) {
            kernel.add(new KernelEntry(item.production, item.dotPosition));
        }
        return kernel;
    }

    /**
     * Consolidates items with the same production and dot position by merging their lookaheads.
     */
    private Set<LR1Item> consolidateItems(Set<LR1Item> items) {
        // Map from (production, dotPosition) to set of lookaheads
        Map<KernelEntry, Set<Symbol>> itemLookaheads = new HashMap<>();
        
        for (LR1Item i : items) {
            KernelEntry key = new KernelEntry(i.production, i.dotPosition);
            itemLookaheads.computeIfAbsent(key, k -> new HashSet<>()).add(i.lookahead);
        }
        
        // Create consolidated items (one per kernel with multiple lookaheads)
        Set<LR1Item> consolidated = new HashSet<>();
        for (Map.Entry<KernelEntry, Set<Symbol>> entry : itemLookaheads.entrySet()) {
            KernelEntry kernel = entry.getKey();
            for (Symbol lookahead : entry.getValue()) {
                consolidated.add(new LR1Item(kernel.production, kernel.dotPosition, lookahead));
            }
        }
        
        return consolidated;
    }
    
    // ... (Getters and KernelEntry class can remain as is)
    public Map<Integer, Map<Symbol, Action>> getActionTable() { return action; }
    public Map<Integer, Map<Symbol, Integer>> getGotoTable() { return gotoTable; }
    public List<String> getConflicts() { return conflicts; }
    public List<Set<LR1Item>> getLALRStates() { return lalrStates; }
    public Map<Integer, Map<Symbol, Integer>> getLALRTransitions() { return lalrTransitions; }
    public int getInitialState() { return initialState; }
    
    private static class KernelEntry {
        public final Production production;
        public final int dotPosition;
        
        KernelEntry(Production production, int dotPosition) {
            this.production = production;
            this.dotPosition = dotPosition;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof KernelEntry)) return false;
            KernelEntry o = (KernelEntry) obj;
            return dotPosition == o.dotPosition && production.equals(o.production);
        }
        
        @Override
        public int hashCode() {
            int r = production.hashCode();
            r = 31 * r + dotPosition;
            return r;
        }
    }
}