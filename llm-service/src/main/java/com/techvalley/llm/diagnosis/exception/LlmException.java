package com.techvalley.llm.diagnosis.exception;

/** Ném ra khi CẢ provider thật (Gemini) LẪN Mock provider đều thất bại (trường hợp hiếm). */
public class LlmException extends RuntimeException {
    public LlmException(String message) {
        super(message);
    }
    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
