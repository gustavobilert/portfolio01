package br.gustavobilert.poll;

import br.gustavobilert.TestUtils;
import br.gustavobilert.exception.ResourceNotFoundException;
import br.gustavobilert.poll.exception.CpfAlreadyVotedException;
import br.gustavobilert.poll.exception.VoterNotAllowedException;
import br.gustavobilert.poll.vote.Vote;
import br.gustavobilert.poll.vote.VoteRepository;
import br.gustavobilert.poll.vote.VoterSituation;
import br.gustavobilert.poll.vote.VoterSituationService;
import info.solidsoft.mockito.java8.api.WithAdditionalMatchers;
import info.solidsoft.mockito.java8.api.WithMockito;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.NotFoundException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static info.solidsoft.mockito.java8.AssertionMatcher.assertArg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
public class PollServiceUnitTest implements WithMockito, WithAdditionalMatchers {

    @InjectMock
    PollRepository pollRepository;
    @InjectMock
    VoteRepository voteRepository;
    @Inject
    PollService pollService;
    @InjectMock
    @RestClient
    VoterSituationService voterSituationService;

    long generatedId;

    @BeforeEach
    public void setUp() {
        generatedId = TestUtils.randomLong();
        mockRepositorySetIdOnPersistedEntity(generatedId);
    }

    @Test
    public void testCreatePersistsTheEntity() {

        String description = TestUtils.randomText();
        Poll poll = getNewPoll(description);
        pollService.create(poll);

        verify(pollRepository, times(1))
                .persist(assertArg((Poll persistedPoll) -> {
                            assertThat(persistedPoll.getId()).isEqualTo(generatedId);
                            assertThat(persistedPoll.getDescription()).isEqualTo(description);
                            assertThat(persistedPoll.getStatus()).isEqualTo(PollStatus.CREATED);
                        }
                ));
        verifyNoMoreInteractions(pollRepository);
    }

    @Test
    public void testCreateReturnsThePersistedEntity() {

        String description = TestUtils.randomText();
        Poll poll = getNewPoll(description);
        Poll createdPoll = pollService.create(poll);

        assertThat(createdPoll.getId()).isEqualTo(generatedId);
        assertThat(createdPoll.getDescription()).isEqualTo(description);
        assertThat(createdPoll.getStatus()).isEqualTo(PollStatus.CREATED);
    }

    @Test
    public void testCreateWithEmptyDescription() {
        Poll poll = getNewPoll("");
        assertThatThrownBy(() -> pollService.create(poll))
                .isExactlyInstanceOf(ConstraintViolationException.class);
    }

    @Test
    public void testList() {
        String description = TestUtils.randomText();
        when(pollRepository.listAll()).thenReturn(List.of(getPollWithId(description, generatedId)));

        List<Poll> list = pollService.list();

        assertThat(list).isNotNull();
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getId()).isEqualTo(generatedId);
        assertThat(list.get(0).getDescription()).isEqualTo(description);
    }

    @Test
    public void testGetById() {
        String description = TestUtils.randomText();
        when(pollRepository.getById(generatedId))
                .thenReturn(getPollWithId(description, generatedId));

        Poll poll = pollService.getById(generatedId);

        assertThat(poll).isNotNull();
        assertThat(poll.getId()).isEqualTo(generatedId);
        assertThat(poll.getDescription()).isEqualTo(description);
        assertThat(poll.getStatus()).isEqualTo(PollStatus.CREATED);
    }

    @Test
    public void testGetByIdNonExistent() {
        when(pollRepository.findById(Long.MAX_VALUE)).thenReturn(null);
        when(pollRepository.getById(Long.MAX_VALUE)).thenCallRealMethod();

        assertThatThrownBy(() -> pollService.getById(Long.MAX_VALUE))
                .isExactlyInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void testStart() {
        Poll mockedPoll = mock(Poll.class);
        when(pollRepository.getById(generatedId))
                .thenReturn(mockedPoll);

        PollStartParameters params = new PollStartParameters();
        params.setDuration(1000);
        pollService.start(generatedId, params);

        verify(mockedPoll, times(1)).setStartTime(isA(OffsetDateTime.class));
        verify(mockedPoll, times(1)).setDuration(1000);
    }

    @Test
    public void testStartWithoutParams() {
        Poll mockedPoll = mock(Poll.class);
        when(pollRepository.getById(generatedId))
                .thenReturn(mockedPoll);

        pollService.start(generatedId, null);

        verify(mockedPoll, times(1)).setStartTime(isA(OffsetDateTime.class));
        verify(mockedPoll, times(1)).setDuration(Poll.DEFAULT_DURATION);
    }

    @Test
    public void testStartWithNegativeDuration() {
        Poll mockedPoll = mock(Poll.class);
        when(pollRepository.getById(generatedId))
                .thenReturn(mockedPoll);

        PollStartParameters params = new PollStartParameters();
        params.setDuration(-1000);

        assertThatThrownBy(() -> pollService.start(generatedId, params))
                .isExactlyInstanceOf(ConstraintViolationException.class);
    }

    @Test
    public void testStartNonExistentPoll() {
        when(pollRepository.findById(-1000L))
                .thenReturn(null);
        when(pollRepository.getById(-1000L))
                .thenCallRealMethod();

        assertThatThrownBy(() -> pollService.start(-1000L, null))
                .isExactlyInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void testVoteByCpfValid() {
        String description = TestUtils.randomText();
        when(pollRepository.getById(generatedId))
                .thenReturn(getPollWithId(description, generatedId));
        String cpf = TestUtils.generateCpf();
        when(voterSituationService.getByCpf(cpf))
                .thenReturn(new VoterSituation(VoterSituation.VoterStatus.ABLE_TO_VOTE));

        Vote vote = new Vote();
        vote.setCpf(cpf);
        vote.setVote(false);
        String externalVoterIdentifier = "cpf-" + cpf;
        vote.setExternalVoterIdentifier(externalVoterIdentifier);
        Vote registeredVote = pollService.voteByCpf(generatedId, vote);

        assertThat(registeredVote).isNotNull();
        assertThat(registeredVote.getCpf()).isEqualTo(cpf);
        assertThat(registeredVote.getVote()).isEqualTo(false);
        assertThat(registeredVote.getExternalVoterIdentifier()).isEqualTo(externalVoterIdentifier);
        Assertions.assertThat(registeredVote.getPoll()).isNotNull();
        Assertions.assertThat(registeredVote.getPoll().getId()).isEqualTo(generatedId);
    }

    @Test
    public void testVoteByCpfInvalid() {
        String description = TestUtils.randomText();
        when(pollRepository.getById(generatedId))
                .thenReturn(getPollWithId(description, generatedId));
        String cpf = "54321";
        when(voterSituationService.getByCpf(cpf))
                .thenThrow(NotFoundException.class);

        Vote vote = new Vote();
        vote.setCpf(cpf);
        vote.setVote(false);
        assertThatThrownBy(() -> pollService.voteByCpf(generatedId, vote))
                .isExactlyInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void testVoteByCpfUnableToVote() {
        String description = TestUtils.randomText();
        when(pollRepository.getById(generatedId))
                .thenReturn(getPollWithId(description, generatedId));
        String cpf = TestUtils.generateCpf();
        when(voterSituationService.getByCpf(cpf))
                .thenReturn(new VoterSituation(VoterSituation.VoterStatus.UNABLE_TO_VOTE));

        Vote vote = new Vote();
        vote.setCpf(cpf);
        vote.setVote(false);
        assertThatThrownBy(() -> pollService.voteByCpf(generatedId, vote))
                .isExactlyInstanceOf(VoterNotAllowedException.class);
    }

    @Test
    public void testVoteByCpfDuplicated() {
        String description = TestUtils.randomText();
        when(pollRepository.getById(generatedId))
                .thenReturn(getPollWithId(description, generatedId));
        String cpf = TestUtils.generateCpf();
        when(voterSituationService.getByCpf(cpf))
                .thenReturn(new VoterSituation(VoterSituation.VoterStatus.ABLE_TO_VOTE));
        PersistenceException persistenceException = new PersistenceException(
                new org.hibernate.exception.ConstraintViolationException("", null, ""));
        doThrow(persistenceException).when(voteRepository).persist(isA(Vote.class));

        Vote vote = new Vote();
        vote.setCpf(cpf);
        vote.setVote(false);
        assertThatThrownBy(() -> pollService.voteByCpf(generatedId, vote))
                .isExactlyInstanceOf(CpfAlreadyVotedException.class);
    }

    @Test
    public void testVoteNonExistentPoll() {
        when(pollRepository.findById(-1000L)).thenReturn(null);
        when(pollRepository.getById(-1000L)).thenCallRealMethod();

        String cpf = TestUtils.generateCpf();
        when(voterSituationService.getByCpf(cpf))
                .thenReturn(new VoterSituation(VoterSituation.VoterStatus.ABLE_TO_VOTE));

        Vote vote = new Vote();
        vote.setCpf(cpf);
        vote.setVote(false);
        assertThatThrownBy(() -> pollService.voteByCpf(-1000L, vote))
                .isExactlyInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void testResult() {
        Poll fakePoll = new Poll();
        fakePoll.setStartTime(OffsetDateTime.now().minus(1, ChronoUnit.SECONDS));
        fakePoll.setDuration(1);
        when(pollRepository.getById(generatedId))
                .thenReturn(fakePoll);

        Poll poll = pollService.getById(generatedId);

        assertThat(poll).isNotNull();
        assertThat(poll.getStatus()).isEqualTo(PollStatus.TIED);
        assertThat(poll.getVotesInFavor()).isZero();
        assertThat(poll.getVotesAgainst()).isZero();
    }

    private void mockRepositorySetIdOnPersistedEntity(long id) {
        doAnswer(invocationOnMock -> {
            Poll poll = invocationOnMock.getArgument(0);
            poll.setId(id);
            return null;
        }).when(pollRepository).persist(isA(Poll.class));
    }

    private Poll getNewPoll(String description) {
        Poll poll = new Poll();
        poll.setDescription(description);
        return poll;
    }

    private Poll getPollWithId(String description, long id) {
        Poll fakePoll = getNewPoll(description);
        fakePoll.setId(id);
        return fakePoll;
    }

}
