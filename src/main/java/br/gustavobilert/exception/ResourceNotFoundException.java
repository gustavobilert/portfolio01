package br.gustavobilert.exception;

import javax.ws.rs.core.Response;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resource) {
        super(Response.Status.NOT_FOUND,
                "The following resource was not found: "+resource);
    }
}
