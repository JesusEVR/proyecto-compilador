package com.compiler.lexer.tokenizer;

/**
 * Represents a lexical rule that pairs a regular expression pattern with a token type.
 * Rules have a priority order - rules defined earlier have higher priority.
 */
public class LexicalRule {
    private final String pattern;
    private final TokenType tokenType;
    private final int priority;
    private final boolean ignore;

    /**
     * Creates a new lexical rule with the specified priority.
     * 
     * @param pattern The regular expression pattern
     * @param tokenType The token type to produce when this pattern matches
     * @param priority The priority of this rule (lower values = higher priority)
     * @param ignore Whether tokens matching this rule should be ignored (not emitted)
     */
    public LexicalRule(String pattern, TokenType tokenType, int priority, boolean ignore) {
        this.pattern = pattern;
        this.tokenType = tokenType;
        this.priority = priority;
        this.ignore = ignore;
    }

    /**
     * Creates a new lexical rule that is not ignored.
     * 
     * @param pattern The regular expression pattern
     * @param tokenType The token type to produce when this pattern matches
     * @param priority The priority of this rule (lower values = higher priority)
     */
    public LexicalRule(String pattern, TokenType tokenType, int priority) {
        this(pattern, tokenType, priority, false);
    }

    public String getPattern() {
        return pattern;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isIgnore() {
        return ignore;
    }

    @Override
    public String toString() {
        return String.format("LexicalRule{pattern='%s', tokenType=%s, priority=%d, ignore=%s}",
                           pattern, tokenType, priority, ignore);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LexicalRule that = (LexicalRule) obj;
        return priority == that.priority &&
               ignore == that.ignore &&
               pattern.equals(that.pattern) &&
               tokenType == that.tokenType;
    }

    @Override
    public int hashCode() {
        int result = pattern.hashCode();
        result = 31 * result + tokenType.hashCode();
        result = 31 * result + priority;
        result = 31 * result + (ignore ? 1 : 0);
        return result;
    }
}