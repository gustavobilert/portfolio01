package br.gustavobilert.poll.exception;

import br.gustavobilert.exception.BaseException;

import javax.ws.rs.core.Response;

public class VoterNotAllowedException extends BaseException {

    public VoterNotAllowedException(String cpf) {
        super(Response.Status.FORBIDDEN,
                "The person under the CPF "+cpf+" is not allowed to vote.");
    }
}
