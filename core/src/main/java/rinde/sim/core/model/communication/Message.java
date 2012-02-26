package rinde.sim.core.model.communication;

/**
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 *
 */
public abstract class Message {
	protected final CommunicationUser sender;

	public Message(CommunicationUser sender) {
		this.sender = sender;
	}
}
