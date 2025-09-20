package com.taggingsystem;

public class TaggingException extends RuntimeException {
    public TaggingException(String message) {
        super(message);
    }

    public TaggingException(String message, Throwable cause) {
        super(message, cause);
    }
}