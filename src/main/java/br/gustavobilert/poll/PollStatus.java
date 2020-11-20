package br.gustavobilert.poll;

public enum PollStatus {

    /**
     * The poll was created and is ready to be started
     */
    CREATED,
    /**
     * The poll has started and is open for voting
     */
    STARTED,
    /**
     * The poll voting time has ended, but the votes were not computed yet
     */
    FINISHED,
    /**
     * The poll was approved
     */
    APPROVED,
    /**
     * The poll was rejected
     */
    REJECTED,
    /**
     * The poll has tied, with same amount of votes against and in favor
     */
    TIED
}
