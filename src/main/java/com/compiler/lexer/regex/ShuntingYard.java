package com.compiler.lexer.regex;

import java.util.Stack;
import java.util.HashMap;
import java.util.Map;

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
     * expression according to standard rules.
     */
    public static String insertConcatenationOperator(String regex) {
        if (regex == null || regex.isEmpty()) {
            return regex;
        }

        // For simple single character patterns, no concatenation needed
        if (regex.length() == 1) {
            return regex;
        }

        String salida = "";

        for (int i = 0; i < regex.length(); i++) {
            char caracActual = regex.charAt(i);
            salida += caracActual;
            
            if (i < regex.length() - 1) {
                char siguiente = regex.charAt(i + 1);
                
                // Check if we need to insert concatenation operator
                if ((isOperand(caracActual) || caracActual == ')' || caracActual == '*' || caracActual == '+' 
                    || caracActual == '?') && (isOperand(siguiente) || siguiente == '(')) {
                    salida += '·';
                }
            }
        }
        
        return salida;
    }

    /**
     * Determines if the given character is an operand (not an operator or
     * parenthesis).
     */
    private static boolean isOperand(char c) {
        return !(c == '|' || c == '*' || c == '?' || c == '+' || c == '(' || c == ')' || c == '·');
    }

    /**
     * Converts an infix regular expression to postfix notation using the
     * Shunting Yard algorithm.
     */
    public static String toPostfix(String infixRegex) {
        // Handle simple cases
        if (infixRegex == null || infixRegex.isEmpty()) {
            return infixRegex;
        }
        
        if (infixRegex.length() == 1 && isOperand(infixRegex.charAt(0))) {
            return infixRegex;
        }
        
        try {
            // Define operator precedence map
            Map<Character, Integer> precendenciaOp = new HashMap<>();
            precendenciaOp.put('*', 3); 
            precendenciaOp.put('+', 3);  
            precendenciaOp.put('?', 3);
            precendenciaOp.put('·', 2);
            precendenciaOp.put('|', 1);

            // Preprocess regex to insert explicit concatenation operators
            String infixRegexPreprocessed = insertConcatenationOperator(infixRegex);

            String salida = "";
            Stack<Character> pila_op = new Stack<>();

            for (int i = 0; i < infixRegexPreprocessed.length(); i++) {
                char caracter = infixRegexPreprocessed.charAt(i);

                if (isOperand(caracter)) {
                    salida += caracter;
                } else if (caracter == '(') {
                    pila_op.push(caracter);
                } else if (caracter == ')') {
                    while (!pila_op.isEmpty() && pila_op.peek() != '(') {
                        salida += pila_op.pop();
                    }

                    if (!pila_op.isEmpty()) {
                        pila_op.pop(); // Remove '('
                    }

                } else if (precendenciaOp.containsKey(caracter)) {
                    while (!pila_op.isEmpty() && pila_op.peek() != '(' && 
                           precendenciaOp.containsKey(pila_op.peek()) && 
                           precendenciaOp.get(pila_op.peek()) >= precendenciaOp.get(caracter)) { 
                        salida += pila_op.pop();
                    }
                    pila_op.push(caracter);
                }
            }

            // Pop remaining operators
            while (!pila_op.isEmpty()) {
                salida += pila_op.pop();
            }

            return salida;
        } catch (Exception e) {
            // If conversion fails, return the original regex for literal interpretation
            return infixRegex;
        }
    }     
}