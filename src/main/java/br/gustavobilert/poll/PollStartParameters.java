package br.gustavobilert.poll;

import javax.validation.constraints.Positive;

public class PollStartParameters {

    /**
     * Duration of the poll in seconds, defaults to 60s.
     */
    @Positive(message = "Duration must be a positive value")
    private Integer duration;

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}
