package com.compiler.lexer.tokenizer;

/**
 * Represents a single token produced by the lexical analyzer.
 * Contains the token type, lexeme (text), and position information for error reporting.
 */
public class Token {
    private final TokenType type;
    private final String lexeme;
    private final int position;
    private final int line;
    private final int column;

    /**
     * Constructs a new token with position information.
     * 
     * @param type The type of this token
     * @param lexeme The actual text of the token
     * @param position The absolute position in the input
     * @param line The line number (1-based)
     * @param column The column number (1-based)
     */
    public Token(TokenType type, String lexeme, int position, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.position = position;
        this.line = line;
        this.column = column;
    }

    /**
     * Simple constructor without position information.
     * 
     * @param type The type of this token
     * @param lexeme The actual text of the token
     */
    public Token(TokenType type, String lexeme) {
        this(type, lexeme, -1, -1, -1);
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getPosition() {
        return position;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        if (position >= 0) {
            return String.format("Token{type=%s, lexeme='%s', pos=%d, line=%d, col=%d}", 
                               type, lexeme, position, line, column);
        } else {
            return String.format("Token{type=%s, lexeme='%s'}", type, lexeme);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Token token = (Token) obj;
        return position == token.position &&
               line == token.line &&
               column == token.column &&
               type == token.type &&
               lexeme.equals(token.lexeme);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + lexeme.hashCode();
        result = 31 * result + position;
        result = 31 * result + line;
        result = 31 * result + column;
        return result;
    }
}