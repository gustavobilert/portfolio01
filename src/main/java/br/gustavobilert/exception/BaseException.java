package br.gustavobilert.exception;

import io.vertx.core.json.JsonObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class BaseException extends WebApplicationException {
    public BaseException(Response.Status status, String message) {
        this(null, status, message);
    }
    public BaseException(Throwable cause, Response.Status status, String message) {
        super(cause, Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new JsonObject()
                        .put("message", message)
                        .toString())
                .build());
    }
}
