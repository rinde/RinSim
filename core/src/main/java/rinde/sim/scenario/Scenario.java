/**
 * 
 */
package rinde.sim.scenario;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.unmodifiableSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import rinde.sim.event.pdp.StandardType;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Scenario is an unmodifiable list of events sorted by the time stamp.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 */
public class Scenario implements Serializable {
	private static final long serialVersionUID = 1839693038677831786L;

	private final PriorityQueue<TimedEvent> events;

	public Scenario() {
		events = new PriorityQueue<TimedEvent>(1024, new TimeComparator());
	}

	public Scenario(Collection<? extends TimedEvent> pEvents) {
		Collection<? extends TimedEvent> tempEvents = pEvents;
		if (pEvents == null) {
			tempEvents = Collections.emptyList();
		}
		checkEvents(tempEvents, getPossibleEventTypes());
		events = new PriorityQueue<TimedEvent>(1024, new TimeComparator());
		events.addAll(tempEvents);
	}

	/**
	 * Copying constructor.
	 * @param s
	 */
	public Scenario(Scenario s) {
		if (s == null) {
			throw new IllegalArgumentException("scenario cannot be null");
		}
		events = new PriorityQueue<TimedEvent>(1024, new TimeComparator());
		events.addAll(s.events);
	}

	protected static void checkEvents(Collection<? extends TimedEvent> eC, final Set<Enum<?>> types) {
		if (types == null) {
			throw new IllegalArgumentException("types not specified via getPossibleEventTypes()");
		}
		boolean violation = Iterables.any(eC, new Predicate<TimedEvent>() {
			@Override
			public boolean apply(TimedEvent i) {
				return !types.contains(i.getEventType());
			}
		});
		if (violation) {
			throw new IllegalArgumentException("not supported event type");
		}
	}

	/**
	 * Return a scenario as a list of (time sorted) events;
	 * @return
	 */
	public List<TimedEvent> asList() {
		ArrayList<TimedEvent> result = new ArrayList<TimedEvent>();
		PriorityQueue<TimedEvent> copy = new PriorityQueue<TimedEvent>(events);
		TimedEvent e = null;
		while ((e = copy.poll()) != null) {
			result.add(e);
		}
		return result;
	}

	/**
	 * Get the access to the first event in the scenario (without removing it)
	 * @return
	 */
	public TimedEvent peek() {
		return events.peek();
	}

	/**
	 * Retrieve an element from the scenario (removing it from list)
	 * @return element or <code>null</code> when scenario has no more events
	 */
	public TimedEvent poll() {
		return events.poll();
	}

	public boolean add(TimedEvent e) {
		checkArgument(e != null, "event can not be null");
		checkEvents(Collections.singleton(e), getPossibleEventTypes());
		return events.add(e);
	}

	public boolean addAll(Collection<? extends TimedEvent> collection) {
		checkArgument(collection != null, "collection can not be null");
		checkEvents(collection, getPossibleEventTypes());
		return events.addAll(collection);
	}

	public boolean remove(Object object) {
		checkArgument(object != null, "object can not be null");
		return events.remove(object);
	}

	public boolean removeAll(Collection<?> collection) {
		checkArgument(collection != null);
		return events.removeAll(collection);
	}

	public void clear() {
		events.clear();
	}

	public int size() {
		return events.size();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Scenario && events.size() == ((Scenario) other).events.size()) {
			Scenario s1 = (Scenario) other;
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
		return unmodifiableSet(newHashSet((Enum<?>[]) StandardType.values()));
	}

	private static class TimeComparator implements Comparator<TimedEvent>, Serializable {

		private static final long serialVersionUID = -2711991793346719648L;

		/**
		 * 
		 */
		public TimeComparator() {}

		@Override
		public int compare(TimedEvent o1, TimedEvent o2) {
			return (int) (o1.time - o2.time);
		}
	}
}
