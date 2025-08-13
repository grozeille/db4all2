package fr.grozeille.db4all.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WrongPasswordException extends IllegalArgumentException {
    public WrongPasswordException() {
        super("The password is wrong");
    }
}
