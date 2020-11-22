package br.gustavobilert.poll.vote;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import br.gustavobilert.poll.Poll;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "cpf"}))
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifier of the voter as used in external systems, this facilitates integration without affecting our model.
     */
    private String externalVoterIdentifier;

    /**
     * CPF (brazilian registration of individuals) of the voter.
     */
    @NotBlank(message = "CPF is required")
    private String cpf;

    /**
     * The vote, true for 'Yes' and false for 'No'
     */
    @NotNull(message = "Vote is required")
    private Boolean vote;

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property="id")
    @JsonIdentityReference(alwaysAsId = true) // otherwise first ref as POJO, others as id
    @ManyToOne
    private Poll poll;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalVoterIdentifier() {
        return externalVoterIdentifier;
    }

    public void setExternalVoterIdentifier(String externalVoterIdentifier) {
        this.externalVoterIdentifier = externalVoterIdentifier;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public Boolean getVote() {
        return vote;
    }

    public void setVote(Boolean vote) {
        this.vote = vote;
    }

    public Poll getPoll() {
        return poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }
}
