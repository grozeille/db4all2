package fr.grozeille.db4all.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidQueryException extends IllegalArgumentException {

    public InvalidQueryException(String message) {
        super(message);
    }
}