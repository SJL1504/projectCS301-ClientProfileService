package com.example.client.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedAgentAccessException extends RuntimeException {
    public UnauthorizedAgentAccessException(String message) {
        super(message);
    }
}
