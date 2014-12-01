package com.github.rinde.rinsim.scenario;

/**
 * Interface for handling scenario events.
 * @author Rinde van Lon 
 */
public interface TimedEventHandler {

  /**
   * Should handle the timed event.
   * @param event The event to handle.
   * @return <code>true</code> if successful, <code>false</code> otherwise.
   */
  boolean handleTimedEvent(TimedEvent event);

}
