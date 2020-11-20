package br.gustavobilert.poll;

import br.gustavobilert.poll.event.PollResultComputed;
import br.gustavobilert.poll.event.PollStarted;
import br.gustavobilert.poll.vote.Vote;
import br.gustavobilert.poll.vote.VoteRepository;
import br.gustavobilert.poll.vote.VoteValidator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class PollService {

    @Inject
    PollRepository pollRepository;
    @Inject
    VoteRepository voteRepository;
    @Inject
    VoteValidator voteValidator;
    @Inject
    @PollStarted
    Event<Poll> pollStarted;
    @Inject
    @PollResultComputed
    Event<Poll> pollResultComputed;

    /**
     * Creates a new poll
     * @param poll Poll to be created
     * @return The poll created and persisted, with any changes made by the application
     */
    @Transactional
    public Poll create(@NotNull(message = "Poll is required") @Valid Poll poll){
        pollRepository.persist(poll);
        return poll;
    }

    /**
     * List all polls
     * @return List of polls
     */
    public List<Poll> list() {
        return pollRepository.listAll();
    }

    /**
     * Retrieves a poll by its id
     * @param id Id of the poll to retrieve, must exist
     * @return The poll requested
     */
    public Poll getById(Long id){
        Poll poll = pollRepository.getById(id);
        if(PollStatus.FINISHED.equals(poll.getStatus())){
            countVotes(poll);
        }
        return poll;
    }

    /**
     * Starts the poll, allowing votes to be placed until the duration expires
     * @param id Id of the poll to be started
     * @param params Parameters object to configure the poll
     */
    @Transactional
    public void start(Long id, @Valid PollStartParameters params){
        Poll poll = pollRepository.getById(id);
        poll.setStartTime(OffsetDateTime.now());
        poll.setDuration(getDurationFromParamsOrDefaultDuration(params));
        pollStarted.fire(poll);
    }

    private Integer getDurationFromParamsOrDefaultDuration(PollStartParameters params) {
        return params != null && params.getDuration() != null ? params.getDuration() : Poll.DEFAULT_DURATION;
    }

    /**
     * Places a vote on the poll. The voter is identified by his/her CPF (brazilian registration of individuals).
     *  @param pollId Id of the poll
     * @return The registered vote
     */
    @Transactional
    public Vote voteByCpf(@NotNull Long pollId, @NotNull @Valid Vote vote){
        Poll poll = pollRepository.getById(pollId);
        vote.setPoll(poll);
        voteValidator.validate(vote);
        try {
            voteRepository.persist(vote);
            return vote;
        } catch (PersistenceException persistenceException){
            voteValidator.convertPersistenceExceptionToMoreSpecific(vote, persistenceException);
            throw persistenceException;
        }
    }

    @Transactional
    void processPollResultById(Long pollId) {
        Poll poll = pollRepository.findById(pollId);
        processPollResult(poll);
    }

    /**
     * Processes the poll result, counting its votes and publishing to message queue
     * @param poll The poll to process
     */
    @Transactional
    void processPollResult(Poll poll) {
        if(PollStatus.FINISHED.equals(poll.getStatus())){
            countVotes(poll);
            pollResultComputed.fire(poll);
        }
    }

    /**
     * Counts the votes and sets the result to the poll
     * @param poll Poll to compute the votes
     */
    public void countVotes(Poll poll) {
        long votesInFavor = voteRepository.countVotes(poll, true);
        poll.setVotesInFavor(votesInFavor);
        long votesAgainst = voteRepository.countVotes(poll, false);
        poll.setVotesAgainst(votesAgainst);
    }

}
