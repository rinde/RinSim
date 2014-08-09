package com.github.rinde.rinsim.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;

/**
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ListenerEventHistory implements Listener {

    private final List<Event> history;

    public ListenerEventHistory() {
        history = new ArrayList<Event>();
    }

    @Override
    public void handleEvent(Event e) {
        history.add(e);
        e.toString();
    }

    public List<Event> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public List<Enum<?>> getEventTypeHistory() {
        final List<Enum<?>> types = new ArrayList<Enum<?>>();
        for (final Event e : history) {
            types.add(e.eventType);
        }
        return types;
    }

    public void clear() {
        history.clear();
    }
}
