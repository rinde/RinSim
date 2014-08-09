/**
 * 
 */
package com.github.rinde.rinsim.event;

import javax.annotation.Nullable;

import com.google.common.base.Optional;

/**
 * The base event class. It is immutable.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 */
public class Event {

  /**
   * The type of event.
   */
  protected final Enum<?> eventType;
  private final Optional<Object> issuer;

  /**
   * Create a new event instance.
   * @param type the event type.
   * @param pIssuer The event issuer, may be null.
   */
  public Event(Enum<?> type, @Nullable Object pIssuer) {
    eventType = type;
    issuer = Optional.fromNullable(pIssuer);
  }

  /**
   * Create a new event without a issuer.
   * @param type The event type.
   */
  protected Event(Enum<?> type) {
    this(type, null);
  }

  /**
   * @return <code>true</code> if this event has an issuer, <code>false</code>
   *         otherwise.
   */
  public boolean hasIssuer() {
    return issuer.isPresent();
  }

  /**
   * @return The event issuer.
   */
  public Object getIssuer() {
    return issuer.get();
  }

  /**
   * @return The type of event.
   */
  public Enum<?> getEventType() {
    return eventType;
  }

  @Override
  public String toString() {
    return "[Event " + eventType + "]";
  }
}
