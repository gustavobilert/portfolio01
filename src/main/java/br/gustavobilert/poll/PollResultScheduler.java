package br.gustavobilert.poll;

import br.gustavobilert.exception.SchedulingException;
import br.gustavobilert.poll.event.PollStarted;
import br.gustavobilert.timer.TimerService;
import io.quarkus.scheduler.Scheduled;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class PollResultScheduler {

    public static final String POLL_ID = "poll_id";

    @Inject
    TimerService timerService;
    @Inject
    PollRepository pollRepository;
    @Inject
    PollService pollService;

    /**
     * When the poll is started, schedule its result processing
     * @param poll The started poll
     */
    public void onPollStarted(@Observes @PollStarted Poll poll){
        schedulePollForResultProcessing(poll);
    }

    /**
     * Schedules the processing of the poll result to the end of its duration
     * @param poll Poll to have its result processed
     */
    public void schedulePollForResultProcessing(@Observes @PollStarted Poll poll){
        Map<String, Long> params = Map.of(POLL_ID, poll.getId());
        try {
            timerService.scheduleOneTimeTask(PollResultSchedulerJob.class, poll.getDuration(), params);
        } catch (SchedulerException e) {
            throw new SchedulingException(e, "Poll result processing of poll with id = "+poll.getId());
        }
    }

    /**
     * Check for polls that may not have been processed
     */
    @Scheduled(every = "1m")
    void checkUnprocessedPolls(){
        List<Poll> polls = pollRepository.findUnprocessedPolls();
        polls = filterFinishedPolls(polls);
        for (Poll poll: polls) {
            pollService.processPollResult(poll);
        }
    }

    private List<Poll> filterFinishedPolls(List<Poll> polls) {
        return polls.stream().filter(
                poll -> PollStatus.FINISHED.equals(poll.getStatus())
        ).collect(Collectors.toList());
    }

    public static class PollResultSchedulerJob implements Job {
        @Inject
        PollService pollService;

        public void execute(JobExecutionContext context) {
            Long pollId = context.getMergedJobDataMap().getLong(POLL_ID);
            pollService.processPollResultById(pollId);
        }
    }
}
