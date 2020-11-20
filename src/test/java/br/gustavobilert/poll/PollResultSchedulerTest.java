package br.gustavobilert.poll;

import br.gustavobilert.TestUtils;
import br.gustavobilert.exception.SchedulingException;
import br.gustavobilert.poll.vote.Vote;
import br.gustavobilert.poll.vote.VoteRepository;
import br.gustavobilert.timer.TimerService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.SchedulerException;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
public class PollResultSchedulerTest {

    @Inject
    PollResultScheduler pollResultScheduler;
    @Inject
    PollRepository pollRepository;
    @Inject
    VoteRepository voteRepository;
    @InjectMock
    TimerService timerServiceMock;

    @Test
    @Transactional
    public void testCheckUnprocessedPolls() {
        // Test with database to check if the @Transactional is working,
        // since we had a bug where the transaction was not
        // being executed because it was called from the same class,
        // and @Transactional depends on being injected to work
        Poll poll = createPoll();

        pollResultScheduler.checkUnprocessedPolls();

        checkPollResult(poll.getId(), 0L, 0L, PollStatus.TIED);
    }

    @Test
    @Transactional
    public void testCheckUnprocessedPollsWithVotes() {
        Poll poll = createPoll();

        placeVote(poll, true);
        placeVote(poll, false);
        placeVote(poll, false);

        pollResultScheduler.checkUnprocessedPolls();

        checkPollResult(poll.getId(), 1L, 2L, PollStatus.REJECTED);
    }

    private void placeVote(Poll poll, boolean inFavor) {
        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setVote(inFavor);
        voteRepository.persist(vote);
    }

    private void checkPollResult(Long id, long votesInFavor, long votesAgainst, PollStatus status) {
        Poll persistent = pollRepository.findById(id);
        assertThat(persistent.getVotesInFavor(), is(votesInFavor));
        assertThat(persistent.getVotesAgainst(), is(votesAgainst));
        assertThat(persistent.getStatus(), is(status));
    }

    private Poll createPoll() {
        Poll poll = new Poll();
        poll.setDescription(TestUtils.randomText());
        poll.setStartTime(OffsetDateTime.now());
        poll.setDuration(0);
        pollRepository.persist(poll);
        return poll;
    }

    @Test
    public void testSchedulingExceptionConversion() throws SchedulerException {
        Mockito.doThrow(SchedulerException.class).when(timerServiceMock)
                .scheduleOneTimeTask(eq(PollResultScheduler.PollResultSchedulerJob.class),
                anyInt(), anyMap());

        Assertions.assertThrows(SchedulingException.class,
                () -> pollResultScheduler.schedulePollForResultProcessing(Mockito.mock(Poll.class)));
    }
}
