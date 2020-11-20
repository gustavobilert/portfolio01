package br.gustavobilert.poll;

import br.gustavobilert.TestUtils;
import br.gustavobilert.poll.vote.VoterSituation;
import br.gustavobilert.poll.vote.VoterSituationService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class PollResourceTest {

    @InjectMock
    @RestClient
    VoterSituationService voterSituationService;
    @Inject
    PollRepository pollRepository;

    @Test
    public void testCreate(){
        String description = TestUtils.randomText();
        given()
            .contentType(ContentType.JSON)
            .body(new JsonObject().put("description", description).toString())
        .when()
            .post("/polls")
        .then()
            .statusCode(200)
            .body("id", greaterThan(0))
            .body("description", is(description))
            .body("status", is(PollStatus.CREATED.toString()));

    }

    @Test
    public void testCreateWithoutBody(){
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/polls")
                .then()
                .statusCode(400)
                .body("errors", hasItem(
                        hasEntry("message", "Message body is required")
                ));
    }

    @Test
    public void testCreateWithEmptyDescription(){
        given()
                .contentType(ContentType.JSON)
                .body(new JsonObject().toString())
                .when()
                .post("/polls")
                .then()
                .statusCode(400)
                .body("errors", hasItem(
                        allOf(
                                hasEntry("field", "description"),
                                hasEntry("message", "Description is required")
                        )
                ));
    }

    @Test
    public void testList() {
        String description = TestUtils.randomText();
        createPoll(description);

        given()
        .when()
            .get("/polls")
        .then()
            .statusCode(200)
            .body(is(not(empty())))
            .body("description", hasItem(description));
    }

    @Test
    public void testGetById() {
        String description = TestUtils.randomText();
        int id = createPoll(description);

        given()
            .pathParam("id",id)
        .when()
            .get("/polls/{id}")
        .then()
            .statusCode(200)
            .body(is(not(empty())))
            .body("id", is(id))
            .body("status", is(PollStatus.CREATED.toString()));
    }


    @Test
    public void testGetByIdInvalid() {
        given()
                .when()
                .get("/polls/null")
                .then()
                .statusCode(404);
    }

    @Test
    public void testGetByIdNonExistent() {
        given()
                .pathParam("id",Long.MAX_VALUE)
                .when()
                .get("/polls/{id}")
                .then()
                .statusCode(404)
                .body("message", is("The following resource was not found: Poll with id = "+Long.MAX_VALUE));
    }

    private int createPoll(String description) {
        // we have to use int because of a RestAssured open bug: https://github.com/rest-assured/rest-assured/issues/741
        return given()
                .contentType(ContentType.JSON)
                .body(new JsonObject().put("description", description).toString())
                .post("/polls")
                .then()
                .extract().body().jsonPath().getInt("id");
    }

    @Test
    public void testStart() {
        // Create the poll
        String description = TestUtils.randomText();
        int id = createPoll(description);

        // Start the poll
        startPoll(id, 30)
            .then()
            .statusCode(204);

        // Check fields
        given()
            .pathParam("id",id)
        .when()
            .get("/polls/{id}")
        .then()
            .statusCode(200)
            .body(is(not(empty())))
            .body("id", is(id))
            .body("status", is(PollStatus.STARTED.toString()))
            .body("startTime", is(not(nullValue())))
            .body("duration", is(30));
    }

    @Test
    public void testStartWithoutParams() {
        // Create the poll
        String description = TestUtils.randomText();
        int id = createPoll(description);

        // Start the poll
        given()
                .contentType(ContentType.JSON)
                .pathParam("id", id)
                .when()
                .post("/polls/{id}/start")
                .then()
                .statusCode(204);

        // Check fields
        given()
                .pathParam("id",id)
                .when()
                .get("/polls/{id}")
                .then()
                .statusCode(200)
                .body(is(not(empty())))
                .body("id", is(id))
                .body("status", is(PollStatus.STARTED.toString()))
                .body("startTime", is(not(nullValue())))
                .body("duration", is(Poll.DEFAULT_DURATION));
    }

    @Test
    public void testStartWithNegativeDuration() {
        // Create the poll
        String description = TestUtils.randomText();
        int id = createPoll(description);

        // Start the poll
        startPoll(id, -30)
                .then()
                .statusCode(400)
                .body("errors", hasItem(
                        allOf(
                                hasEntry("field", "duration"),
                                hasEntry("message", "Duration must be a positive value")
                        )
                ));
    }

    @Test
    public void testStartNonExistentPoll() {
        // Start the poll
        startPoll(Integer.MAX_VALUE, 30)
                .then()
                .statusCode(404)
                .body("message", is("The following resource was not found: Poll with id = "+Integer.MAX_VALUE));
    }

    private Response startPoll(int id, int duration) {
        return given()
                .contentType(ContentType.JSON)
                .pathParam("id", id)
                .body(new JsonObject().put("duration", duration).toString())
                .when()
                .post("/polls/{id}/start");
    }

    @Test
    public void testVoteByCpfValid() {
        String cpf = TestUtils.generateCpf();
        Mockito.when(voterSituationService.getByCpf(cpf))
                .thenReturn(new VoterSituation(VoterSituation.VoterStatus.ABLE_TO_VOTE));

        // Create the poll
        String description = TestUtils.randomText();
        int id = createPoll(description);

        // Start the poll
        startPoll(id, 30);

        // Place vote
        String externalVoterIdentifier = "cpf-"+cpf;
        placeVoteByCpf(id, cpf, externalVoterIdentifier, true)
        .then()
            .statusCode(200)
            .body("cpf", is(cpf))
            .body("poll", is(id))
            .body("externalVoterIdentifier", is(externalVoterIdentifier));
    }

    @Test
    public void testVoteByCpfInvalid() {
        String cpf = "54321";
        Mockito.when(voterSituationService.getByCpf(cpf))
                .thenThrow(NotFoundException.class);

        // Create the poll
        String description = TestUtils.randomText();
        int id = createPoll(description);

        // Start the poll
        startPoll(id, 30);

        // Place vote
        String externalVoterIdentifier = "cpf-"+cpf;
        placeVoteByCpf(id, cpf, externalVoterIdentifier, true)
                .then()
                .statusCode(404)
                .body("message", is("The following resource was not found: CPF: "+cpf));
    }


    @Test
    public void testVoteByCpfUnableToVote() {
        String cpf = TestUtils.generateCpf();
        Mockito.when(voterSituationService.getByCpf(cpf))
                .thenReturn(new VoterSituation(VoterSituation.VoterStatus.UNABLE_TO_VOTE));

        // Create the poll
        String description = TestUtils.randomText();
        int id = createPoll(description);

        // Start the poll
        startPoll(id, 30);

        // Place vote
        String externalVoterIdentifier = "cpf-"+cpf;
        placeVoteByCpf(id, cpf, externalVoterIdentifier, true)
                .then()
                .statusCode(403)
                .body("message", is("The person under the CPF "+cpf+" is not allowed to vote."));
    }

    @Test
    public void testVoteByCpfDuplicated() {
        String cpf = TestUtils.generateCpf();
        Mockito.when(voterSituationService.getByCpf(cpf))
                .thenReturn(new VoterSituation(VoterSituation.VoterStatus.ABLE_TO_VOTE));

        // Create the poll
        String description = TestUtils.randomText();
        int id = createPoll(description);

        // Start the poll
        startPoll(id, 30);

        // Place vote
        String externalVoterIdentifier = "cpf-"+cpf;
        placeVoteByCpf(id, cpf, externalVoterIdentifier, true)
                .then()
                .statusCode(200)
                .body("cpf", is(cpf))
                .body("poll", is(id))
                .body("externalVoterIdentifier", is(externalVoterIdentifier));

        // Place another vote with same CPF
        placeVoteByCpf(id, cpf, externalVoterIdentifier, false)
                .then()
                .statusCode(409)
                .body("message", is("The person under this CPF has already voted: "+cpf));

    }

    private Response placeVoteByCpf(int id, String cpf, String externalVoterIdentifier, boolean vote) {
        return given()
                .contentType(ContentType.JSON)
                .pathParam("id", id)
                .pathParam("cpf", cpf)
                .body(new JsonObject().put("vote", vote)
                        .put("externalVoterIdentifier", externalVoterIdentifier).toString())
                .when()
                .post("/polls/{id}/vote-by-cpf/{cpf}");
    }

    @Test
    public void testVoteNonExistentPoll() {
        // Place vote
        String cpf = TestUtils.generateCpf();
        given()
                .contentType(ContentType.JSON)
                .pathParam("id", Long.MAX_VALUE)
                .pathParam("cpf", cpf)
                .body(new JsonObject().put("vote", true).toString())
                .when()
                .post("/polls/{id}/vote-by-cpf/{cpf}")
                .then()
                .statusCode(404)
                .body("message", is("The following resource was not found: Poll with id = "+Long.MAX_VALUE));
    }

    @Test
    public void testResult() throws InterruptedException {
        // Create the poll
        String description = TestUtils.randomText();
        int id = createPoll(description);

        // Start the poll
        startPoll(id, 1);

        Thread.sleep(1000);

        given()
                .pathParam("id",id)
                .when()
                .get("/polls/{id}")
                .then()
                .statusCode(200)
                .body(is(not(empty())))
                .body("id", is(id))
                .body("status", is(PollStatus.TIED.toString()));

        checkPollStatusOnDatabase(id, PollStatus.TIED);
    }

    private void checkPollStatusOnDatabase(long id, PollStatus status) {
        Poll poll = pollRepository.findById(id);
        MatcherAssert.assertThat(poll, is(not(nullValue())));
        MatcherAssert.assertThat("Poll result was not persisted by the scheduled execution",
                poll.getStatus(), is(status));
    }

}