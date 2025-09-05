package com.compiler.lexer.dfa;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashMap;
import com.compiler.lexer.nfa.State;

/**
 *  DfaState
 *  --------
 *  Represents a single state in a Deterministic Finite Automaton (DFA).
 *  Each DFA state corresponds to a set of states from the original NFA.
 *  Provides methods for managing transitions, checking finality, and equality based on NFA state sets.
 */
public class DfaState {
    /**
     *  Static counter to assign unique IDs to each DFA state
     */ 
    private static int nextId = 0;
    /**
     *  Unique identifier for this DFA state.
     */
    public final int id;
    /**
     *  The set of NFA states this DFA state represents.
     */
    public final Set<State> nfaStates;
    /**
     *  Indicates whether this DFA state is a final (accepting) state.
     */
    public boolean isFinal;
    /**
     *  Map of input symbols to destination DFA states (transitions).
     */
    public final Map<Character, DfaState> transitions;

    /**
     *  Constructs a new DFA state.
     *  @param nfaStates The set of NFA states that this DFA state represents.
     */
    public DfaState(Set<State> nfaStates) {
        this.nfaStates = nfaStates;
        this.id = nextId++;
        this.isFinal = false;
        this.transitions = new HashMap<>();
    }

    /**
     *  Adds a transition from this state to another on a given symbol.
     *  @param symbol The input symbol for the transition.
     *  @param toState The destination DFA state.
     */
    public void addTransition(Character symbol, DfaState toState) {
        transitions.put(symbol, toState);
    }

    /**
     *  Two DfaStates are considered equal if they represent the same set of NFA states.
     *  @param obj The object to compare.
     *  @return True if the states are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DfaState other)) {
        return false;
        }
        return this.nfaStates.size() == other.nfaStates.size() && this.nfaStates.containsAll(other.nfaStates);
    }

    /**
     *  Sets the finality of the DFA state.
     *  @param isFinal True if this state is a final state, false otherwise.
     */
    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    /**
     *  Checks if the DFA state is final.
     *  @return True if this state is a final state, false otherwise.
     */
    public boolean isFinal() {
        if(this.isFinal == true){
            return true;
        } else {
            return false;
        }
    }

    /**
     *  Returns the set of NFA states this DFA state represents.
     *  @return The set of NFA states.
     */
    public Set<State> getName() {
        return this.nfaStates;
    }

    /**
     *  Gets the transition for a given input symbol.
     *  @param symbol The input symbol for the transition.
     *  @return The destination DFA state for the transition, or null if there is no transition for the given symbol.
     */
    public DfaState getTransition(char symbol) {
        return transitions.get(symbol);
    }

    /**
     *  Returns all the transitions of this state.
     *  @return Map of input symbols to destination DFA states (transitions).
     */
    public Map<Character, DfaState> getTransitions() {
        return this.transitions;
    }

    /**
     *  The hash code is based on the set of NFA states.
     *  @return The hash code for this DFA state.
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.nfaStates);
    }

    /**
     *  Returns a string representation of the DFA state, including its id and
     *  finality.
     *  
     *  @return String representation of the state.
     */
    @Override
    public String toString() {

        String estado = "State " + id + (isFinal ? " (final state)" : "");

        String estadosNFA = "  NFA States: " + nfaStates;

        String trans = "  Transitions:";

        if (transitions.isEmpty()) {
            trans += " (none)";
        } else {
            for (var entry : transitions.entrySet()) {
                trans += "\n    '" + entry.getKey() + "' --> State " + entry.getValue().id;
            }
        }
        return estado + "\n" + estadosNFA + "\n" + trans;
    }
}