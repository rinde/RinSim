/**
 * 
 */
package rinde.sim.scenario;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Scenario is an unmodifiable list of events sorted by the time stamp. For help
 * with creating scenarios {@link ScenarioBuilder} is provided.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 */
public class Scenario implements Serializable {
    private static final long serialVersionUID = 1839693038677831786L;

    private final List<TimedEvent> events;
    private final Set<Enum<?>> supportedTypes;

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
        checkArgument(!pEvents.isEmpty(), "events can not be null or empty");
        checkArgument(!pSupportedTypes.isEmpty(), "supported types must be a non-empty set");
        supportedTypes = newLinkedHashSet(pSupportedTypes);

        // final PriorityQueue<TimedEvent> tmp = new PriorityQueue<TimedEvent>(
        // pEvents.size(), new TimeComparator());
        // tmp.addAll(pEvents);
        // System.out.println(tmp);

        // final List<TimedEvent> tmp = ;

        events = unmodifiableList(newArrayList(pEvents));

        // events = new PriorityQueue<TimedEvent>(max(pEvents.size(), 1),
        // new TimeComparator());
        // events.addAll(pEvents);
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
        return newArrayList(events);
    }

    public Queue<TimedEvent> asQueue() {
        return newLinkedList(events);
    }

    /**
     * Get the access to the first event in the scenario (without removing it).
     * @return element or <code>null</code> when scenario has no more events.
     */
    // public TimedEvent peek() {
    // return events.peek();
    // }

    /**
     * Retrieve an element from the scenario (removing it from list).
     * @return element or <code>null</code> when scenario has no more events
     */
    // public TimedEvent poll() {
    // return events.poll();
    // }

    /**
     * @return The number of events that is in this scenario.
     */
    public int size() {
        return events.size();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Scenario
                && events.size() == ((Scenario) other).events.size()) {
            final Scenario s1 = (Scenario) other;
            return asList().equals(s1.asList());
        }
        return false;
    }

    /**
     * Specify event types that can occur in a scenario. The events added to
     * scenario are checked for the event type.
     * @return event types
     */
    public Set<Enum<?>> getPossibleEventTypes() {
        return unmodifiableSet(supportedTypes);
    }

    protected static Set<Enum<?>> collectEventTypes(
            Collection<? extends TimedEvent> pEvents) {
        if (pEvents == null) {
            throw new IllegalArgumentException("events can not be null");
        }
        final Set<Enum<?>> types = newHashSet();
        for (final TimedEvent te : pEvents) {
            types.add(te.getEventType());
        }
        return types;
    }
}
