package br.gustavobilert.exception;

import javax.ws.rs.core.Response;

public class SchedulingException extends BaseException {

    public SchedulingException(Throwable cause, String jobDescription) {
        super(cause, Response.Status.INTERNAL_SERVER_ERROR,
                "Failed to schedule job: "+jobDescription);
    }
}
