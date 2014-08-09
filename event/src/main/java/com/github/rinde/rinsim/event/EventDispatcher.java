/**
 * 
 */
package com.github.rinde.rinsim.event;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
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
  protected final ImmutableSet<Enum<?>> supportedTypes;

  /**
   * The 'public' api of this dispatcher. Public in this context means API that
   * is intended for <i>users</i> of the dispatcher, that is, classes that want
   * to be notified of events.
   */
  protected final PublicEventAPI publicAPI;

  /**
   * Creates a new {@link EventDispatcher} instance which is capable of
   * dispatching any {@link Event} with a <code>type</code> attribute that is
   * one of <code>eventTypes</code>.
   * @param supportedEventTypes The types of events this EventDispatcher
   *          supports.
   */
  public EventDispatcher(Set<Enum<?>> supportedEventTypes) {
    listeners = LinkedHashMultimap.create();
    supportedTypes = ImmutableSet.copyOf(supportedEventTypes);
    publicAPI = new PublicEventAPI(this);
  }

  /**
   * Creates a new {@link EventDispatcher} instance which is capable of
   * dispatching any {@link Event} with a <code>type</code> attribute that is
   * one of <code>eventTypes</code>.
   * @param supportedEventTypes The types of events this EventDispatcher
   *          supports.
   */
  public EventDispatcher(Enum<?>... supportedEventTypes) {
    this(new HashSet<Enum<?>>(asList(supportedEventTypes)));
  }

  /**
   * Dispatch an event. Notifies all listeners that are listening for this type
   * of event.
   * @param e The event to be dispatched, only events with a supported type can
   *          be dispatched.
   */
  public void dispatchEvent(Event e) {
    checkArgument(
        supportedTypes.contains(e.getEventType()),
        "Cannot dispatch an event of type %s since it was not registered at this dispatcher.",
        e.getEventType());
    for (final Listener l : listeners.get(e.getEventType())) {
      l.handleEvent(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(Listener listener, Enum<?>... eventTypes) {
    addListener(listener, newHashSet(eventTypes), eventTypes.length == 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(Listener listener, Set<Enum<?>> eventTypes) {
    addListener(listener, eventTypes, false);
  }

  /**
   * Adds the specified listener. From now on, the specified listener will be
   * notified of events with one of the <code>eventTypes</code>. If
   * <code>eventTypes</code> is empty, the listener will be notified of no
   * events. If <code>all</code> is <code>true</code> the value for
   * <code>eventTypes</code> is ignored and the listener is registered for
   * <i>all</i> events. Otherwise, if <code>all</code> is <code>false</code> the
   * listener is only registered for the event types in <code>eventTypes</code>.
   * @param listener The listener to register.
   * @param eventTypes The event types to listen to.
   * @param all Indicates whether <code>eventTypes</code> is used or if the
   *          listener is registered to all event types.
   */
  protected void addListener(Listener listener, Set<Enum<?>> eventTypes,
      boolean all) {
    final Set<Enum<?>> theTypes = all ? supportedTypes : eventTypes;
    for (final Enum<?> eventType : theTypes) {
      checkArgument(eventType != null, "event type to add can not be null");
      checkArgument(supportedTypes.contains(eventType),
          "A listener for type %s is not allowed.", eventType);
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
    if (eventTypes.isEmpty()) {
      // remove all store keys in intermediate set to avoid concurrent
      // modifications
      final Set<Enum<?>> keys = new HashSet<Enum<?>>(listeners.keySet());
      for (final Enum<?> eventType : keys) {
        if (listeners.containsEntry(eventType, listener)) {
          removeListener(listener, eventType);
        }
      }
    } else {
      for (final Enum<?> eventType : eventTypes) {
        checkArgument(eventType != null, "event type to remove can not be null");
        checkArgument(
            containsListener(listener, eventType),
            "The listener %s for the type %s cannot be removed because it does not exist.",
            listener, eventType);
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
   * This method returns the public {@link EventAPI} instance associated to this
   * {@link EventDispatcher}. This instance can be made publicly available to
   * classes outside the scope of the events. Through this instance listeners
   * can be added and removed to this {@link EventDispatcher}.
   * @return A wrapper for {@link EventDispatcher}, only shows the methods which
   *         should be allowed to be called outside of the dispatcher's parent.
   */
  public EventAPI getPublicEventAPI() {
    return publicAPI;
  }

  static class PublicEventAPI implements EventAPI {
    private final EventDispatcher ref;

    PublicEventAPI(EventDispatcher ed) {
      ref = ed;
    }

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
    public void removeListener(Listener listener, Set<Enum<?>> eventTypes) {
      ref.removeListener(listener, eventTypes);
    }

    @Override
    public boolean containsListener(Listener l, Enum<?> eventType) {
      return ref.containsListener(l, eventType);
    }
  }
}
