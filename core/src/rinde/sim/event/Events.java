/**
 * 
 */
package rinde.sim.event;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public interface Events {

	public void addListener(Listener l, Enum<?>... eventTypes);

	public void removeListener(Listener l, Enum<?>... eventTypes);

	public boolean containsListener(Listener l, Enum<?> eventType);

}
