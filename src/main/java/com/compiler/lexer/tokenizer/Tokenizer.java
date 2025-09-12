package com.compiler.lexer.tokenizer;

import com.compiler.lexer.*;
import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;
import com.compiler.lexer.nfa.Transition;
import com.compiler.lexer.regex.RegexParser;

import java.util.*;

/**
 * Tokenizer
 * ---------
 * Main tokenizer class that combines multiple lexical rules into a single DFA
 * and applies the maximal munch strategy to tokenize input strings.
 */
public class Tokenizer {
    private final List<LexicalRule> rules;
    private DFA combinedDfa;
    private final RegexParser regexParser;

    /**
     * Creates a new tokenizer with the specified lexical rules.
     */
    public Tokenizer(List<LexicalRule> rules) {
        this.rules = new ArrayList<>(rules);
        // Sort rules by priority to ensure proper precedence
        this.rules.sort(Comparator.comparingInt(LexicalRule::getPriority));
        this.regexParser = new RegexParser();
        buildCombinedDfa();
    }

    /**
     * Tokenizes the input string using the maximal munch strategy.
     */
    public List<Token> tokenize(String input) throws TokenizerException {
        List<Token> tokens = new ArrayList<>();
        int position = 0;
        int line = 1;
        int column = 1;
        
        while (position < input.length()) {
            TokenMatch match = findLongestMatch(input, position, line, column);
            
            if (match == null) {
                // No match found - create an unknown token for single character
                String lexeme = String.valueOf(input.charAt(position));
                tokens.add(new Token(TokenType.UNKNOWN, lexeme, position, line, column));
                position++;
                column++;
            } else {
                // Create token if not ignored
                if (!match.isIgnore()) {
                    Token token = new Token(match.getTokenType(), match.getLexeme(), 
                                          position, line, column);
                    tokens.add(token);
                }
                
                // Update position and line/column tracking
                String lexeme = match.getLexeme();
                for (char c : lexeme.toCharArray()) {
                    if (c == '\n') {
                        line++;
                        column = 1;
                    } else {
                        column++;
                    }
                }
                position += lexeme.length();
            }
        }
        
        // Add EOF token
        tokens.add(new Token(TokenType.EOF, "", position, line, column));
        return tokens;
    }

    /**
     * Finds the longest matching token starting at the specified position.
     * Implements maximal munch strategy with priority-based conflict resolution.
     */
    private TokenMatch findLongestMatch(String input, int startPos, int line, int column) {
        if (startPos >= input.length()) {
            return null;
        }

        DfaState currentState = combinedDfa.getStartState();
        TokenMatch bestMatch = null;
        int currentPos = startPos;
        
        // Simulate the DFA to find the longest match
        while (currentPos < input.length() && currentState != null) {
            char currentChar = input.charAt(currentPos);
            DfaState nextState = currentState.getTransition(currentChar);
            
            if (nextState == null) {
                break;
            }
            
            currentState = nextState;
            currentPos++;
            
            // Check if current state is final and has a token type
            if (currentState.isFinal() && currentState.getTokenType() != null) {
                String lexeme = input.substring(startPos, currentPos);
                TokenType tokenType = currentState.getTokenType();
                int priority = currentState.getTokenPriority();
                boolean ignore = isIgnoredToken(tokenType);
                
                // Update best match - prefer longer matches or better priority
                if (bestMatch == null || 
                    lexeme.length() > bestMatch.getLexeme().length() ||
                    (lexeme.length() == bestMatch.getLexeme().length() && 
                     priority < bestMatch.getPriority())) {
                    
                    bestMatch = new TokenMatch(tokenType, lexeme, priority, ignore);
                }
            }
        }
        
        return bestMatch;
    }

    /**
     * Checks if a token type should be ignored based on the lexical rules.
     */
    private boolean isIgnoredToken(TokenType tokenType) {
        for (LexicalRule rule : rules) {
            if (rule.getTokenType() == tokenType) {
                return rule.isIgnore();
            }
        }
        return false;
    }

    /**
     * Builds a combined DFA from all lexical rules using Thompson construction
     * and the subset construction algorithm.
     */
    private void buildCombinedDfa() {
        try {
            if (rules.isEmpty()) {
                // Create empty DFA if no rules
                State start = new State();
                State end = new State();
                start.transitions.add(new Transition(null, end));
                NFA emptyNfa = new NFA(start, end);
                Set<Character> emptyAlphabet = new HashSet<>();
                this.combinedDfa = NfaToDfaConverter.convertNfaToDfa(emptyNfa, emptyAlphabet);
                return;
            }
            
            // Create a common start state that connects to all rule NFAs
            State commonStart = new State();
            List<NFA> individualNfas = new ArrayList<>();
            
            // Build individual NFAs for each lexical rule
            for (LexicalRule rule : rules) {
                try {
                    NFA ruleNfa = regexParser.parse(rule.getPattern());
                    
                    // Set token information on the end state
                    ruleNfa.endState.setTokenType(rule.getTokenType(), rule.getPriority());
                    
                    // Connect common start to this NFA's start with epsilon transition
                    commonStart.transitions.add(new Transition(null, ruleNfa.startState));
                    
                    individualNfas.add(ruleNfa);
                    
                } catch (Exception e) {
                    System.err.println("Warning: Failed to parse pattern '" + rule.getPattern() + "': " + e.getMessage());
                    // Try to create a simple literal NFA for failed patterns
                    NFA literalNfa = createSimpleLiteralNfa(rule.getPattern(), rule.getTokenType(), rule.getPriority());
                    if (literalNfa != null) {
                        commonStart.transitions.add(new Transition(null, literalNfa.startState));
                        individualNfas.add(literalNfa);
                    }
                }
            }
            
            // Create the combined NFA with dummy end state
            State combinedEnd = new State();
            NFA combinedNfa = new NFA(commonStart, combinedEnd);
            
            // Extract comprehensive alphabet from all patterns
            Set<Character> alphabet = extractAlphabet();
            
            // Convert NFA to DFA using subset construction
            this.combinedDfa = NfaToDfaConverter.convertNfaToDfa(combinedNfa, alphabet);
            
            // Update DFA states with token information from constituent NFA states
            updateDfaTokenInformation();
            
            // Minimize the DFA to reduce state count while preserving functionality
            this.combinedDfa = DfaMinimizer.minimizeDfa(this.combinedDfa, alphabet);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to build combined DFA: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates a simple literal NFA for patterns that can't be parsed as regex.
     * This serves as a fallback for complex or malformed patterns.
     */
    private NFA createSimpleLiteralNfa(String pattern, TokenType tokenType, int priority) {
        try {
            if (pattern.length() == 1) {
                State start = new State();
                State end = new State();
                end.setTokenType(tokenType, priority);
                start.transitions.add(new Transition(pattern.charAt(0), end));
                return new NFA(start, end);
            }
            
            // For multi-character patterns, create concatenation chain
            if (pattern.length() > 1) {
                State start = new State();
                State current = start;
                
                for (int i = 0; i < pattern.length() - 1; i++) {
                    State next = new State();
                    current.transitions.add(new Transition(pattern.charAt(i), next));
                    current = next;
                }
                
                State end = new State();
                end.setTokenType(tokenType, priority);
                current.transitions.add(new Transition(pattern.charAt(pattern.length() - 1), end));
                
                return new NFA(start, end);
            }
        } catch (Exception e) {
            System.err.println("Failed to create literal NFA for pattern: " + pattern);
        }
        return null;
    }

    /**
     * Updates DFA states with token type information from their constituent NFA states.
     * Ensures the best priority token type is preserved and handles conflicts properly.
     */
    private void updateDfaTokenInformation() {
        for (DfaState dfaState : combinedDfa.getAllStates()) {
            TokenType bestTokenType = null;
            int bestPriority = Integer.MAX_VALUE;
            boolean hasFinalState = false;
            
            // Check all NFA states that make up this DFA state
            for (State nfaState : dfaState.getName()) {
                if (nfaState.isFinal() && nfaState.getTokenType() != null) {
                    hasFinalState = true;
                    // Choose the token type with the best (lowest) priority
                    if (nfaState.getTokenPriority() < bestPriority) {
                        bestPriority = nfaState.getTokenPriority();
                        bestTokenType = nfaState.getTokenType();
                    }
                }
            }
            
            if (hasFinalState && bestTokenType != null) {
                dfaState.setFinal(true);
                dfaState.setTokenType(bestTokenType, bestPriority);
            }
        }
    }

    /**
     * Extracts the complete alphabet (set of all characters) used in the lexical rules.
     * Includes all printable ASCII, whitespace, and commonly used characters.
     */
    private Set<Character> extractAlphabet() {
        Set<Character> alphabet = new HashSet<>();
        
        // Add ASCII printable characters (space to tilde)
        for (char c = 32; c <= 126; c++) {
            alphabet.add(c);
        }
        
        // Add essential whitespace characters
        alphabet.add(' ');   // space
        alphabet.add('\t');  // tab
        alphabet.add('\n');  // newline
        alphabet.add('\r');  // carriage return
        
        // Explicitly add digits for number recognition
        for (char c = '0'; c <= '9'; c++) {
            alphabet.add(c);
        }
        
        // Explicitly add letters for identifier recognition
        for (char c = 'a'; c <= 'z'; c++) {
            alphabet.add(c);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            alphabet.add(c);
        }
        
        // Add underscore for identifiers
        alphabet.add('_');
        
        // Add common punctuation and operators
        char[] commonChars = {'!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', 
                             '=', '[', ']', '{', '}', '\\', '|', ';', ':', '"', '\'', 
                             '<', '>', ',', '.', '?', '/', '~', '`'};
        for (char c : commonChars) {
            alphabet.add(c);
        }
        
        // Add characters found explicitly in patterns (for safety)
        for (LexicalRule rule : rules) {
            String pattern = rule.getPattern();
            for (char c : pattern.toCharArray()) {
                // Add literal characters, excluding regex metacharacters
                if (c != '\\' && c != '(' && c != ')' && c != '[' && c != ']' && 
                    c != '{' && c != '}' && c != '*' && c != '+' && c != '?' && 
                    c != '|' && c != '.' && c != '^' && c != '$') {
                    alphabet.add(c);
                }
            }
        }
        
        return alphabet;
    }

    /**
     * Gets the combined DFA used by this tokenizer.
     * @return The internal DFA used for tokenization.
     */
    public DFA getCombinedDfa() {
        return combinedDfa;
    }

    /**
     * Gets the lexical rules used by this tokenizer.
     * @return A copy of the lexical rules list.
     */
    public List<LexicalRule> getRules() {
        return new ArrayList<>(rules);
    }

    /**
     * Gets the number of rules in this tokenizer.
     * @return The count of lexical rules.
     */
    public int getRuleCount() {
        return rules.size();
    }

    /**
     * Checks if the tokenizer has any rules defined.
     * @return true if there are rules, false otherwise.
     */
    public boolean hasRules() {
        return !rules.isEmpty();
    }

    /**
     * Gets information about the tokenizer's current state.
     * Useful for debugging and introspection.
     */
    public String getTokenizerInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Tokenizer Information:\n");
        info.append("- Rules count: ").append(rules.size()).append("\n");
        info.append("- DFA states: ").append(combinedDfa != null ? combinedDfa.getAllStates().size() : 0).append("\n");
        info.append("- Rules (by priority):\n");
        
        for (int i = 0; i < Math.min(rules.size(), 10); i++) {
            LexicalRule rule = rules.get(i);
            info.append(String.format("  %d. %s: '%s' %s\n", 
                       rule.getPriority(), 
                       rule.getTokenType(), 
                       rule.getPattern(),
                       rule.isIgnore() ? "(ignored)" : ""));
        }
        
        if (rules.size() > 10) {
            info.append("  ... and ").append(rules.size() - 10).append(" more rules\n");
        }
        
        return info.toString();
    }

    /**
     * Inner class to represent a token match during tokenization.
     * Encapsulates all information about a potential token match.
     */
    private static class TokenMatch {
        private final TokenType tokenType;
        private final String lexeme;
        private final int priority;
        private final boolean ignore;

        /**
         * Creates a new token match.
         * @param tokenType The type of token matched.
         * @param lexeme The actual text that was matched.
         * @param priority The priority of the matching rule.
         * @param ignore Whether this token should be ignored in output.
         */
        public TokenMatch(TokenType tokenType, String lexeme, int priority, boolean ignore) {
            this.tokenType = tokenType;
            this.lexeme = lexeme;
            this.priority = priority;
            this.ignore = ignore;
        }

        public TokenType getTokenType() { 
            return tokenType; 
        }
        
        public String getLexeme() { 
            return lexeme; 
        }
        
        public int getPriority() { 
            return priority; 
        }
        
        public boolean isIgnore() { 
            return ignore; 
        }

        @Override
        public String toString() {
            return String.format("TokenMatch{type=%s, lexeme='%s', priority=%d, ignore=%s}",
                               tokenType, lexeme, priority, ignore);
        }
    }
}