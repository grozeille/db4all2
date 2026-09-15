package fr.grozeille.db4all.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ViewNotFoundException extends IllegalArgumentException {

    public ViewNotFoundException(String viewId) {
        super("View not found: " + viewId);
    }
}