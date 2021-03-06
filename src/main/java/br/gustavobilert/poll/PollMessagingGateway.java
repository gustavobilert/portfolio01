package br.gustavobilert.poll;

import br.gustavobilert.poll.event.PollResultComputed;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class PollMessagingGateway {

    @Inject
    @Channel("poll-create")
    Emitter<Poll> pollEmitter;

    @ConfigProperty(name = "mp.messaging.outgoing.poll-create.enabled", defaultValue = "false")
    Boolean publishingEnabled;


    private static final Logger LOG = Logger.getLogger(PollMessagingGateway.class);

    /**
     * When the poll is completed with its result, it is published to other services through messaging
     * @param poll The completed poll
     */
    public void onPollResultComputed(@Observes @PollResultComputed Poll poll) {
        publishPoll(poll);
    }

    /**
     * Publish the poll to messaging services
     * @param poll The poll to publish
     */
    public void publishPoll(Poll poll) {
        try {
            if(publishingEnabled){
                pollEmitter.send(poll);
            }
        } catch (IllegalStateException e) {
            LOG.warn("MESSAGING NOT FOUND: ActiveMQ Artemis must be running in order to publish messages to the queue. " +
                    "You may run it by executing 'docker-compose up' in the 'docker' directory. " +
                    "You need to enable messaging in the application.properties file too.");
        }
    }

    @Incoming("polls")
    public void pollReceived(JsonObject poll){
        // Just log the message for demonstration of the feature
        LOG.info("MESSAGING: Poll received\n" + poll);
    }

}
