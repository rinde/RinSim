/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
