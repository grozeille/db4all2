package fr.grozeille.db4all.api.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    private final String message;
    private final String errorType;

    public ErrorResponse(String message) {
        this.message = message;
        this.errorType = null;
    }
    public ErrorResponse(String message, String errorType) {
        this.message = message;
        this.errorType = errorType;
    }
}
