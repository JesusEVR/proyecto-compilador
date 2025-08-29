package com.compiler.lexer.regex;

import java.util.Stack;
import com.compiler.lexer.nfa.State;
import com.compiler.lexer.nfa.Transition;
import com.compiler.lexer.nfa.NFA;

/**
 * RegexParser
 * -----------
 * This class provides functionality to convert infix regular expressions into nondeterministic finite automata (NFA)
 * using Thompson's construction algorithm. It supports standard regex operators: concatenation (·), union (|),
 * Kleene star (*), optional (?), and plus (+). The conversion process uses the Shunting Yard algorithm to transform
 * infix regex into postfix notation, then builds the corresponding NFA.
 *
 * Features:
 * - Parses infix regular expressions and converts them to NFA.
 * - Supports regex operators: concatenation, union, Kleene star, optional, plus.
 * - Implements Thompson's construction rules for NFA generation.
 *
 * Example usage:
 * <pre>
 *     RegexParser parser = new RegexParser();
 *     NFA nfa = parser.parse("a(b|c)*");
 * </pre>
 */

/**
 * Parses regular expressions and constructs NFAs using Thompson's construction.
 */
public class RegexParser {
    /**
     * Default constructor for RegexParser.
     */
    public RegexParser() {
    }

    /**
     * Converts an infix regular expression to an NFA.
     *
     * @param infixRegex The regular expression in infix notation.
     * @return The constructed NFA.
     */
    public NFA parse(String infixRegex) {
        // Pseudocode: Convert infix to postfix, then build NFA from postfix
        String postfixRegex = ShuntingYard.toPostfix(infixRegex);
        return buildNfaFromPostfix(postfixRegex);

    }

    /**
     * Builds an NFA from a postfix regular expression.
     *
     * @param postfixRegex The regular expression in postfix notation.
     * @return The constructed NFA.
     */
    private NFA buildNfaFromPostfix(String postfixRegex) {
        // Pseudocode: For each char in postfix, handle operators and operands using a stack

        // Stack initialized
        Stack<NFA> pila = new Stack<>();

        for (int i = 0; i < postfixRegex.length(); i++) {

            char caracter = postfixRegex.charAt(i);

            if (isOperand(caracter)) {
                NFA automataUnSoloCaracter = createNfaForCharacter(caracter);
                pila.push(automataUnSoloCaracter);
            } else if (caracter == '|') {
                handleUnion(pila);
            } else if (caracter == '·' ) {
                handleConcatenation(pila);
            } else if (caracter == '?') {
                handleOptional(pila);
            } else if (caracter == '+') {
                handlePlus(pila);
            } else if (caracter == '*') {
                handleKleeneStar(pila);
            } else {
                throw new IllegalArgumentException("Error. Operador no válido: " + caracter);
            }
        }
        
        return pila.pop();
    }

    /**
     * Handles the '?' operator (zero or one occurrence).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or one occurrence.
     * @param stack The NFA stack.
     */
    private void handleOptional(Stack<NFA> stack) {
        // Pseudocode: Pop NFA, create new start/end, add epsilon transitions for zero/one occurrence
        if (stack.isEmpty()) {
            throw new IllegalStateException("Error. La pila está vacía. No es posible usar el operador 'opcional' (?).");
        }
        
        NFA atfn = stack.pop(); // Pop the NFA to apply ? to
        
        State inicialN = new State(); // New start state
        State finalN = new State(); // New end state
        
        // Epsilon transition: nuevo inicial -> inicial anterior (una rep.)
        inicialN.transitions.add(new Transition(null, atfn.startState));
        
        // Epsilon transition: nuevo inicial -> nuevo final (cero rep.)
        inicialN.transitions.add(new Transition(null, finalN));
        
        // Epsilon transition: final anterior -> nuevo final
        atfn.endState.transitions.add(new Transition(null, finalN));

        // Old end state is no longer final
        atfn.endState.isFinal = false;
        
        // Push the new NFA onto the stack
        stack.push(new NFA(inicialN, finalN));
    }

    /**
     * Handles the '+' operator (one or more occurrences).
     * Pops an NFA from the stack and creates a new NFA that accepts one or more occurrences.
     * @param stack The NFA stack.
     */
    private void handlePlus(Stack<NFA> stack) {
        // Pseudocode: Pop NFA, create new start/end, add transitions for one or more occurrence
        if (stack.isEmpty()) {
            throw new IllegalStateException("Error: la pila está vacía. No es posible usar el operador 'más' (+).");
        }
            State inicioN = new State(); // New start state
            State finalN = new State(); // New end state

            NFA atfn = stack.pop(); // Pop the NFA to apply Plus to

            // Epsilon transition: nuevo inicial -> incial anterior
            inicioN.transitions.add(new Transition(null, atfn.startState));

            // Epsilon transition: final antrior -> inicial anterior
            atfn.endState.transitions.add(new Transition(null, atfn.startState));

            // Epsilon transition: final anterior -> nuevo final
            atfn.endState.transitions.add(new Transition(null, finalN));

            // Old end state is no longer final
            atfn.endState.isFinal = false;

            // Push the new NFA onto the stack
            stack.push(new NFA(inicioN, finalN));
    }
    
    /**
     * Creates an NFA for a single character.
     * @param c The character to create an NFA for.
     * @return The constructed NFA.
     */
    private NFA createNfaForCharacter(char c) {
        // Pseudocode: Create start/end state, add transition for character
        State inicio = new State(); // Start state
        State fin = new State(); // End state
        
        // Transition for the character c
        inicio.transitions.add(new Transition(c, fin));

        // We create and return the NFA
        return new NFA(inicio, fin);
    }

    /**
     * Handles the concatenation operator (·).
     * Pops two NFAs from the stack and connects them in sequence.
     * @param stack The NFA stack.
     */
    private void handleConcatenation(Stack<NFA> stack) {
        // Pseudocode: Pop two NFAs, connect end of first to start of second
        if (stack.size() < 2) {
            throw new IllegalStateException("Error: Se necesitan al menos dos AFNs para la concatenación (·).");
        } 
            // Pop two NFAs (el orden debe ser así: 2 -> 1, si no da error esta cosa)
            NFA at2 = stack.pop();
            NFA at1 = stack.pop();

            // Epsilon transition: final del primero -> inicial del segundo
            at1.endState.transitions.add(new Transition(null, at2.startState));
            // At1's old end state is no longer final
            at1.endState.isFinal = false;

            // Push the new NFA onto the stack
            stack.push(new NFA(at1.startState, at2.endState));

    }

    /**
     * Handles the union operator (|).
     * Pops two NFAs from the stack and creates a new NFA that accepts either.
     * @param stack The NFA stack.
     */
    private void handleUnion(Stack<NFA> stack) {
        // Pseudocode: Pop two NFAs, create new start/end, add epsilon transitions for union
        if (stack.size() < 2) {
            throw new IllegalStateException("Error: Se necesitan al menos dos AFNs para la unión (|).");
        } 
            // Pop two NFAs
            NFA at1 = stack.pop();
            NFA at2 = stack.pop();

            State inicialN = new State(); // New start state
            State finalN = new State(); // New end state

            // Epsilon transition: nuevo inicial -> inicial de a1
            inicialN.transitions.add(new Transition(null, at1.startState));
            // Epsilon transition: nuevo inicial -> inicial de a2
            inicialN.transitions.add(new Transition(null, at2.startState));
            // Epsilon transition: final de a1 -> nuevo final
            at1.endState.transitions.add(new Transition(null, finalN));
            // Epsilon transition: final de a2 -> nuevo final
            at2.endState.transitions.add(new Transition(null, finalN));

            // Old end states are no longer final ones
            at1.endState.isFinal = false;
            at2.endState.isFinal = false;

            // Push the new NFA onto the stack
            stack.push(new NFA(inicialN, finalN));
        
    }

    /**
     * Handles the Kleene star operator (*).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or more repetitions.
     * @param stack The NFA stack.
     */
    private void handleKleeneStar(Stack<NFA> stack) {
        // Pseudocode: Pop NFA, create new start/end, add transitions for zero or more repetitions

        if (stack.isEmpty()) {
            throw new IllegalStateException("Error: La pila está vacía. No es posible usar la 'Estrella de Kleene' (*).");
        } 
            State inicioN = new State(); // New start state
            State finalN = new State(); // New end state

            NFA atfn = stack.pop(); // Pop the NFA to apply Kleene star to

            // Epsilon transition: nuevo inicial -> nuevo final (cero repeticiones)
            inicioN.transitions.add(new Transition(null, finalN));

            // Epsilon transition: nuevo inicial -> incial anterior
            inicioN.transitions.add(new Transition(null, atfn.startState));

            // Epsilon transition: final antrior -> inicial anterior
            atfn.endState.transitions.add(new Transition(null, atfn.startState));

            // Epsilon transition: final anterior -> nuevo final
            atfn.endState.transitions.add(new Transition(null, finalN));

            // Old end state is no longer final
            atfn.endState.isFinal = false;

            // Push the new NFA onto the stack
            stack.push(new NFA(inicioN, finalN));
        
    }

    /**
     * Checks if a character is an operand (not an operator).
     * @param c The character to check.
     * @return True if the character is an operand, false if it is an operator.
     */
    private boolean isOperand(char c) {
        // Pseudocode: Return true if c is not an operator
        if (c == '|' || c == '·' || c == '*' || c == '?' || c == '+') {
            return false;
        } else {
            return true;
        }
    }
}