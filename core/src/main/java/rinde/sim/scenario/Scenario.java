/**
 * 
 */
package rinde.sim.scenario;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Objects;

// TODO should be converted to use immutable collections and move builder into
// this class.
/**
 * Scenario is an unmodifiable list of events sorted by the time stamp. For help
 * with creating scenarios {@link ScenarioBuilder} is provided.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 */
public class Scenario {
  private final List<TimedEvent> events;
  private final Set<Enum<?>> supportedTypes;

  /**
   * Create an empty scenario.
   */
  public Scenario() {
    supportedTypes = unmodifiableSet(new LinkedHashSet<Enum<?>>());
    events = unmodifiableList(new ArrayList<TimedEvent>());
  }

  /**
   * Create a new scenario which supports the specified event types with the
   * specified events. Note that it is not checked whether the supported types
   * match the events.
   * 
   * ORDERING IS NOT CHECKED!
   * 
   * @param pSupportedTypes The types of event this scenario supports.
   * @param pEvents The actual events.
   */
  public Scenario(Collection<? extends TimedEvent> pEvents,
      Set<Enum<?>> pSupportedTypes) {
    checkArgument(!pSupportedTypes.isEmpty(),
        "supported types must be a non-empty set");
    supportedTypes = unmodifiableSet(newLinkedHashSet(pSupportedTypes));
    events = unmodifiableList(newArrayList(pEvents));
  }

  /**
   * Create a new scenario with the specified events.
   * @param pEvents The events of the scenario.
   */
  public Scenario(Collection<? extends TimedEvent> pEvents) {
    this(pEvents, collectEventTypes(pEvents));
  }

  /**
   * Copying constructor.
   * @param s the scenario to copy.
   */
  public Scenario(Scenario s) {
    this(s.events, s.getPossibleEventTypes());
  }

  /**
   * Return a scenario as a list of (time sorted) events.
   * @return the list of events.
   */
  public List<TimedEvent> asList() {
    // TODO can return original list?
    return newArrayList(events);
  }

  /**
   * @return A queue containing all events of this scenario.
   */
  public Queue<TimedEvent> asQueue() {
    return newLinkedList(events);
  }

  /**
   * @return The number of events that is in this scenario.
   */
  public int size() {
    return events.size();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (other.getClass() != getClass()) {
      return false;
    }
    return events.equals(((Scenario) other).events);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(supportedTypes, events);
  }

  /**
   * Specify event types that can occur in a scenario. The events added to
   * scenario are checked for the event type.
   * @return event types
   */
  public Set<Enum<?>> getPossibleEventTypes() {
    return supportedTypes;
  }

  /**
   * Finds all event types in the provided events.
   * @param pEvents The events to check.
   * @return A set of event types.
   */
  protected static Set<Enum<?>> collectEventTypes(
      Collection<? extends TimedEvent> pEvents) {
    final Set<Enum<?>> types = newLinkedHashSet();
    for (final TimedEvent te : pEvents) {
      types.add(te.getEventType());
    }
    return types;
  }
}
