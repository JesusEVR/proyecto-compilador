package com.compiler.lexer;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;
import java.util.*;

/**
 *  NfaToDfaConverter
 *  -----------------
 *  This class provides a static method to convert a Non-deterministic Finite Automaton (NFA)
 *  into a Deterministic Finite Automaton (DFA) using the standard subset construction algorithm.
 */
/**
 * Utility class for converting NFAs to DFAs using the subset construction
 * algorithm.
 */
public class NfaToDfaConverter {

	/**
	 * Default constructor for NfaToDfaConverter.
	 */
	public NfaToDfaConverter() {
	}

	/**
	 * Converts an NFA to a DFA using the subset construction algorithm.
	 * Each DFA state represents a set of NFA states. Final states are marked if any
	 * NFA state in the set is final.
	 *
	 * @param nfa      The input NFA
	 * @param alphabet The input alphabet (set of characters)
	 * @return The resulting DFA
	 */
	public static DFA convertNfaToDfa(NFA nfa, Set<Character> alphabet) {

		Queue<DfaState> estadosPorProcesar = new LinkedList<>();
		List<DfaState> estadosDfa = new ArrayList<>();

		// Create initial DFA state from epsilon-closure of NFA start state
		DfaState inicialDFA = createInitialDfaState(nfa);
		estadosDfa.add(inicialDFA);
		estadosPorProcesar.add(inicialDFA);

		// Process all unprocessed DFA states
		processStates(estadosPorProcesar, estadosDfa, alphabet);

		// Return the DFA with its start state and all states
		return new DFA(inicialDFA, estadosDfa);
	}

	/**
	 * Computes the epsilon-closure of a set of NFA states.
	 * The epsilon-closure is the set of states reachable by epsilon (null)
	 * transitions.
	 *
	 * @param states The set of NFA states.
	 * @return The epsilon-closure of the input states.
	 */
	private static Set<State> epsilonClosure(Set<State> states) {
		// 1. Initialize closure with input states
		Set<State> closure = new HashSet<>(states);

		// 2. Use stack to process states
		Stack<State> pila = new Stack<>();
		pila.addAll(states);

		// 3. For each state, add all reachable states via epsilon transitions
		while (pila.isEmpty() == false) {
			State estado = pila.pop();

			// Get epsilon transitions form current state
			List<State> epsilonTransitions = estado.getEpsilonTransitions();

			for (State state : epsilonTransitions) {
				// If the state isn't in the closure, then we add it
				if (closure.contains(state) == false) {
					closure.add(state);
					pila.push(state);
				}
			}
		}
		// 4. Return closure set
		return closure;
	}

	/**
	 * Returns the set of states reachable from a set of NFA states by a given
	 * symbol.
	 *
	 * @param states The set of NFA states.
	 * @param symbol The input symbol.
	 * @return The set of reachable states.
	 */
	private static Set<State> move(Set<State> states, char symbol) {

		Set<State> resultSet = new HashSet<>();
		// 1. For each state in input set:
		for (State estado : states) {
			// For each transition with given symbol:
			List<State> trans = estado.getTransitions(symbol);
			// Add destination state to result set
			resultSet.addAll(trans);
		}
		// 2. Return result set
		return resultSet;
	}

	/**
	 * Finds an existing DFA state representing a given set of NFA states.
	 *
	 * @param dfaStates     The list of DFA states.
	 * @param nextNfaStates The set of NFA states to search for.
	 * @return The matching DFA state, or null if not found.
	 */
	private static DfaState findDfaState(List<DfaState> dfaStates, Set<State> nextNfaStates) {
		// 1. For each DFA state in list:
		for (DfaState estado : dfaStates) {
			// - If its NFA state set equals target set, return DFA state
			if (estado.getName().equals(nextNfaStates)) {
				return estado;
			}
		}
		// 2. If not found, return null
		return null;
	}

	/**
	 * Creates the initial DFA state from the epsilon-closure
	 * of the NFA start state.
	 *
	 * @param nfa The input NFA.
	 * @return The initial DFA state.
	 */
	private static DfaState createInitialDfaState(NFA nfa) {
		Set<State> inicialesNfa = new HashSet<>();
		inicialesNfa.add(nfa.getStartState());

		Set<State> cerradura = epsilonClosure(inicialesNfa);
		DfaState inicialDFA = new DfaState(cerradura);

		if (containsFinalState(cerradura)) {
			inicialDFA.setFinal(true);
		}
		return inicialDFA;
	}

	/**
	 * Processes all unprocessed DFA states until the queue is empty.
	 * For each state, computes its transitions for all symbols in the alphabet.
	 *
	 * @param estadosPorProcesar The queue of DFA states to process.
	 * @param estadosDfa         The list of all DFA states created so far.
	 * @param alphabet           The input alphabet.
	 */
	private static void processStates(Queue<DfaState> estadosPorProcesar, List<DfaState> estadosDfa,
			Set<Character> alphabet) {

		while (estadosPorProcesar.isEmpty() == false) {
			DfaState estadoActual = estadosPorProcesar.poll();

			for (Character s : alphabet) {
				handleTransition(estadoActual, s, estadosDfa, estadosPorProcesar);
			}
		}
	}

	/**
	 * Handles the creation or reuse of a target DFA state
	 * when transitioning from a given DFA state on a specific input symbol.
	 *
	 * @param estadoActual       The current DFA state.
	 * @param symbol             The input symbol.
	 * @param estadosDfa         The list of all DFA states created so far.
	 * @param estadosPorProcesar The queue of DFA states to process.
	 */
	private static void handleTransition(DfaState estadoActual, Character symbol, List<DfaState> estadosDfa,
			Queue<DfaState> estadosPorProcesar) {
		// - Compute move and epsilon-closure for current DFA state
		Set<State> moveComputed = move(estadoActual.getName(), symbol);

		if (moveComputed.isEmpty() == false) {
			Set<State> nextNfaStates = epsilonClosure(moveComputed);

			// Find if an existing DFA state already represents this set
			DfaState nextDfaStates = findDfaState(estadosDfa, nextNfaStates);

			if (nextDfaStates == null) {
				nextDfaStates = new DfaState(nextNfaStates);

				if (containsFinalState(nextNfaStates)) {
					nextDfaStates.setFinal(true);
				}

				estadosDfa.add(nextDfaStates);
				estadosPorProcesar.add(nextDfaStates);
			}
			estadoActual.addTransition(symbol, nextDfaStates);
		}
	}

	/**
	 * Checks if a set of NFA states contains at least one final state.
	 *
	 * @param states The set of NFA states.
	 * @return True if at least one NFA state is final, false otherwise.
	 */
	private static boolean containsFinalState(Set<State> states) {
		for (State s : states) {
			if (s.isFinal()) {
				return true;
			}
		}
		return false;
	}
}
