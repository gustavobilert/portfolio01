package br.gustavobilert.poll;

import br.gustavobilert.exception.ResourceNotFoundException;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class PollRepository implements PanacheRepository<Poll> {

    /**
     * Retrieves a poll from the database
     * @param id Id of the poll
     * @return The loaded poll
     * @throws ResourceNotFoundException if the poll does not exist
     */
    public Poll getById(Long id) throws ResourceNotFoundException {
        Poll poll = findById(id);
        if(poll == null) {
            throw new ResourceNotFoundException("Poll with id = " + id);
        }
        return poll;
    }

    /**
     * Find polls that do not have the count of votes in favor or against yet.
     * @return The unprocessed polls
     */
    public List<Poll> findUnprocessedPolls() {
        return find("votesInFavor is null OR votesAgainst is null").list();
    }
}
