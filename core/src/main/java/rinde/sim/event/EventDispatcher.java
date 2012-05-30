/**
 * 
 */
package rinde.sim.event;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.LinkedHashMultimap;
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
	 * @param supportedEventTypes The types of events this EventDispatcher
	 *            supports.
	 */
	public EventDispatcher(Enum<?>... supportedEventTypes) {
		checkArgument(supportedEventTypes != null, "event types can not be null");
		listeners = LinkedHashMultimap.create();
		types = new HashSet<Enum<?>>(asList(supportedEventTypes));
	}

	/**
	 * Dispatch an event. Notifies all listeners that are listening for this
	 * type of event.
	 * @param e The event to be dispatched, only events with a supported type
	 *            can be dispatched.
	 */
	public void dispatchEvent(Event e) {
		if (e == null) {
			throw new IllegalArgumentException("event can not be null");
		}
		checkArgument(types.contains(e.getEventType()), "Cannot dispatch an event of type " + e.getEventType()
				+ " since it was not registered at this dispatcher.");
		for (Listener l : listeners.get(e.getEventType())) {
			l.handleEvent(e);
		}
	}

	/**
	 * Adds the specified listener. From now on, the specified listener will be
	 * notified of events with type <code>eventType</code>.
	 * @param l The listener, may not be null.
	 * @param eventType The type of {@link Event}, this must be a type that is
	 *            supported by this EventDispatcher. May not be null.
	 */
	public void addListener(Listener l, Enum<?> eventType) {
		checkArgument(l != null, "listener can not be null");
		checkArgument(eventType != null, "event type can not be null");
		checkArgument(types.contains(eventType), "A listener for type " + eventType + " is not allowed");
		listeners.put(eventType, l);
	}

	/**
	 * Adds the specified listener. From now on, the specified listener will be
	 * notified of events with one of the <code>eventTypes</code>.
	 * @param l The listener, may not be null.
	 * @param eventTypes The {@link Event} types, each type but be a type that
	 *            is supported by this EventDispatcher. May not be null and may
	 *            not be empty.
	 */
	public void addListener(Listener l, Enum<?>... eventTypes) {
		checkArgument(l != null, "listener can not be null");
		if (eventTypes == null) {
			throw new IllegalArgumentException("event types can not be null");
		}
		checkArgument(eventTypes.length > 0, "A listener has to listen to at least one event type.");
		for (Enum<?> t : eventTypes) {
			addListener(l, t);
		}
	}

	/**
	 * Removes the specified listener from all event types it is registered to.
	 * @param listener The listener to remove, may not be null.
	 */
	public void removeListenerForAllTypes(Listener listener) {
		checkArgument(listener != null, "listener can not be null");
		// store keys in intermediate set to avoid concurrent modifications
		Set<Enum<?>> keys = new HashSet<Enum<?>>(listeners.keySet());
		for (Enum<?> eventType : keys) {
			if (listeners.containsEntry(eventType, listener)) {
				removeListener(listener, eventType);
			}
		}
	}

	/**
	 * Removes the specified listener with the specified event type. From now
	 * on, <code>listener</code> will no longer be notified of new {@link Event}
	 * s with type <code>eventType</code>.
	 * @param listener The {@link Listener} to remove.
	 * @param eventType The event type.
	 */
	public void removeListener(Listener listener, Enum<?> eventType) {
		checkArgument(listener != null, "listener can not be null");
		checkArgument(eventType != null, "event type can not be null");
		checkArgument(containsListener(listener, eventType), "The listener " + listener + " for the type " + eventType
				+ " cannot be removed because it does not exist.");
		listeners.remove(eventType, listener);
	}

	/**
	 * Removes the specified listener with the specified event types. From now
	 * on, <code>listener</code> will no longer be notified of new {@link Event}
	 * s with any of <code>eventTypes</code>. If <code>eventTypes</code> is
	 * empty the listener will be removed using
	 * {@link #removeListenerForAllTypes(Listener)}.
	 * @param listener The {@link Listener} to remove.
	 * @param eventTypes The event types.
	 */
	public void removeListener(Listener listener, Enum<?>... eventTypes) {
		checkArgument(listener != null, "listener can not be null");
		if (eventTypes == null) {
			throw new IllegalArgumentException("event types can not be null");
		}
		if (eventTypes.length == 0) {
			removeListenerForAllTypes(listener);
		} else {
			for (Enum<?> e : eventTypes) {
				removeListener(listener, e);
			}
		}
	}

	/**
	 * Checks if the specified <code>listener</code> is registered as listening
	 * to <code>eventType</code>.
	 * @param listener The listener to check.
	 * @param eventType The type of event.
	 * @return <code>true</code> if the listener is listening to
	 *         <code>eventType</code>, <code>false</code> otherwise.
	 */
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
