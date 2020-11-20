package br.gustavobilert.timer;

import org.quartz.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@ApplicationScoped
public class TimerService {

    @Inject
    Scheduler quartz;

    /**
     * Schedules a one-time task to be executed after a delay
     * @param jobClass Task class, must implement Job interface
     * @param delay Delay in seconds to execute the task
     * @param jobParameters Map containing the parameters needed by the task
     * @throws SchedulerException If the task can not be scheduled
     */
    public void scheduleOneTimeTask(Class<? extends Job> jobClass, int delay, Map<?, ?> jobParameters) throws SchedulerException {
        JobDetail job = createJob(jobClass);
        Trigger trigger = createTrigger(jobClass, delay, jobParameters);
        quartz.scheduleJob(job, trigger);
    }

    private JobDetail createJob(Class<? extends Job> jobClass) {
        return JobBuilder.newJob(jobClass)
                .withDescription(jobClass.getSimpleName())
                .build();
    }

    private Trigger createTrigger(Class<? extends Job> jobClass, int delay, Map<?, ?> jobParameters) {
        return TriggerBuilder.newTrigger()
                .withDescription(jobClass.getSimpleName() + "-" + OffsetDateTime.now())
                .usingJobData(new JobDataMap(jobParameters))
                .startAt(getStartTime(delay))
                .build();
    }

    private Date getStartTime(int delay) {
        return new Date(Instant.now().plus(delay, ChronoUnit.SECONDS).toEpochMilli());
    }
}
