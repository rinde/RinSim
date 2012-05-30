/**
 * 
 */
package rinde.sim.event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;

import rinde.sim.scenario.TimedEvent;

/**
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 */
public class Event implements Serializable {
	private static final long serialVersionUID = -390528892294335442L;

	protected final Enum<?> eventType;
	transient private Object issuer;

	/**
	 * Create a new event instance.
	 * @param type the event type.
	 * @param pIssuer The event issuer, may be null.
	 */
	public Event(Enum<?> type, Object pIssuer) {
		checkArgument(type != null, "type can not be null");
		eventType = type;
		issuer = pIssuer;
	}

	/**
	 * Should be used only by extension classes when the issuer is not known at
	 * creation time.
	 * @see TimedEvent
	 * @param type
	 */
	protected Event(Enum<?> type) {
		this(type, null);
	}

	/**
	 * Issuer is set only if it was previously empty.
	 * @param pIssuer The issuer of the event.
	 */
	public void setIssuer(Object pIssuer) {
		checkArgument(pIssuer != null, "issuer can not be null");
		checkState(issuer == null, "issuer is already set, can not be overridden");
		issuer = pIssuer;
	}

	/**
	 * 
	 * @return typed issuer
	 */
	public Object getIssuer() {
		return issuer;
	}

	public Enum<?> getEventType() {
		return eventType;
	}

	@Override
	public String toString() {
		return "[Event " + eventType + "]";
	}

}
