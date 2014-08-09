package com.github.rinde.rinsim.scenario;

import java.util.Comparator;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.event.Event;
import com.google.common.base.Objects;

/**
 * Simplest time event. The object is a value object.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class TimedEvent extends Event {

  /**
   * The time at which the event is to be dispatched.
   */
  public final long time;

  /**
   * Create a new TimedEvent.
   * @param type The type of the event.
   * @param timestamp The time at which the event occurs.
   */
  public TimedEvent(Enum<?> type, long timestamp) {
    super(type);
    time = timestamp;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(eventType, time);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final TimedEvent other = (TimedEvent) obj;
    if (!eventType.equals(other.eventType)) {
      return false;
    }
    return time == other.time;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(eventType).append("|").append(time)
        .toString();
  }

  /**
   * Comparator for comparing {@link TimedEvent}s on their time.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public enum TimeComparator implements Comparator<TimedEvent> {
    /**
     * Comparator for comparing {@link TimedEvent}s on their time.
     */
    INSTANCE;

    @Override
    public int compare(TimedEvent o1, TimedEvent o2) {
      return (int) (o1.time - o2.time);
    }
  }
}
