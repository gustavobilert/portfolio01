package br.gustavobilert.poll.vote;

import br.gustavobilert.poll.Poll;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class VoteRepository implements PanacheRepository<Vote> {

    /**
     * Count the poll votes in favor or against
     * @param poll The poll to count the votes
     * @param inFavor true to count the votes in favor, false to count the votes against
     * @return The count of votes
     */
    public long countVotes(Poll poll, boolean inFavor){
        return count("poll = ?1 and vote = ?2", poll, inFavor);
    }
}
