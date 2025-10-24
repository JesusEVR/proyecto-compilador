package com.compiler.parser.lr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.compiler.lexer.Token;
import com.compiler.parser.grammar.SymbolType;
import com.compiler.parser.grammar.Symbol;
import com.compiler.parser.grammar.Production;

/**
 * Implements the LALR(1) parsing engine.
 * Uses a stack and the LALR(1) table to process a sequence of tokens.
 * Complementary task for Practice 9.
 */
public class LALR1Parser {
    private final LALR1Table table;

    public LALR1Parser(LALR1Table table) {
        this.table = table;
    }

    // package-private accessor for tests
    LALR1Table getTable() {
        return table;
    }

    /**
     * Parses a sequence of tokens using the LALR(1) parsing algorithm.
     * @param tokens The list of tokens from the lexer.
     * @return true if the sequence is accepted, false if a syntax error is found.
     */
    public boolean parse(List<Token> tokens) {
        // TODO: Implement the LALR(1) parsing algorithm.
        // Initialize a stack for states and push the initial state (from table.getInitialState()).
        Stack<Integer> stateStack = new Stack<>();
        stateStack.push(table.getInitialState());
        
        // Create a mutable list of input tokens from the parameter and add the end-of-input token ("$").
        List<Token> input = new ArrayList<>(tokens);
        input.add(new Token("$", "$")); // End-of-input token
        
        // Initialize an instruction pointer `ip` to 0, pointing to the first token.
        int ip = 0;
        
        Map<Integer, Map<Symbol, LALR1Table.Action>> actionTable = table.getActionTable();
        Map<Integer, Map<Symbol, Integer>> gotoTable = table.getGotoTable();
        
        // 4. Start a loop that runs until an ACCEPT or ERROR condition is met.
        while (true) {
            // a. Get current state from top of stack
            int currentState = stateStack.peek();
            
            // b. Get current token
            if (ip >= input.size()) {
                // Unexpected end of input
                return false;
            }
            Token currentToken = input.get(ip);
            
            // Convert token type to Symbol for table lookup
            Symbol tokenSymbol = new Symbol(currentToken.type, SymbolType.TERMINAL);
            
            // c. Look up action in ACTION table
            LALR1Table.Action action = null;
            Map<Symbol, LALR1Table.Action> stateActions = actionTable.get(currentState);
            if (stateActions != null) {
                action = stateActions.get(tokenSymbol);
            }
            
            // d. If no action found, it's a syntax error
            if (action == null) {
                return false;
            }
            
            // e. Process action based on type
            switch (action.type) {
                case SHIFT:
                    // i. Push new state onto stack
                    stateStack.push(action.state);
                    // ii. Advance input pointer
                    ip++;
                    break;
                // f. If the action is REDUCE(A -> β):
                case REDUCE:
                    // i. Pop |β| states from stack (where β is the RHS of production)
                    int rhsLength = action.reduceProd.right.size();
                    
                    // Handle epsilon productions (empty RHS)
                    if (rhsLength == 1 && action.reduceProd.right.get(0).name.equals("ε")) {
                        rhsLength = 0;
                    }
                    
                    for (int i = 0; i < rhsLength; i++) {
                        if (stateStack.isEmpty()) {
                            return false;
                        }
                        stateStack.pop();
                    }
                    
                    // ii. Get new state from top of stack
                    if (stateStack.isEmpty()) {
                        return false;
                    }
                    int stateAfterPop = stateStack.peek();
                    
                    // iii. Look up GOTO state
                    Symbol lhs = action.reduceProd.left;
                    Integer gotoState = null;
                    Map<Symbol, Integer> stateGotos = gotoTable.get(stateAfterPop);
                    if (stateGotos != null) {
                        gotoState = stateGotos.get(lhs);
                    }
                    
                    // iv. If no GOTO state found, it's an error
                    if (gotoState == null) {
                        return false;
                    }
                    
                    // v. Push GOTO state onto stack
                    stateStack.push(gotoState);
                    break;
                //g. If the action is ACCEPT:
                case ACCEPT:
                    // i. Input successfully parsed
                    return true;
                // h. If the action is none of the above, it's an unhandled case or error. Return false.
                default:
                    return false;
            }
        }
    }
}