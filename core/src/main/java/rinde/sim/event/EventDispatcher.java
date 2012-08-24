/**
 * 
 */
package rinde.sim.event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Basic event dispatcher for easily dispatching {@link Event}s to
 * {@link Listener}s. It provides methods for dispatching events and removing
 * and adding of listeners.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class EventDispatcher implements EventAPI {

    /**
     * A map of event types to registered {@link Listener}s.
     */
    protected final Multimap<Enum<?>, Listener> listeners;

    /**
     * The set of event types that this event dispatcher supports.
     */
    protected final Set<Enum<?>> supportedTypes;

    /**
     * Creates a new {@link EventDispatcher} instance which is capable of
     * dispatching any {@link Event} with a <code>type</code> attribute that is
     * one of <code>eventTypes</code>.
     * @param supportedEventTypes The types of events this EventDispatcher
     *            supports.
     */
    public EventDispatcher(Set<Enum<?>> supportedEventTypes) {
        checkArgument(supportedEventTypes != null, "event types can not be null");
        listeners = LinkedHashMultimap.create();
        supportedTypes = newHashSet(supportedEventTypes);
    }

    /**
     * Creates a new {@link EventDispatcher} instance which is capable of
     * dispatching any {@link Event} with a <code>type</code> attribute that is
     * one of <code>eventTypes</code>.
     * @param supportedEventTypes The types of events this EventDispatcher
     *            supports.
     */
    public EventDispatcher(Enum<?>... supportedEventTypes) {
        this(new HashSet<Enum<?>>(asList(supportedEventTypes)));
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
        checkArgument(supportedTypes.contains(e.getEventType()), "Cannot dispatch an event of type "
                + e.getEventType()
                + " since it was not registered at this dispatcher.");
        for (final Listener l : listeners.get(e.getEventType())) {
            l.handleEvent(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(Listener listener, Enum<?>... eventTypes) {
        checkArgument(eventTypes != null, "event types can not be null");
        addListener(listener, newHashSet(eventTypes), eventTypes.length == 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(Listener listener, Set<Enum<?>> eventTypes) {
        addListener(listener, eventTypes, false);
    }

    protected void addListener(Listener listener, Set<Enum<?>> eventTypes,
            boolean all) {
        checkArgument(listener != null, "listener can not be null");
        if (eventTypes == null) {
            throw new IllegalArgumentException("event types can not be null");
        }
        final Set<Enum<?>> theTypes = all ? supportedTypes : eventTypes;
        for (final Enum<?> eventType : theTypes) {
            checkArgument(eventType != null, "event type can not be null");
            checkArgument(supportedTypes.contains(eventType), "A listener for type "
                    + eventType + " is not allowed");
            listeners.put(eventType, listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(Listener listener, Enum<?>... eventTypes) {
        removeListener(listener, newHashSet(eventTypes));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(Listener listener, Set<Enum<?>> eventTypes) {
        checkArgument(listener != null, "listener can not be null");
        if (eventTypes == null) {
            throw new IllegalArgumentException("event types can not be null");
        }

        if (eventTypes.isEmpty()) {
            // remove all
            // store keys in intermediate set to avoid concurrent modifications
            final Set<Enum<?>> keys = new HashSet<Enum<?>>(listeners.keySet());
            for (final Enum<?> eventType : keys) {
                if (listeners.containsEntry(eventType, listener)) {
                    removeListener(listener, eventType);
                }
            }
        } else {
            for (final Enum<?> eventType : eventTypes) {
                checkArgument(eventType != null, "event type can not be null");
                checkArgument(containsListener(listener, eventType), "The listener "
                        + listener
                        + " for the type "
                        + eventType
                        + " cannot be removed because it does not exist.");
                listeners.remove(eventType, listener);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsListener(Listener listener, Enum<?> eventType) {
        return listeners.containsEntry(eventType, listener);
    }

    /**
     * This method returns an {@link EventAPI} instance which can be made
     * publicly available to classes outside the scope of the events. Through
     * this instance listeners can be added and removed to this
     * {@link EventDispatcher}.
     * @return A wrapper for {@link EventDispatcher}, only shows the methods
     *         which should be allowed to be called outside of the dispatcher's
     *         parent.
     */
    public EventAPI getEventAPI() {
        final EventDispatcher ref = this;
        return new EventAPI() {
            @Override
            public void addListener(Listener l, Enum<?>... eventTypes) {
                ref.addListener(l, eventTypes);
            }

            @Override
            public void addListener(Listener listener, Set<Enum<?>> eventTypes) {
                ref.addListener(listener, eventTypes);
            }

            @Override
            public void removeListener(Listener l, Enum<?>... eventTypes) {
                ref.removeListener(l, eventTypes);
            }

            @Override
            public void removeListener(Listener listener,
                    Set<Enum<?>> eventTypes) {
                ref.removeListener(listener, eventTypes);
            }

            @Override
            public boolean containsListener(Listener l, Enum<?> eventType) {
                return ref.containsListener(l, eventType);
            }
        };
    }
}
