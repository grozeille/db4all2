package fr.grozeille.db4all.api.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    public static final String PASSWORD_TOO_WEAK = "PASSWORD_TOO_WEAK";
    public static final String UNDEFINED = "UNDEFINED";

    private final String message;
    private final String errorType;

    public ErrorResponse(String message) {
        this.message = message;
        this.errorType = UNDEFINED;
    }

    public ErrorResponse(String message, String errorType) {
        this.message = message;
        this.errorType = errorType;
    }
}
