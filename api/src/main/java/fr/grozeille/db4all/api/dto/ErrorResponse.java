package fr.grozeille.db4all.api.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    public static final String PASSWORD_TOO_WEAK = "PASSWORD_TOO_WEAK";
    public static final String WRONG_PASSWORD = "WRONG_PASSWORD";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String SELF_STATUS_CHANGE_FORBIDDEN = "SELF_STATUS_CHANGE_FORBIDDEN";
    public static final String PROJECT_NAME_ALREADY_USED = "PROJECT_NAME_ALREADY_USED";
    public static final String PROJECT_ADMIN_ALREADY_EXISTS = "PROJECT_ADMIN_ALREADY_EXISTS";
    public static final String PROJECT_ADMIN_SELF_REMOVAL_FORBIDDEN = "PROJECT_ADMIN_SELF_REMOVAL_FORBIDDEN";
    public static final String PROJECT_LAST_ADMIN_REMOVAL_FORBIDDEN = "PROJECT_LAST_ADMIN_REMOVAL_FORBIDDEN";
    public static final String PROJECT_ACCESS_FORBIDDEN = "PROJECT_ACCESS_FORBIDDEN";
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
