package rinde.sim.core.model.communication;

/**
 * Communication API exposed to agent to allow them for communication.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public interface CommunicationAPI {
    /**
     * Send the message to a given recipient. Message will be delivered with a
     * specific probability if recipient is within the range (see
     * {@link CommunicationUser} for details).
     * @param recipient
     * @param message
     */
    void send(CommunicationUser recipient, Message message);

    /**
     * Send the message to a given recipient. Message will be delivered with a
     * specific probability to all possible recipients within the range (see
     * {@link CommunicationUser} for details).
     * @param message
     */
    void broadcast(Message message);

    /**
     * Send the message to a given recipient. Message will be delivered with a
     * specific probability to all possible recipients within the range (see
     * {@link CommunicationUser} for details).
     * @param message
     * @param type type of recipient to deliver a message to
     */
    void broadcast(Message message, Class<? extends CommunicationUser> type);
}
