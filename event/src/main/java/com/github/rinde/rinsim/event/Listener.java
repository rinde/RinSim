/**
 * 
 */
package com.github.rinde.rinsim.event;

/**
 * Interface for listening to {@link Event}s.
 * @author Rinde van Lon 
 */
public interface Listener {
  /**
   * Is called to notify the listener that an {@link Event} was issued.
   * @param e The event.
   */
  void handleEvent(Event e);
}
