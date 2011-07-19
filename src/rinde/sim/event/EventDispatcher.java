/**
 * 
 */
package rinde.sim.event;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This class is the root in the Event system. It provides methods for
 * dispatching events and removing and adding of listeners.
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public class EventDispatcher {

	protected final Multimap<Enum<?>, Listener> listeners;
	protected final Set<Enum<?>> types;

	/**
	 * Creates a new {@link EventDispatcher} instance which is capable of
	 * dispatching any {@link Event} with a <code>type</code> attribute that is
	 * one of <code>eventTypes</code>.
	 * @param eventTypes The types of events.
	 */
	public EventDispatcher(Enum<?>... eventTypes) {
		listeners = HashMultimap.create();
		types = new HashSet<Enum<?>>(asList(eventTypes));
	}

	public void dispatchEvent(Event e) {
		if (!types.contains(e.eventType)) {
			throw new IllegalArgumentException("Cannot dispatch an event of type " + e.eventType + " since it was not registered at this dispatcher.");
		}
		for (Listener l : listeners.get(e.eventType)) {
			l.handleEvent(e);
		}
	}

	public void addListener(Listener l, Enum<?> eventType) {
		if (types.contains(eventType)) {
			listeners.put(eventType, l);
		} else {
			throw new IllegalArgumentException("A listener for type " + eventType + " is not allowed");
		}
	}

	public void addListener(Listener l, Enum<?>... eventTypes) {
		if (eventTypes.length == 0) {
			throw new IllegalArgumentException("A listener has to listen to at least one event type.");
		}
		for (Enum<?> t : eventTypes) {
			addListener(l, t);
		}
	}

	public void removeListenerForAllTypes(Listener listener) {
		// store keys in intermediate set to avoid concurrent modifications
		Set<Enum<?>> keys = new HashSet<Enum<?>>(listeners.keySet());
		for (Enum<?> eventType : keys) {
			if (listeners.containsEntry(eventType, listener)) {
				removeListener(listener, eventType);
			}
		}
	}

	public void removeListener(Listener l, Enum<?> eventType) {
		if (containsListener(l, eventType)) {
			listeners.remove(eventType, l);
		} else {
			throw new IllegalArgumentException("The listener " + l + " for the type " + eventType + " cannot be removed because it does not exist.");
		}
	}

	public void removeListener(Listener l, Enum<?>... eventTypes) {
		if (eventTypes.length == 0) {
			removeListenerForAllTypes(l);
		} else {
			for (Enum<?> e : eventTypes) {
				removeListener(l, e);
			}
		}
	}

	public boolean containsListener(Listener listener, Enum<?> eventType) {
		return listeners.containsEntry(eventType, listener);
	}

	/**
	 * This method returns an {@link Events} instance which can be made publicly
	 * available to classes outside the scope of the events. Through this
	 * instance listeners can be added and removed to this
	 * {@link EventDispatcher}.
	 * @return A wrapper for {@link EventDispatcher}, only shows the methods
	 *         which should be allowed to be modified outside of the
	 *         dispatcher's parent.
	 */
	public Events getEvents() {
		final EventDispatcher ref = this;
		return new Events() {
			@Override
			public void addListener(Listener l, Enum<?>... eventTypes) {
				ref.addListener(l, eventTypes);
			}

			@Override
			public void removeListener(Listener l, Enum<?>... eventTypes) {
				ref.removeListener(l, eventTypes);
			}

			@Override
			public boolean containsListener(Listener l, Enum<?> eventType) {
				return ref.containsListener(l, eventType);
			}
		};
	}
}
