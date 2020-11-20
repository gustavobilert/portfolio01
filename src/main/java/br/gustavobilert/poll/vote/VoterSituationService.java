package br.gustavobilert.poll.vote;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@ApplicationScoped
@RegisterRestClient(configKey = "voter-situation-api")
public interface VoterSituationService {

    @GET
    @Path("/users/{cpf}")
    @Produces(MediaType.APPLICATION_JSON)
    VoterSituation getByCpf(@PathParam("cpf") String cpf);
}
