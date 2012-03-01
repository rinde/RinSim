/**
 * 
 */
package rinde.sim.event;

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

	public Event(Enum<?> type, Object issuer) {
		eventType = type;
		this.issuer = issuer;
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
	 * @param issuer
	 */
	public void setIssuer(Object issuer) {
		if(this.issuer == null) {
			this.issuer = issuer;
		}
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
