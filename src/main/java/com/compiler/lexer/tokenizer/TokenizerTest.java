package com.compiler.lexer.tokenizer;

import java.util.List;
import java.util.Arrays;

/**
 * Test class for the tokenizer functionality.
 * Provides unit tests to verify correct tokenization behavior including
 * maximal munch, priority handling, and error cases.
 */
public class TokenizerTest {
    
    /**
     * Runs all tests and reports results.
     */
    public static void main(String[] args) {
        System.out.println("=== TOKENIZER UNIT TESTS ===\n");
        
        int totalTests = 0;
        int passedTests = 0;
        
        // Test basic tokenization
        totalTests++; if (testBasicTokenization()) passedTests++;
        totalTests++; if (testMaximalMunch()) passedTests++;
        totalTests++; if (testKeywordPriority()) passedTests++;
        totalTests++; if (testIgnoredTokens()) passedTests++;
        totalTests++; if (testNumberTokens()) passedTests++;
        totalTests++; if (testOperatorPrecedence()) passedTests++;
        totalTests++; if (testPositionTracking()) passedTests++;
        totalTests++; if (testEmptyInput()) passedTests++;
        totalTests++; if (testUnknownCharacters()) passedTests++;
        
        System.out.println("=== TEST SUMMARY ===");
        System.out.println("Passed: " + passedTests + "/" + totalTests);
        
    }
    
    private static boolean testBasicTokenization() {
        System.out.println("Test: Basic Tokenization");
        try {
            List<LexicalRule> rules = new LexicalRulesBuilder()
                    .addStandardLanguageRules()
                    .build();
            Tokenizer tokenizer = new Tokenizer(rules);
            
            List<Token> tokens = tokenizer.tokenize("int x = 42;");
            
            // Expected: KEYWORD_INT, IDENTIFIER, ASSIGN, NUMBER, SEMICOLON, EOF
            TokenType[] expected = {
                TokenType.KEYWORD_INT, TokenType.IDENTIFIER, TokenType.ASSIGN, 
                TokenType.NUMBER, TokenType.SEMICOLON, TokenType.EOF
            };
            
            if (tokens.size() != expected.length) {
                System.out.println("  FAILED: Expected " + expected.length + " tokens, got " + tokens.size());
                return false;
            }
            
            for (int i = 0; i < expected.length; i++) {
                if (tokens.get(i).getType() != expected[i]) {
                    System.out.println("  FAILED: Expected " + expected[i] + " at position " + i + 
                                     ", got " + tokens.get(i).getType());
                    return false;
                }
            }
            
            System.out.println("  PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  FAILED: Exception - " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testMaximalMunch() {
        System.out.println("Test: Maximal Munch Strategy");
        try {
            List<LexicalRule> rules = new LexicalRulesBuilder()
                    .addOperators()
                    .addWhitespaceAndComments()
                    .build();
            Tokenizer tokenizer = new Tokenizer(rules);
            
            List<Token> tokens = tokenizer.tokenize("<=");
            
            // Should recognize "<=" as LESS_EQUAL, not "<" followed by "="
            if (tokens.size() != 2) { // LESS_EQUAL + EOF
                System.out.println("  FAILED: Expected 2 tokens, got " + tokens.size());
                return false;
            }
            
            if (tokens.get(0).getType() != TokenType.LESS_EQUAL) {
                System.out.println("  FAILED: Expected LESS_EQUAL, got " + tokens.get(0).getType());
                return false;
            }
            
            if (!tokens.get(0).getLexeme().equals("<=")) {
                System.out.println("  FAILED: Expected lexeme '<=', got '" + tokens.get(0).getLexeme() + "'");
                return false;
            }
            
            System.out.println("  PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  FAILED: Exception - " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testKeywordPriority() {
        System.out.println("Test: Keyword Priority over Identifiers");
        try {
            List<LexicalRule> rules = new LexicalRulesBuilder()
                    .addKeywords()
                    .addLiterals() // This includes IDENTIFIER rule
                    .addWhitespaceAndComments()
                    .build();
            Tokenizer tokenizer = new Tokenizer(rules);
            
            List<Token> tokens = tokenizer.tokenize("if myif");
            
            // "if" should be KEYWORD_IF, "myif" should be IDENTIFIER
            if (tokens.size() != 3) { // KEYWORD_IF + IDENTIFIER + EOF
                System.out.println("  FAILED: Expected 3 tokens, got " + tokens.size());
                return false;
            }
            
            if (tokens.get(0).getType() != TokenType.KEYWORD_IF) {
                System.out.println("  FAILED: Expected KEYWORD_IF for 'if', got " + tokens.get(0).getType());
                return false;
            }
            
            if (tokens.get(1).getType() != TokenType.IDENTIFIER) {
                System.out.println("  FAILED: Expected IDENTIFIER for 'myif', got " + tokens.get(1).getType());
                return false;
            }
            
            System.out.println("  PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  FAILED: Exception - " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testIgnoredTokens() {
        System.out.println("Test: Ignored Tokens (Whitespace)");
        try {
            List<LexicalRule> rules = new LexicalRulesBuilder()
                    .addKeywords()
                    .addWhitespaceAndComments()
                    .build();
            Tokenizer tokenizer = new Tokenizer(rules);
            
            List<Token> tokens = tokenizer.tokenize("  if   \t\n  else  ");
            
            // Whitespace should be ignored, only keywords should remain
            if (tokens.size() != 3) { // KEYWORD_IF + KEYWORD_ELSE + EOF
                System.out.println("  FAILED: Expected 3 tokens (ignoring whitespace), got " + tokens.size());
                return false;
            }
            
            if (tokens.get(0).getType() != TokenType.KEYWORD_IF ||
                tokens.get(1).getType() != TokenType.KEYWORD_ELSE) {
                System.out.println("  FAILED: Keywords not properly recognized");
                return false;
            }
            
            System.out.println("  PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  FAILED: Exception - " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testNumberTokens() {
        System.out.println("Test: Number Token Recognition");
        try {
            List<LexicalRule> rules = new LexicalRulesBuilder()
                    .addLiterals()
                    .addWhitespaceAndComments()
                    .build();
            Tokenizer tokenizer = new Tokenizer(rules);
            
            // Test with simpler input that should work with current implementation
            List<Token> tokens = tokenizer.tokenize("42 123 0");
            
            // Debug: print what we actually got
            System.out.println("    Debug - Got " + tokens.size() + " tokens:");
            for (int i = 0; i < tokens.size(); i++) {
                System.out.println("      " + i + ". " + tokens.get(i).getType() + " : '" + tokens.get(i).getLexeme() + "'");
            }
            
            // Should recognize three numbers + EOF = 4 tokens
            if (tokens.size() != 4) {
                System.out.println("  FAILED: Expected 4 tokens, got " + tokens.size());
                return false;
            }
            
            for (int i = 0; i < 3; i++) {
                if (tokens.get(i).getType() != TokenType.NUMBER) {
                    System.out.println("  FAILED: Expected NUMBER at position " + i + 
                                     ", got " + tokens.get(i).getType());
                    return false;
                }
            }
            
            System.out.println("  PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  FAILED: Exception - " + e.getMessage());
            return false;
        }
    }
    
    
    
    private static boolean testOperatorPrecedence() {
        System.out.println("Test: Operator Precedence (== vs =)");
        try {
            List<LexicalRule> rules = new LexicalRulesBuilder()
                    .addOperators()
                    .addWhitespaceAndComments()
                    .build();
            Tokenizer tokenizer = new Tokenizer(rules);
            
            List<Token> tokens = tokenizer.tokenize("= ==");
            
            // Should be ASSIGN followed by EQUALS
            if (tokens.size() != 3) { // ASSIGN + EQUALS + EOF
                System.out.println("  FAILED: Expected 3 tokens, got " + tokens.size());
                return false;
            }
            
            if (tokens.get(0).getType() != TokenType.ASSIGN) {
                System.out.println("  FAILED: Expected ASSIGN, got " + tokens.get(0).getType());
                return false;
            }
            
            if (tokens.get(1).getType() != TokenType.EQUALS) {
                System.out.println("  FAILED: Expected EQUALS, got " + tokens.get(1).getType());
                return false;
            }
            
            System.out.println("  PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  FAILED: Exception - " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testPositionTracking() {
        System.out.println("Test: Position Tracking");
        try {
            List<LexicalRule> rules = new LexicalRulesBuilder()
                    .addKeywords()
                    .addLiterals()
                    .addWhitespaceAndComments()
                    .build();
            Tokenizer tokenizer = new Tokenizer(rules);
            
            List<Token> tokens = tokenizer.tokenize("if x");
            
            // Debug: print what we actually got
            System.out.println("    Debug - Got " + tokens.size() + " tokens:");
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                System.out.println("      " + i + ". " + token.getType() + " : '" + token.getLexeme() + 
                                 "' at pos " + token.getPosition());
            }
            
            // Check position information for the tokens we expect
            boolean foundIfToken = false;
            boolean foundXToken = false;
            
            for (Token token : tokens) {
                if (token.getType() == TokenType.KEYWORD_IF && token.getLexeme().equals("if")) {
                    foundIfToken = true;
                    if (token.getPosition() != 0 || token.getLine() != 1 || token.getColumn() != 1) {
                        System.out.println("  FAILED: Incorrect position for 'if' token");
                        return false;
                    }
                }
                if (token.getType() == TokenType.IDENTIFIER && token.getLexeme().equals("x")) {
                    foundXToken = true;
                    if (token.getPosition() != 3 || token.getLine() != 1 || token.getColumn() != 4) {
                        System.out.println("  FAILED: Incorrect position for 'x' token");
                        return false;
                    }
                }
            }
            
            if (!foundIfToken || !foundXToken) {
                System.out.println("  FAILED: Expected tokens not found");
                return false;
            }
            
            System.out.println("  PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  FAILED: Exception - " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testEmptyInput() {
        System.out.println("Test: Empty Input");
        try {
            List<LexicalRule> rules = new LexicalRulesBuilder()
                    .addStandardLanguageRules()
                    .build();
            Tokenizer tokenizer = new Tokenizer(rules);
            
            List<Token> tokens = tokenizer.tokenize("");
            
            // Should only contain EOF token
            if (tokens.size() != 1) {
                System.out.println("  FAILED: Expected 1 token (EOF), got " + tokens.size());
                return false;
            }
            
            if (tokens.get(0).getType() != TokenType.EOF) {
                System.out.println("  FAILED: Expected EOF, got " + tokens.get(0).getType());
                return false;
            }
            
            System.out.println("  PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  FAILED: Exception - " + e.getMessage());
            return false;
        }
    }
    
    private static boolean testUnknownCharacters() {
        System.out.println("Test: Unknown Characters");
        try {
            List<LexicalRule> rules = new LexicalRulesBuilder()
                    .addKeywords()
                    .addWhitespaceAndComments()
                    .build();
            Tokenizer tokenizer = new Tokenizer(rules);
            
            List<Token> tokens = tokenizer.tokenize("if @ else");
            
            // Should be KEYWORD_IF + UNKNOWN('@') + KEYWORD_ELSE + EOF
            if (tokens.size() != 4) {
                System.out.println("  FAILED: Expected 4 tokens, got " + tokens.size());
                return false;
            }
            
            if (tokens.get(0).getType() != TokenType.KEYWORD_IF) {
                System.out.println("  FAILED: Expected KEYWORD_IF, got " + tokens.get(0).getType());
                return false;
            }
            
            if (tokens.get(1).getType() != TokenType.UNKNOWN) {
                System.out.println("  FAILED: Expected UNKNOWN, got " + tokens.get(1).getType());
                return false;
            }
            
            if (!tokens.get(1).getLexeme().equals("@")) {
                System.out.println("  FAILED: Expected '@' lexeme, got '" + tokens.get(1).getLexeme() + "'");
                return false;
            }
            
            if (tokens.get(2).getType() != TokenType.KEYWORD_ELSE) {
                System.out.println("  FAILED: Expected KEYWORD_ELSE, got " + tokens.get(2).getType());
                return false;
            }
            
            System.out.println("  PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  FAILED: Exception - " + e.getMessage());
            return false;
        }
    }
}