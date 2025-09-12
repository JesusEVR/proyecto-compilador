package com.compiler.lexer.nfa;

import java.util.List;
import java.util.ArrayList;
import com.compiler.lexer.tokenizer.TokenType;

/**
 * Represents a state in a Non-deterministic Finite Automaton (NFA).
 * Each state has a unique identifier, a list of transitions to other states,
 * a flag indicating whether it is a final (accepting) state, and optional token information.
 *
 * <p>
 * Fields:
 * <ul>
 *   <li>{@code id} - Unique identifier for the state.</li>
 *   <li>{@code transitions} - List of transitions from this state to others.</li>
 *   <li>{@code isFinal} - Indicates if this state is an accepting state.</li>
 *   <li>{@code tokenType} - The type of token this state recognizes (if final).</li>
 *   <li>{@code tokenPriority} - Priority for conflict resolution.</li>
 * </ul>
 *
 *
 * <p>
 * The {@code nextId} static field is used to assign unique IDs to each state.
 * </p>
 */
public class State {
    private static int nextId = 0;
    /**
     * Unique identifier for this state.
     */
    public final int id;

    /**
     * List of transitions from this state to other states.
     */
    public List<Transition> transitions;

    /**
     * Indicates if this state is a final (accepting) state.
     */
    public boolean isFinal;

    /**
     * The token type this state recognizes (null if not a final state or doesn't recognize a token).
     */
    private TokenType tokenType;
    
    /**
     * Priority of the token type (lower values = higher priority). Used for conflict resolution.
     */
    private int tokenPriority;

    /**
     * Constructs a new state with a unique identifier and no transitions.
     * The state is not final by default.
     */
    public State() {
        this.id = nextId++;
        this.transitions = new ArrayList<>();
        this.isFinal = false;
        this.tokenType = null;
        this.tokenPriority = Integer.MAX_VALUE;
    }

    /**
     * Checks if this state is a final (accepting) state.
     * @return true if this state is final, false otherwise
     */
    public boolean isFinal() {
        if(this.isFinal) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the token type this state recognizes.
     * @param tokenType The token type.
     * @param priority The priority of this token type (lower values = higher priority).
     */
    public void setTokenType(TokenType tokenType, int priority) {
        // Only set if this has higher priority (lower priority value) or if no token type is set
        if (this.tokenType == null || priority < this.tokenPriority) {
            this.tokenType = tokenType;
            this.tokenPriority = priority;
        }
    }

    /**
     * Gets the token type this state recognizes.
     * @return The token type, or null if this state doesn't recognize a token.
     */
    public TokenType getTokenType() {
        return tokenType;
    }

    /**
     * Gets the priority of the token type.
     * @return The token priority.
     */
    public int getTokenPriority() {
        return tokenPriority;
    }

    /**
     * Returns the states reachable from this state via epsilon transitions (symbol == null).
     * @return a list of states reachable by epsilon transitions
     */
    public List<State> getEpsilonTransitions() {
        List<State> estadosEpsilon = new ArrayList<>();
        for(Transition t : transitions) {
            if(t.symbol == null) {
                estadosEpsilon.add(t.toState);
            }
        }
        return estadosEpsilon;
    }

    /**
     * Returns the states reachable from this state via a transition with the given symbol.
     * @param symbol the symbol for the transition
     * @return a list of states reachable by the given symbol
     */
    public List<State> getTransitions(char symbol) {
        List<State> estadosSimb = new ArrayList<>();
        for(Transition t : transitions) {
            if( t.symbol != null && t.symbol == symbol) {
                estadosSimb.add(t.toState);
            }
        }
        return estadosSimb;
    }
}