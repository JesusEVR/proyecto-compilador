package com.compiler.lexer.tokenizer;

/**
 * Enumeration representing different types of tokens that can be recognized by the lexer.
 * Each token type has a descriptive name for debugging and display purposes.
 */
public enum TokenType {
    // Literals
    NUMBER("NUMBER"),
    STRING("STRING"),
    IDENTIFIER("IDENTIFIER"),
    
    // Keywords
    KEYWORD_IF("IF"),
    KEYWORD_ELSE("ELSE"),
    KEYWORD_WHILE("WHILE"),
    KEYWORD_FOR("FOR"),
    KEYWORD_INT("INT"),
    KEYWORD_FLOAT("FLOAT"),
    KEYWORD_BOOLEAN("BOOLEAN"),
    KEYWORD_TRUE("TRUE"),
    KEYWORD_FALSE("FALSE"),
    KEYWORD_RETURN("RETURN"),
    KEYWORD_VOID("VOID"),
    
    // Operators
    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    MODULO("%"),
    ASSIGN("="),
    EQUALS("=="),
    NOT_EQUALS("!="),
    LESS_THAN("<"),
    LESS_EQUAL("<="),
    GREATER_THAN(">"),
    GREATER_EQUAL(">="),
    LOGICAL_AND("&&"),
    LOGICAL_OR("||"),
    LOGICAL_NOT("!"),
    BITWISE_AND("&"),
    BITWISE_OR("|"),
    
    // Delimiters
    LEFT_PAREN("("),
    RIGHT_PAREN(")"),
    LEFT_BRACE("{"),
    RIGHT_BRACE("}"),
    LEFT_BRACKET("["),
    RIGHT_BRACKET("]"),
    SEMICOLON(";"),
    COMMA(","),
    DOT("."),
    
    // Special
    WHITESPACE("WHITESPACE"),
    COMMENT("COMMENT"),
    EOF("EOF"),
    UNKNOWN("UNKNOWN");
    
    private final String displayName;
    
    TokenType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}