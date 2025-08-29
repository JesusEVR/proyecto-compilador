package com.compiler.lexer.regex;

import java.util.Stack;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for regular expression parsing using the Shunting Yard
 * algorithm.
 * <p>
 * Provides methods to preprocess regular expressions by inserting explicit
 * concatenation operators, and to convert infix regular expressions to postfix
 * notation for easier parsing and NFA construction.
 */
/**
 * Utility class for regular expression parsing using the Shunting Yard
 * algorithm.
 */
public class ShuntingYard {

    /**
     * Default constructor for ShuntingYard.
     */
    public ShuntingYard() {
    }

    /**
     * Inserts the explicit concatenation operator ('·') into the regular
     * expression according to standard rules. This makes implicit
     * concatenations explicit, simplifying later parsing.
     *
     * @param regex Input regular expression (may have implicit concatenation).
     * @return Regular expression with explicit concatenation operators.
     */
    public static String insertConcatenationOperator(String regex) {

        // Check for null or empty input
        if (regex.isEmpty() || regex == null) {
            return regex;
        } else {

            // Initialize output string
            String salida = "";

            /*  
                Iterate through each character in the regex:
                - Append current character to output
                - If not at end of string:
                    - Check if current and next character form an implicit concatenation
                    - If so, append '·' to output
                Return output as string 
            */
            for (int i = 0; i < regex.length(); i++) {
                // Get current character
                char caracActual = regex.charAt(i);
                // Append current character to output
                salida += caracActual;
                // If not at the end of the string, check for implicit concatenation
                if (i < regex.length() - 1) {
                    // Get next character
                    char siguiente = regex.charAt(i + 1);
                    // Check if we need to insert concatenation operator
                    // Conditions for implicit concatenation:
                    if ((isOperand(caracActual) || caracActual == ')' || caracActual == '*' || caracActual == '+' 
                        || caracActual == '?') && (isOperand(siguiente) || siguiente == '(')) {
                        // Insert concatenation operator
                        salida += '·';
                    }
                }
            }
            // Return the modified regex with explicit concatenation operators
            return salida;
        }
    }

    /**
     * Determines if the given character is an operand (not an operator or
     * parenthesis).
     *
     * @param c Character to evaluate.
     * @return true if it is an operand, false otherwise.
     */
    private static boolean isOperand(char c) {
        // Return true if c is not one of: '|', '*', '?', '+', '(', ')', '·'
        if(c == '|' || c == '*' || c == '?' || c == '+' || c == '(' || c == ')' || c == '·') {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Converts an infix regular expression to postfix notation using the
     * Shunting Yard algorithm. This is useful for constructing NFAs from
     * regular expressions.
     *
     * @param infixRegex Regular expression in infix notation.
     * @return Regular expression in postfix notation.
     */
    public static String toPostfix(String infixRegex) {
        
        // 1. Define operator precedence map
        Map<Character, Integer> precendenciaOp = new HashMap<>();
        precendenciaOp.put('*', 3); 
        precendenciaOp.put('+', 3);  
        precendenciaOp.put('·', 2);
        precendenciaOp.put('?', 3);
        precendenciaOp.put('|', 1);

        // 2. Preprocess regex to insert explicit concatenation operators
        String infixRegexPreprocessed = insertConcatenationOperator(infixRegex);

        // Output regex in postfix notation
        String salida = "";

        // Stack for operators
        Stack<Character> pila_op = new Stack<>();

        /*
            3. For each character in regex:
                - If operand: append to output
                - If '(': push to stack
                - If ')': pop operators to output until '(' is found
                - If operator: pop operators with higher/equal precedence, then push current operator
        */
        for (int i = 0; i < infixRegexPreprocessed.length(); i++) {
            char caracter = infixRegexPreprocessed.charAt(i);

            if (isOperand(caracter) == true) {
                salida += caracter;
            } else if (caracter == '(') {
                pila_op.push(caracter);
            } else if (caracter == ')') {
                while (pila_op.isEmpty() == false && pila_op.peek() != '(') {
                    salida += pila_op.pop();
                }

                if (pila_op.isEmpty() == false) {
                    pila_op.pop();
                }

            } else if (precendenciaOp.containsKey(caracter)) {
                // If operator, pop operators with higher or equal precedence
                while (pila_op.isEmpty() == false && !(pila_op.peek() == '(') && precendenciaOp.containsKey(pila_op.peek()) 
                    && precendenciaOp.get(pila_op.peek()) >= precendenciaOp.get(caracter)) 
                    { 
                        salida += pila_op.pop();
                }

                pila_op.push(caracter);
            }
        }

        // 4. After loop, pop remaining operators to output
        while (pila_op.isEmpty() == false) {
            salida += pila_op.pop();
        }

        // 5. Return output as string
        return salida;
    }     
}

