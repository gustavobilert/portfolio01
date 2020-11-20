package br.gustavobilert.poll.exception;

import br.gustavobilert.exception.BaseException;

import javax.ws.rs.core.Response;

public class CpfAlreadyVotedException extends BaseException {

    public CpfAlreadyVotedException(String cpf){
        super(Response.Status.CONFLICT,
                "The person under this CPF has already voted: "+cpf);
    }
}
