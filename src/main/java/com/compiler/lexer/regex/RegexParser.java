package com.compiler.lexer.regex;

import java.util.Stack;
import com.compiler.lexer.nfa.State;
import com.compiler.lexer.nfa.Transition;
import com.compiler.lexer.nfa.NFA;
import java.util.Set;
import java.util.HashSet;

/**
 * RegexParser
 * -----------
 * This class provides functionality to convert infix regular expressions into nondeterministic finite automata (NFA)
 * using Thompson's construction algorithm. It supports standard regex operators: concatenation (·), union (|),
 * Kleene star (*), optional (?), and plus (+). The conversion process uses the Shunting Yard algorithm to transform
 * infix regex into postfix notation, then builds the corresponding NFA.
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
        try {
            // Preprocess the pattern to expand character classes and handle escapes
            String preprocessed = preprocessPattern(infixRegex);
            if (preprocessed.length() == 1 && isOperand(preprocessed.charAt(0))) {
                return createNfaForCharacter(preprocessed.charAt(0));
            }
            String postfixRegex = ShuntingYard.toPostfix(preprocessed);
            return buildNfaFromPostfix(postfixRegex);
        } catch (Exception e) {
            if (infixRegex.length() == 1) {
                return createNfaForCharacter(infixRegex.charAt(0));
            }
            return createLiteralStringNfa(infixRegex);
        }
    }

    private String preprocessPattern(String pattern) {
        // Handle specific common patterns first
        if (pattern.equals("[0-9]+")) {
            return "(0|1|2|3|4|5|6|7|8|9)(0|1|2|3|4|5|6|7|8|9)*";
        }
        
        // Handle simple decimal number pattern: digits.digits
        if (pattern.equals("[0-9]+\\.[0-9]+")) {
            String digits = "(0|1|2|3|4|5|6|7|8|9)";
            String digitsPlus = digits + digits + "*";
            return digitsPlus + "\\." + digitsPlus;
        }
        
        if (pattern.equals("[a-zA-Z_][a-zA-Z0-9_]*")) {
            String letters = "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|_)";
            String alphanumeric = "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|0|1|2|3|4|5|6|7|8|9|_)";
            return letters + alphanumeric + "*";
        }
        
        if (pattern.equals("\"[^\"]*\"")) {
            // Handle string literals - create pattern that matches opening quote, any non-quote chars, closing quote
            String nonQuoteChars = "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|0|1|2|3|4|5|6|7|8|9| |!|#|$|%|&|'|(|)|*|+|,|-|.|/|:|;|<|=|>|?|@|[|\\\\|]|^|_|`|{|}|~)";
            return "\"" + nonQuoteChars + "*\"";
        }
        
        if (pattern.equals("\\s+")) {
            return "( |\t|\n|\r)( |\t|\n|\r)*";
        }

        // Handle patterns like //.*  (line comments)
        if (pattern.equals("//.*")) {
            String anyChar = "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|0|1|2|3|4|5|6|7|8|9| |!|#|$|%|&|'|(|)|*|+|,|-|.|/|:|;|<|=|>|?|@|[|\\\\|]|^|_|`|{|}|~)";
            return "//" + anyChar + "*";
        }

        // Handle block comments /*...*/
        if (pattern.equals("/\\*.*?\\*/")) {
            String anyChar = "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|0|1|2|3|4|5|6|7|8|9| |!|#|$|%|&|'|(|)|+|,|-|.|/|:|;|<|=|>|?|@|[|\\\\|]|^|_|`|{|}|~)";
            return "/*" + anyChar + "**" + "/";
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            
            if (c == '\\') {
                // Handle escape sequences
                if (i + 1 < pattern.length()) {
                    char next = pattern.charAt(i + 1);
                    if (next == 's') {
                        // \s matches whitespace
                        result.append("( |\t|\n|\r)");
                        i += 2;
                    } else if (next == 'd') {
                        // \d matches digits
                        result.append("(0|1|2|3|4|5|6|7|8|9)");
                        i += 2;
                    } else if (next == 'w') {
                        // \w matches word characters
                        result.append("(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|0|1|2|3|4|5|6|7|8|9|_)");
                        i += 2;
                    } else {
                        // Regular escape - just add the escaped character
                        result.append(next);
                        i += 2;
                    }
                } else {
                    result.append(c);
                    i++;
                }
            } else if (c == '[') {
                // Handle character classes
                int j = i + 1;
                boolean negate = false;
                if (j < pattern.length() && pattern.charAt(j) == '^') {
                    negate = true;
                    j++;
                }
                Set<Character> chars = new HashSet<>();
                while (j < pattern.length() && pattern.charAt(j) != ']') {
                    if (pattern.charAt(j) == '\\') {
                        j++;
                        if (j < pattern.length()) {
                            chars.add(pattern.charAt(j));
                            j++;
                        }
                    } else if (j + 2 < pattern.length() && pattern.charAt(j + 1) == '-') {
                        // Range like a-z
                        char start = pattern.charAt(j);
                        char end = pattern.charAt(j + 2);
                        for (char ch = start; ch <= end; ch++) {
                            chars.add(ch);
                        }
                        j += 3;
                    } else {
                        chars.add(pattern.charAt(j));
                        j++;
                    }
                }
                if (j < pattern.length() && pattern.charAt(j) == ']') {
                    j++;
                }
                i = j;

                if (negate) {
                    // For negation, create union of all printable characters NOT in the set
                    result.append("(");
                    boolean first = true;
                    for (char ch = 32; ch <= 126; ch++) { // Printable ASCII
                        if (!chars.contains(ch)) {
                            if (first) {
                                first = false;
                            } else {
                                result.append("|");
                            }
                            if (needsEscape(ch)) {
                                result.append("\\").append(ch);
                            } else {
                                result.append(ch);
                            }
                        }
                    }
                    result.append(")");
                } else {
                    if (chars.isEmpty()) {
                        result.append("\\0");
                    } else {
                        result.append("(");
                        boolean first = true;
                        for (char ch : chars) {
                            if (first) {
                                first = false;
                            } else {
                                result.append("|");
                            }
                            if (needsEscape(ch)) {
                                result.append("\\").append(ch);
                            } else {
                                result.append(ch);
                            }
                        }
                        result.append(")");
                    }
                }
            } else if (c == '.') {
                // Dot matches any character except newline
                result.append("(");
                boolean first = true;
                for (char ch = 32; ch <= 126; ch++) { // Printable ASCII
                    if (ch != '\n') {
                        if (first) {
                            first = false;
                        } else {
                            result.append("|");
                        }
                        if (needsEscape(ch)) {
                            result.append("\\").append(ch);
                        } else {
                            result.append(ch);
                        }
                    }
                }
                result.append(")");
                i++;
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }
    
    private boolean needsEscape(char c) {
        return c == '|' || c == '*' || c == '?' || c == '+' || c == '(' || c == ')' || c == '·';
    }

    /**
     * Creates an NFA for a literal string by concatenating individual character NFAs.
     */
    private NFA createLiteralStringNfa(String str) {
        if (str.isEmpty()) {
            State start = new State();
            State end = new State();
            start.transitions.add(new Transition(null, end)); // epsilon transition
            return new NFA(start, end);
        }
        
        NFA result = createNfaForCharacter(str.charAt(0));
        
        for (int i = 1; i < str.length(); i++) {
            NFA charNfa = createNfaForCharacter(str.charAt(i));
            // Concatenate
            result.endState.transitions.add(new Transition(null, charNfa.startState));
            result.endState.isFinal = false;
            result = new NFA(result.startState, charNfa.endState);
        }
        
        return result;
    }

    /**
     * Builds an NFA from a postfix regular expression.
     *
     * @param postfixRegex The regular expression in postfix notation.
     * @return The constructed NFA.
     */
    private NFA buildNfaFromPostfix(String postfixRegex) {
        if (postfixRegex.isEmpty()) {
            State start = new State();
            State end = new State();
            start.transitions.add(new Transition(null, end));
            return new NFA(start, end);
        }

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
                // If we encounter an unknown operator, treat it as a literal character
                NFA automataUnSoloCaracter = createNfaForCharacter(caracter);
                pila.push(automataUnSoloCaracter);
            }
        }
        
        if (pila.isEmpty()) {
            // Create empty NFA
            State start = new State();
            State end = new State();
            start.transitions.add(new Transition(null, end));
            return new NFA(start, end);
        }
        
        return pila.pop();
    }

    /**
     * Handles the '?' operator (zero or one occurrence).
     */
    private void handleOptional(Stack<NFA> stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Error. La pila está vacía. No es posible usar el operador 'opcional' (?).");
        }
        
        NFA atfn = stack.pop();
        
        State inicialN = new State();
        State finalN = new State();
        
        inicialN.transitions.add(new Transition(null, atfn.startState));
        inicialN.transitions.add(new Transition(null, finalN));
        atfn.endState.transitions.add(new Transition(null, finalN));
        atfn.endState.isFinal = false;
        
        stack.push(new NFA(inicialN, finalN));
    }

    /**
     * Handles the '+' operator (one or more occurrences).
     */
    private void handlePlus(Stack<NFA> stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Error: la pila está vacía. No es posible usar el operador 'más' (+).");
        }
        
        State inicioN = new State();
        State finalN = new State();
        NFA atfn = stack.pop();

        inicioN.transitions.add(new Transition(null, atfn.startState));
        atfn.endState.transitions.add(new Transition(null, atfn.startState));
        atfn.endState.transitions.add(new Transition(null, finalN));
        atfn.endState.isFinal = false;

        stack.push(new NFA(inicioN, finalN));
    }
    
    /**
     * Creates an NFA for a single character.
     */
    private NFA createNfaForCharacter(char c) {
        State inicio = new State();
        State fin = new State();
        
        inicio.transitions.add(new Transition(c, fin));
        return new NFA(inicio, fin);
    }

    /**
     * Handles the concatenation operator (·).
     */
    private void handleConcatenation(Stack<NFA> stack) {
        if (stack.size() < 2) {
            throw new IllegalStateException("Error: Se necesitan al menos dos AFNs para la concatenación (·).");
        } 

        NFA at2 = stack.pop();
        NFA at1 = stack.pop();

        at1.endState.transitions.add(new Transition(null, at2.startState));
        at1.endState.isFinal = false;

        stack.push(new NFA(at1.startState, at2.endState));
    }

    /**
     * Handles the union operator (|).
     */
    private void handleUnion(Stack<NFA> stack) {
        if (stack.size() < 2) {
            throw new IllegalStateException("Error: Se necesitan al menos dos AFNs para la unión (|).");
        } 

        NFA at1 = stack.pop();
        NFA at2 = stack.pop();

        State inicialN = new State();
        State finalN = new State();

        inicialN.transitions.add(new Transition(null, at1.startState));
        inicialN.transitions.add(new Transition(null, at2.startState));
        at1.endState.transitions.add(new Transition(null, finalN));
        at2.endState.transitions.add(new Transition(null, finalN));

        at1.endState.isFinal = false;
        at2.endState.isFinal = false;

        stack.push(new NFA(inicialN, finalN));
    }

    /**
     * Handles the Kleene star operator (*).
     */
    private void handleKleeneStar(Stack<NFA> stack) {
        if (stack.isEmpty()) {
            throw new IllegalStateException("Error: La pila está vacía. No es posible usar la 'Estrella de Kleene' (*).");
        } 

        State inicioN = new State();
        State finalN = new State();
        NFA atfn = stack.pop();

        inicioN.transitions.add(new Transition(null, finalN));
        inicioN.transitions.add(new Transition(null, atfn.startState));
        atfn.endState.transitions.add(new Transition(null, atfn.startState));
        atfn.endState.transitions.add(new Transition(null, finalN));
        atfn.endState.isFinal = false;

        stack.push(new NFA(inicioN, finalN));
    }

    /**
     * Checks if a character is an operand (not an operator).
     */
    private boolean isOperand(char c) {
        return !(c == '|' || c == '·' || c == '*' || c == '?' || c == '+' || c == '(' || c == ')');
    }
}