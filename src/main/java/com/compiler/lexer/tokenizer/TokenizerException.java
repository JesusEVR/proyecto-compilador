package com.compiler.lexer.tokenizer;

/**
 * Exception thrown when the tokenizer encounters an error during tokenization.
 * This includes cases where invalid characters or token sequences are encountered.
 */
public class TokenizerException extends Exception {
    private final int position;
    private final int line;
    private final int column;

    /**
     * Constructs a tokenizer exception with position information.
     * 
     * @param message Error message
     * @param position Position in input where error occurred
     * @param line Line number where error occurred
     * @param column Column number where error occurred
     */
    public TokenizerException(String message, int position, int line, int column) {
        super(String.format("%s at position %d (line %d, column %d)", message, position, line, column));
        this.position = position;
        this.line = line;
        this.column = column;
    }

    /**
     * Constructs a tokenizer exception with just a message.
     * 
     * @param message Error message
     */
    public TokenizerException(String message) {
        super(message);
        this.position = -1;
        this.line = -1;
        this.column = -1;
    }

    /**
     * Constructs a tokenizer exception with a message and cause.
     * 
     * @param message Error message
     * @param cause The underlying cause
     */
    public TokenizerException(String message, Throwable cause) {
        super(message, cause);
        this.position = -1;
        this.line = -1;
        this.column = -1;
    }

    public int getPosition() { return position; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
}