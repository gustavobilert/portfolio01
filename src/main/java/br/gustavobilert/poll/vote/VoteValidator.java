package br.gustavobilert.poll.vote;

import br.gustavobilert.exception.ResourceNotFoundException;
import br.gustavobilert.poll.exception.CpfAlreadyVotedException;
import br.gustavobilert.poll.exception.VoterNotAllowedException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hibernate.exception.ConstraintViolationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.ws.rs.NotFoundException;

@ApplicationScoped
public class VoteValidator {

    @Inject
    @RestClient
    VoterSituationService voterSituationService;

    /**
     * Validates the vote, throwing specific exceptions for each violation
     * @param vote The vote to validate
     */
    public void validate(Vote vote) {
        validateCpf(vote.getCpf());
    }

    private void validateCpf(String cpf) {
        VoterSituation voterSituation = getVoterSituationFromExternalService(cpf);
        checkCpfIsAbleToVote(cpf, voterSituation);
    }

    private VoterSituation getVoterSituationFromExternalService(String cpf) {
        try {
            return voterSituationService.getByCpf(cpf);
        } catch (NotFoundException e) {
            throw new ResourceNotFoundException("CPF: " + cpf);
        }
    }

    private void checkCpfIsAbleToVote(String cpf, VoterSituation voterSituation) {
        // We whitelist ABLE_TO_VOTE so if they add another status it will not be accidentally valid
        if(!VoterSituation.VoterStatus.ABLE_TO_VOTE.equals(voterSituation.getStatus())) {
            throw new VoterNotAllowedException(cpf);
        }
    }

    /**
     * Checks if the {@link PersistenceException} can be converted to a more specific and semantic one.
     * @param vote The vote that was tried to persist
     * @param persistenceException The exception to be converted
     */
    public void convertPersistenceExceptionToMoreSpecific(Vote vote, PersistenceException persistenceException) {
        checkCpfAlreadyVoted(vote.getCpf(), persistenceException);
    }

    private void checkCpfAlreadyVoted(String cpf, PersistenceException persistenceException) {
        // Hibernate exception is poor, if we had another constraint violation
        // we would have to dig into the SQL in the message
        if(persistenceException.getCause() != null
                && persistenceException.getCause() instanceof ConstraintViolationException){
            throw new CpfAlreadyVotedException(cpf);
        }
    }
}
