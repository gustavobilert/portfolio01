package br.gustavobilert.poll;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Entity
public class Poll implements Serializable {

    public static final Integer DEFAULT_DURATION = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Description of the poll, where the subject being voted should be explained to the voters.
     */
    @NotBlank(message = "Description is required")
    private String description;

    /**
     * The instant when the poll starts, allowing the votes to be submitted.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd@HH:mm:ss.SSSZ")
    private OffsetDateTime startTime;

    /**
     * Duration of the poll (in seconds). When this amount of time has passed since {@link #startTime}, no more votes are allowed.
     */
    private Integer duration;

    /**
     * Votes in favor, only available after the poll has finished
     */
    private Long votesInFavor;

    /**
     * Votes against, only available after the poll has finished
     */
    private Long votesAgainst;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /**
     * Identifies the status of the poll, if it has just been created, is open, or its result when finished
     * The status is computed so it does not get inconsistent in case the poll time ends and the application fails to
     * process the votes for any reason
     */
    public PollStatus getStatus() {
        if(startTime == null) {
            return PollStatus.CREATED;
        } else if(ChronoUnit.SECONDS.between(startTime, OffsetDateTime.now()) < duration) {
            return PollStatus.STARTED;
        } else if(votesInFavor == null || votesAgainst == null) {
            return PollStatus.FINISHED;
        } else {
            if(votesInFavor > votesAgainst) {
                return PollStatus.APPROVED;
            } else if(votesInFavor < votesAgainst) {
                return PollStatus.REJECTED;
            } else {
                return PollStatus.TIED;
            }
        }
    }

    public Long getVotesInFavor() {
        return votesInFavor;
    }

    public void setVotesInFavor(Long votesInFavor) {
        this.votesInFavor = votesInFavor;
    }

    public Long getVotesAgainst() {
        return votesAgainst;
    }

    public void setVotesAgainst(Long votesAgainst) {
        this.votesAgainst = votesAgainst;
    }
}
