package fr.grozeille.db4all.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends IllegalArgumentException {
    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
    }
}
