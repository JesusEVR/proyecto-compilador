package com.compiler.lexer.tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder class for creating common sets of lexical rules.
 * Provides convenient methods to build rule sets for typical programming language tokens.
 */
public class LexicalRulesBuilder {
    private final List<LexicalRule> rules;
    private int currentPriority;

    /**
     * Creates a new builder with empty rule set.
     */
    public LexicalRulesBuilder() {
        this.rules = new ArrayList<>();
        this.currentPriority = 0;
    }

    /**
     * Adds a lexical rule with the current priority and increments the priority counter.
     */
    public LexicalRulesBuilder addRule(String pattern, TokenType tokenType, boolean ignore) {
        rules.add(new LexicalRule(pattern, tokenType, currentPriority++, ignore));
        return this;
    }

    /**
     * Adds a lexical rule that is not ignored.
     */
    public LexicalRulesBuilder addRule(String pattern, TokenType tokenType) {
        return addRule(pattern, tokenType, false);
    }

    /**
     * Adds an ignored rule (typically for whitespace or comments).
     */
    public LexicalRulesBuilder addIgnoredRule(String pattern, TokenType tokenType) {
        return addRule(pattern, tokenType, true);
    }

    /**
     * Adds common keyword rules. Keywords must be added before identifier rules
     * to ensure proper priority handling.
     */
   public LexicalRulesBuilder addKeywords() {
        addRule("if", TokenType.KEYWORD_IF);
        addRule("else", TokenType.KEYWORD_ELSE);
        addRule("while", TokenType.KEYWORD_WHILE);
        addRule("for", TokenType.KEYWORD_FOR);
        addRule("int", TokenType.KEYWORD_INT);
        addRule("float", TokenType.KEYWORD_FLOAT);
        addRule("boolean", TokenType.KEYWORD_BOOLEAN);
        addRule("true", TokenType.KEYWORD_TRUE);
        addRule("false", TokenType.KEYWORD_FALSE);
        addRule("return", TokenType.KEYWORD_RETURN);
        addRule("void", TokenType.KEYWORD_VOID);
        return this;
    }

    /**
     * Adds common operator rules.
     */
    public LexicalRulesBuilder addOperators() {
        addRule("==", TokenType.EQUALS);
        addRule("!=", TokenType.NOT_EQUALS);
        addRule("<=", TokenType.LESS_EQUAL);
        addRule(">=", TokenType.GREATER_EQUAL);
        addRule("&&", TokenType.LOGICAL_AND);
        addRule("\\|\\|", TokenType.LOGICAL_OR);
        addRule("\\+", TokenType.PLUS);
        addRule("-", TokenType.MINUS);
        addRule("\\*", TokenType.MULTIPLY);
        addRule("/", TokenType.DIVIDE);
        addRule("%", TokenType.MODULO);
        addRule("=", TokenType.ASSIGN);
        addRule("<", TokenType.LESS_THAN);
        addRule(">", TokenType.GREATER_THAN);
        addRule("!", TokenType.LOGICAL_NOT);
        addRule("&", TokenType.BITWISE_AND);
        addRule("\\|", TokenType.BITWISE_OR);
        return this;
    }

    public LexicalRulesBuilder addDelimiters() {
        addRule("\\(", TokenType.LEFT_PAREN);
        addRule("\\)", TokenType.RIGHT_PAREN);
        addRule("\\{", TokenType.LEFT_BRACE);
        addRule("\\}", TokenType.RIGHT_BRACE);
        addRule("\\[", TokenType.LEFT_BRACKET);
        addRule("\\]", TokenType.RIGHT_BRACKET);
        addRule(";", TokenType.SEMICOLON);
        addRule(",", TokenType.COMMA);
        addRule("\\.", TokenType.DOT);
        return this;
    }

    /**
     * Adds common literal rules (numbers, identifiers, strings).
     * Using simplified patterns that work with the current regex parser.
     */
    public LexicalRulesBuilder addLiterals() {
        // Use simple patterns that work well with Thompson construction
        addRule("[0-9]+", TokenType.NUMBER);
        addRule("[a-zA-Z_][a-zA-Z0-9_]*", TokenType.IDENTIFIER);
        addRule("\"[^\"]*\"", TokenType.STRING);
        return this;
    }

    public LexicalRulesBuilder addWhitespaceAndComments() {
        addIgnoredRule("\\s+", TokenType.WHITESPACE);
        addIgnoredRule("//.*", TokenType.COMMENT);
        addIgnoredRule("/\\*.*?\\*/", TokenType.COMMENT);
        return this;
    }

    public LexicalRulesBuilder addStandardLanguageRules() {
        return addWhitespaceAndComments()
               .addKeywords()
               .addOperators()
               .addDelimiters()
               .addLiterals();
    }

    /**
     * Gets the current priority value (useful for manual rule additions).
     */
    public int getCurrentPriority() {
        return currentPriority;
    }

    /**
     * Sets the current priority value.
     */
    public LexicalRulesBuilder setPriority(int priority) {
        this.currentPriority = priority;
        return this;
    }

    /**
     * Builds and returns the list of lexical rules.
     */
    public List<LexicalRule> build() {
        return new ArrayList<>(rules);
    }

    /**
     * Gets the number of rules currently in the builder.
     */
    public int size() {
        return rules.size();
    }

    /**
     * Clears all rules from the builder and resets priority to 0.
     */
    public LexicalRulesBuilder clear() {
        rules.clear();
        currentPriority = 0;
        return this;
    }
}