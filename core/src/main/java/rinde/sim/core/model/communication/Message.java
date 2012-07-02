package rinde.sim.core.model.communication;

/**
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 */
public abstract class Message implements Cloneable {
    protected final CommunicationUser sender;

    public Message(CommunicationUser sender) {
        this.sender = sender;
    }

    @Override
    public Message clone() throws CloneNotSupportedException {
        return (Message) super.clone();
    }

    public CommunicationUser getSender() {
        return sender;
    }
}
