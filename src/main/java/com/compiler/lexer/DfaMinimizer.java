package com.compiler.lexer;

import java.util.*;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;


/**
 * Implements DFA minimization using the table-filling algorithm.
 */
public class DfaMinimizer {
    /**
     * Default constructor for DfaMinimizer.
     */
        public DfaMinimizer() {
        }

    /**
     * Minimizes a given DFA using the table-filling algorithm.
     *
     * @param originalDfa The original DFA to be minimized.
     * @param alphabet The set of input symbols.
     * @return A minimized DFA equivalent to the original.
     */
    public static DFA minimizeDfa(DFA originalDfa, Set<Character> alphabet) {
        // 1. Collect and sort all DFA states
        List<DfaState> allStates = new ArrayList<>(originalDfa.getAllStates());
        allStates.sort(Comparator.comparingInt(s -> s.id));
        
        // 2. Initialize table of state pairs
        Map<Pair, Boolean> table = initializeDistinguishabilityTable(allStates);
        
        // 3. Iteratively mark pairs as distinguishable
        markDistinguishablePairs(allStates, table, alphabet);
        
        // 4. Partition states into equivalence classes
        List<Set<DfaState>> particiones = createPartitions(allStates, table);
        
        // 5. Create new minimized states for each partition
        Map<Set<DfaState>, DfaState> partitionToState = new HashMap<>();
        List<DfaState> estadosMin = new ArrayList<>();
        
        for (Set<DfaState> p : particiones) {
            // Create new state for this partition
            DfaState estadoRepr = p.iterator().next();
            DfaState nuevoEstado = new DfaState(estadoRepr.nfaStates);
            
            // Set finality based on any state in the partition (all should be the same)
            nuevoEstado.setFinal(estadoRepr.isFinal());
            
            // Preserve token information - find the best priority token in the partition
            preserveTokenInformation(p, nuevoEstado);
            
            partitionToState.put(p, nuevoEstado);
            estadosMin.add(nuevoEstado);
        }
        
        // 6. Reconstruct transitions for minimized states
        reconstructTransitions(particiones, partitionToState, alphabet);
        
        // 7. Set start state
        DfaState estadoInicialOriginal = originalDfa.getStartState();
        DfaState estadoInicialMin = null;
        
        for (Set<DfaState> p : particiones) {
            if (p.contains(estadoInicialOriginal)) {
                estadoInicialMin = partitionToState.get(p);
                break;
            }
        }
        
        return new DFA(estadoInicialMin, estadosMin);
    }
    
    /**
     * Preserves token information from the partition states into the new minimized state.
     * Chooses the token with the best (lowest) priority.
     */
    private static void preserveTokenInformation(Set<DfaState> partition, DfaState newState) {
        DfaState bestTokenState = null;
        int bestPriority = Integer.MAX_VALUE;
        
        for (DfaState state : partition) {
            if (state.getTokenType() != null && state.getTokenPriority() < bestPriority) {
                bestPriority = state.getTokenPriority();
                bestTokenState = state;
            }
        }
        
        if (bestTokenState != null) {
            newState.setTokenType(bestTokenState.getTokenType(), bestTokenState.getTokenPriority());
        }
    }

    /**
     * Initializes the distinguishability table by marking pairs where one state is final and the other is not,
     * or where they have different token types.
     */
    private static Map<Pair, Boolean> initializeDistinguishabilityTable(List<DfaState> allStates) {
        Map<Pair, Boolean> table = new HashMap<>();
        
        // Initialize all pairs as false (not distinguishable)
        for (int i = 0; i < allStates.size(); i++) {
            for (int j = i + 1; j < allStates.size(); j++) {
                table.put(new Pair(allStates.get(i), allStates.get(j)), false);
            }
        }
        
        // Mark pairs as distinguishable if one is final and the other is not,
        // or if they have different token types
        for (int i = 0; i < allStates.size(); i++) {
            for (int j = i + 1; j < allStates.size(); j++) {
                DfaState s1 = allStates.get(i);
                DfaState s2 = allStates.get(j);
                
                // Distinguishable if finality differs
                if (s1.isFinal() != s2.isFinal()) {
                    table.put(new Pair(s1, s2), true);
                }
                // Also distinguishable if both are final but have different token types
                else if (s1.isFinal() && s2.isFinal()) {
                    if (!Objects.equals(s1.getTokenType(), s2.getTokenType())) {
                        table.put(new Pair(s1, s2), true);
                    }
                }
            }
        }
        
        return table;
    }

    /**
     * Iteratively marks pairs as distinguishable based on their transitions.
     *
     * @param allStates List of all DFA states.
     * @param table The distinguishability table to update.
     * @param alphabet The set of input symbols.
     */
    private static void markDistinguishablePairs(List<DfaState> allStates, Map<Pair, Boolean> table, Set<Character> alphabet) {
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            for (int i = 0; i < allStates.size(); i++) {
                for (int j = i + 1; j < allStates.size(); j++) {
                    DfaState s1 = allStates.get(i);
                    DfaState s2 = allStates.get(j);
                    Pair parActual = new Pair(s1, s2);
                    
                    // Skip if already marked as distinguishable
                    if (table.get(parActual)) {
                        continue;
                    }
                    
                    // Check transitions for each symbol in alphabet
                    for (Character s : alphabet) {
                        DfaState t1 = s1.getTransition(s);
                        DfaState t2 = s2.getTransition(s);
                        
                        // If only one has a transition, they're distinguishable
                        if ((t1 == null) != (t2 == null)) {
                            table.put(parActual, true);
                            cambio = true;
                            break;
                        }
                        
                        // If both have transitions, check if destination states are distinguishable
                        if (t1 != null && t2 != null && !t1.equals(t2)) {
                            Pair parTrans = new Pair(t1, t2);
                            if (table.get(parTrans)) {
                                table.put(parActual, true);
                                cambio = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Reconstructs transitions for the minimized DFA states.
     *
     * @param partitions List of state partitions.
     * @param partitionToState Map from partitions to their corresponding minimized states.
     * @param alphabet The set of input symbols.
     */
    private static void reconstructTransitions(List<Set<DfaState>> partitions, Map<Set<DfaState>, DfaState> partitionToState, Set<Character> alphabet) {
        for (Set<DfaState> partition : partitions) {
            DfaState nuevoEstado = partitionToState.get(partition);
            DfaState estadoRepr = partition.iterator().next();
            
            for (Character symbol : alphabet) {
                DfaState objetivoOriginal = estadoRepr.getTransition(symbol);
                if (objetivoOriginal != null) {
                    // Find which partition the target belongs to
                    Set<DfaState> transObjetivo = null;
                    for (Set<DfaState> p : partitions) {
                        if (p.contains(objetivoOriginal)) {
                            transObjetivo = p;
                            break;
                        }
                    }
                    
                    if (transObjetivo != null) {
                        DfaState targetState = partitionToState.get(transObjetivo);
                        nuevoEstado.addTransition(symbol, targetState);
                    }
                }
            }
        }
    }

    /**
     * Groups equivalent states into partitions using union-find.
     *
     * @param allStates List of all DFA states.
     * @param table Table indicating which pairs are distinguishable.
     * @return List of partitions, each containing equivalent states.
     */
    private static List<Set<DfaState>> createPartitions(List<DfaState> allStates, Map<Pair, Boolean> table) {
        // 1. Initialize each state as its own parent
        Map<DfaState, DfaState> parent = new HashMap<>();
        for (DfaState state : allStates) {
            parent.put(state, state);
        }
        
        // 2. For each pair not marked as distinguishable, union the states
        for (int i = 0; i < allStates.size(); i++) {
            for (int j = i + 1; j < allStates.size(); j++) {
                DfaState s1 = allStates.get(i);
                DfaState s2 = allStates.get(j);
                Pair par = new Pair(s1, s2);
                
                // If not distinguishable, they're equivalent - union them
                if (!table.get(par)) {
                    union(parent, s1, s2);
                }
            }
        }
        
        // 3. Group states by their root parent
        Map<DfaState, Set<DfaState>> raizParticiones = new HashMap<>();
        for (DfaState e : allStates) {
            DfaState raiz = find(parent, e);
            raizParticiones.computeIfAbsent(raiz, k -> new HashSet<>()).add(e);
        }
        
        // 4. Return list of partitions
        return new ArrayList<>(raizParticiones.values());
    }

    /**
     * Finds the root parent of a state in the union-find structure.
     * Implements path compression for efficiency.
     *
     * @param parent Parent map.
     * @param state State to find.
     * @return Root parent of the state.
     */
    private static DfaState find(Map<DfaState, DfaState> parent, DfaState state) {
        /*  If parent[state] == state, return state */
        if (parent.get(state).equals(state)) {
            return state;
        }
        
        /* Else, recursively find parent and apply path compression */
        DfaState root = find(parent, parent.get(state));
        parent.put(state, root);
        return root;
    }

    /**
     * Unites two states in the union-find structure.
     *
     * @param parent Parent map.
     * @param s1 First state.
     * @param s2 Second state.
     */
    private static void union(Map<DfaState, DfaState> parent, DfaState s1, DfaState s2) {

        /* Find roots of s1 and s2 */
        DfaState r1 = find(parent, s1);
        DfaState r2 = find(parent, s2);
        
        /* If roots are different, set parent of one to the other */
        if (r1.equals(r2) == false) {
            parent.put(r2, r1);
        }
    }

    /**
     * Helper class to represent a pair of DFA states in canonical order.
     * Used for table indexing and comparison.
     */
    private static class Pair {
        final DfaState s1;
        final DfaState s2;

        /**
         * Constructs a pair in canonical order (lowest id first).
         * @param s1 First state.
         * @param s2 Second state.
         */
        public Pair(DfaState s1, DfaState s2) {
            /*
             Assign s1 and s2 so that s1.id <= s2.id
            */
            if (s1.id <= s2.id) {
                this.s1 = s1;
                this.s2 = s2;
            } else {
                this.s1 = s2;
                this.s2 = s1;
            }
        }

        @Override
        public boolean equals(Object o) {
            /* Return true if both s1 and s2 ids match */
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pair)) {
                return false;
            }

            Pair pair = (Pair) o;

            if (s1.id == pair.s1.id && s2.id == pair.s2.id){
                return true;
            } else{
                return false;
            }
        }

        @Override
        public int hashCode() {
            /* Return hash of s1.id and s2.id */
            return Objects.hash(s1.id, s2.id);
        }
    }
}