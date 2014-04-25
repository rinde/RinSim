/**
 * 
 */
package rinde.sim.scenario;

/**
 * Interface for handling scenario events.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface TimedEventHandler {

  /**
   * Should handle the timed event.
   * @param event The event to handle.
   * @return <code>true</code> if successful, <code>false</code> otherwise.
   */
  boolean handleTimedEvent(TimedEvent event);

}
