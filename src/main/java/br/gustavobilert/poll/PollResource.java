package br.gustavobilert.poll;

import br.gustavobilert.poll.vote.Vote;
import org.eclipse.microprofile.openapi.annotations.Operation;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/polls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PollResource {

    @Inject
    PollService pollService;

    /**
     * Creates a new poll
     * @param poll Poll to be created
     * @return The poll created and persisted, with any changes made by the application
     */
    @Operation(summary = "Criar nova pauta",
    description = "Cria uma pauta que será votada pelos cooperados após a mesma ser iniciada")
    @POST
    public Poll create(@NotNull(message = "Message body is required") Poll poll){
        return pollService.create(poll);
    }

    /**
     * List all polls
     * @return List of polls
     */
    @Operation(summary = "Listar pautas",
    description = "Recupera todas as pautas cadastradas, independente da situação atual da pauta")
    @GET
    public List<Poll> list() {
        return pollService.list();
    }

    /**
     * Retrieves a poll by its id
     * @param id Id of the poll to retrieve, must exist
     * @return The poll requested
     */
    @Operation(summary = "Obter pauta pelo Id",
    description = "Recupera uma pauta com base no Id da mesma, que é retornado quando a pauta é criada")
    @GET
    @Path("{id}")
    public Poll getById(@PathParam("id") Long id){
        return pollService.getById(id);
    }

    /**
     * Starts the poll, allowing votes to be placed until the duration expires
     * @param id Id of the poll to be started
     * @param params Params passed on the request body to configure the poll
     */
    @Operation(summary = "Iniciar votação da pauta",
    description = "Inicia a pauta, permitindo que os votos sejam registrados durante a duração definida")
    @POST
    @Path("{id}/start")
    public void start(@PathParam("id") Long id, PollStartParameters params){
        pollService.start(id, params);
    }

    /**
     * Places a vote on the poll based on the voter's CPF (brazilian registration of individuals).
     * CPF is not the ideal identifier for the voters, since it may prevent foreign people from voting and is vulnerable to legislation changes.
     * We adopted it to maintain consistency with the external system, that uses it to identify the voter,
     * and because it is specified that our API should return an error 404 (Not Found) when the CPF is invalid,
     * in this case the resource that was not found is the CPF, so the resource URI must include it to be consistent.
     *  @param pollId Id of the poll
     * @param cpf CPF of the voter
     * @return The registered vote
     */
    @Operation(summary = "Votar por CPF",
    description = "Registra um voto com base no CPF do cooperado, desde que o CPF esteja liberado para votar," +
            " conforme informação obtida a partir de outro sistema (sistema externo)")
    @POST
    @Path("{id}/vote-by-cpf/{cpf}")
    public Vote voteByCpf(@PathParam("id") Long pollId, @PathParam("cpf") String cpf, @NotNull Vote vote){
        vote.setCpf(cpf);
        return pollService.voteByCpf(pollId, vote);
    }

}
