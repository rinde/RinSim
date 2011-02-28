/**
 * 
 */
package rinde.sim.event;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class Event {

	public final Enum<?> eventType;
	public final Object issuer;

	public Event(Enum<?> type, Object issuer) {
		eventType = type;
		this.issuer = issuer;
	}

	@Override
	public String toString() {
		return "[Event " + eventType + "]";
	}

}
