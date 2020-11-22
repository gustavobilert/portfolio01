package br.gustavobilert.poll.vote;

public class VoterSituation {

    public enum VoterStatus {
        ABLE_TO_VOTE,
        UNABLE_TO_VOTE
    }

    private VoterStatus status;

    public VoterSituation() {}

    public VoterSituation(VoterStatus status) {
        this.status = status;
    }

    public VoterStatus getStatus() {
        return status;
    }
}
