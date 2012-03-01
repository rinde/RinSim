/**
 * 
 */
package rinde.sim.scenario;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

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

	public Scenario(Collection<? extends TimedEvent> events) {
		if(events == null) events = Collections.emptyList();
		checkEvents(events, getPossibleEventTypes());
		this.events = new PriorityQueue<TimedEvent>(1024, new TimeComparator());
		this.events.addAll(events);
	}
	
	protected static void checkEvents(Collection<? extends TimedEvent> eC,
			final Enum<?>[] types) {
		if(types == null) throw new IllegalArgumentException("types not specified via getPossibleEventTypes()");
		boolean violation = Iterables.any(eC, new Predicate<TimedEvent>() {
			@Override
			public boolean apply(TimedEvent i) {
				for (Enum<?> e : types) { if(i.getEventType() == e) return false; }
				return true;
			}
		});
		if(violation) throw new IllegalArgumentException("not supported event type");
	}

	/**
	 * Copying constructor.
	 * @param s
	 */
	public Scenario(Scenario s) {
		if(s == null) throw new IllegalArgumentException("scenario cannot be null");
		this.events = new PriorityQueue<TimedEvent>(1024, new TimeComparator());
		this.events.addAll(s.events);
	}
	
	public Scenario() {
		this.events = new PriorityQueue<TimedEvent>(1024, new TimeComparator());
	}

	/**
	 * Return a scenario as a list of (time sorted) events;
	 * @return
	 */
	public List<TimedEvent> asList() {
		ArrayList<TimedEvent> result = new ArrayList<TimedEvent>();
		PriorityQueue<TimedEvent> copy = new PriorityQueue<TimedEvent>(events);
		TimedEvent e = null;
		while((e = copy.poll()) != null)
				result.add(e);
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
		checkEvents(Collections.singleton(e), getPossibleEventTypes());
		return events.add(e);
	}
	
	public boolean addAll(Collection<? extends TimedEvent> c) {
		checkEvents(c, getPossibleEventTypes());
		return events.addAll(c);
	}

	public boolean remove(Object o) {
		return events.remove(o);
	}

	public boolean removeAll(Collection<?> c) {
		return events.removeAll(c);
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
	 * Specify event types that can occur in a scenario. The events added to scenario are 
	 * checked for the event type.
	 * @return event types
	 */
	public Enum<?>[] getPossibleEventTypes() {
		return StandardType.values();
	}
	
	private static class TimeComparator implements Comparator<TimedEvent>, Serializable {

		private static final long serialVersionUID = -2711991793346719648L;

		@Override
		public int compare(TimedEvent o1, TimedEvent o2) {
			return (int) (o1.time - o2.time);
		}
	};
}
