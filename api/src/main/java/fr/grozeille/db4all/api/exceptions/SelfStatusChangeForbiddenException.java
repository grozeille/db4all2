package fr.grozeille.db4all.api.exceptions;

public class SelfStatusChangeForbiddenException extends RuntimeException {
    public SelfStatusChangeForbiddenException(String message) {
        super(message);
    }
}
