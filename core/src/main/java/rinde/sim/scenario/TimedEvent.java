package rinde.sim.scenario;

import javax.annotation.Nullable;

import rinde.sim.event.Event;

import com.google.common.base.Objects;

/**
 * Simplest time event. The object is a value object.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class TimedEvent extends Event {
  private static final long serialVersionUID = 6832559417793517102L;

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
}
