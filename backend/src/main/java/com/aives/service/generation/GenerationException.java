package com.aives.service.generation;

/** Safe, user-facing failure. Never include API response bodies or secrets. */
public class GenerationException extends Exception {
    public GenerationException(String message) { super(message); }
    public GenerationException(String message, Throwable cause) { super(message, cause); }
}
