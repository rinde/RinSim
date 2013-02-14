/**
 * 
 */
package rinde.sim.event;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static rinde.sim.event.EventDispatcherTest.EventTypes.EVENT1;
import static rinde.sim.event.EventDispatcherTest.EventTypes.EVENT2;
import static rinde.sim.event.EventDispatcherTest.EventTypes.EVENT3;
import static rinde.sim.event.EventDispatcherTest.OtherEventTypes.OTHER_EVENT1;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class EventDispatcherTest {

    enum EventTypes {
        EVENT1, EVENT2, EVENT3
    }

    enum OtherEventTypes {
        OTHER_EVENT1
    }

    ListenerEventHistory l1, l2, l3;

    EventDispatcher dispatcher;
    EventAPI api;

    @Before
    public void setup() {
        l1 = new ListenerEventHistory();
        l2 = new ListenerEventHistory();
        l3 = new ListenerEventHistory();

        dispatcher = new EventDispatcher(EVENT1, EVENT2, EVENT3);
        api = dispatcher.getPublicEventAPI();
    }

    @SuppressWarnings("unused")
    @Test(expected = NullPointerException.class)
    public void constructorFail1() {
        new EventDispatcher((EventTypes[]) null);
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void constructorFail2() {
        new EventDispatcher((Set<Enum<?>>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dispatchEventFail1() {
        dispatcher.dispatchEvent(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dispatchEventFail2() {
        dispatcher.dispatchEvent(new Event(OTHER_EVENT1));
    }

    @Test
    public void dispatchEvent() {
        dispatcher.addListener(l1, EVENT1);

        final Set<Enum<?>> set = new HashSet<Enum<?>>(asList(EVENT1, EVENT2));
        api.addListener(l2, set);
        dispatcher.addListener(l3, EVENT1, EVENT2, EVENT3);

        dispatcher.dispatchEvent(new Event(EVENT2));
        assertEquals(asList(), l1.getEventTypeHistory());
        assertEquals(asList(EVENT2), l2.getEventTypeHistory());
        assertEquals(asList(EVENT2), l3.getEventTypeHistory());

        dispatcher.dispatchEvent(new Event(EVENT3));
        assertEquals(asList(), l1.getEventTypeHistory());
        assertEquals(asList(EVENT2), l2.getEventTypeHistory());
        assertEquals(asList(EVENT2, EVENT3), l3.getEventTypeHistory());

        dispatcher.dispatchEvent(new Event(EVENT1));
        assertEquals(asList(EVENT1), l1.getEventTypeHistory());
        assertEquals(asList(EVENT2, EVENT1), l2.getEventTypeHistory());
        assertEquals(asList(EVENT2, EVENT3, EVENT1), l3.getEventTypeHistory());

        dispatcher.dispatchEvent(new Event(EVENT3));
        assertEquals(asList(EVENT1), l1.getEventTypeHistory());
        assertEquals(asList(EVENT2, EVENT1), l2.getEventTypeHistory());
        assertEquals(asList(EVENT2, EVENT3, EVENT1, EVENT3), l3.getEventTypeHistory());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addListenerFail1A() {
        dispatcher.addListener(null, (Set<Enum<?>>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addListenerFail1B() {
        dispatcher.addListener(null, (Enum<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addListenerFail2A() {
        dispatcher.addListener(l1, (Set<Enum<?>>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addListenerFail2B() {
        dispatcher.addListener(l1, (Enum<?>[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addListenerFail3() {
        dispatcher.addListener(l1, OTHER_EVENT1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addListenerFail4() {
        dispatcher.addListener(null, (Enum<?>[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addListenerFail5() {
        dispatcher.addListener(l1, (Enum<?>[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addListenerFail6() {
        dispatcher.addListener(l1, new Enum<?>[] { EVENT2, null, EVENT3 });
    }

    @Test
    public void addListenerToAll() {
        dispatcher.addListener(l1, new Enum<?>[] {});
        assertTrue(dispatcher.containsListener(l1, EVENT1));
        assertTrue(dispatcher.containsListener(l1, EVENT2));
        assertTrue(dispatcher.containsListener(l1, EVENT3));
    }

    @Test
    public void removeListenerForAllTypes() {
        dispatcher.addListener(l1, EVENT1);
        dispatcher.addListener(l2, EVENT1, EVENT2);
        api.addListener(l3, EVENT1, EVENT2, EVENT3);

        assertTrue(api.containsListener(l1, EVENT1));
        assertFalse(api.containsListener(l1, EVENT2));
        assertFalse(api.containsListener(l1, EVENT3));
        assertFalse(api.containsListener(l1, OTHER_EVENT1));

        assertTrue(dispatcher.containsListener(l2, EVENT1));
        assertTrue(dispatcher.containsListener(l2, EVENT2));
        assertFalse(dispatcher.containsListener(l2, EVENT3));
        assertFalse(dispatcher.containsListener(l2, OTHER_EVENT1));

        assertTrue(dispatcher.containsListener(l3, EVENT1));
        assertTrue(dispatcher.containsListener(l3, EVENT2));
        assertTrue(dispatcher.containsListener(l3, EVENT3));
        assertFalse(dispatcher.containsListener(l3, OTHER_EVENT1));

        api.removeListener(l3, new HashSet<Enum<?>>());
        assertFalse(dispatcher.containsListener(l3, EVENT1));
        assertFalse(dispatcher.containsListener(l3, EVENT2));
        assertFalse(dispatcher.containsListener(l3, EVENT3));
        assertFalse(dispatcher.containsListener(l3, OTHER_EVENT1));

        dispatcher.removeListener(l1);
        assertFalse(dispatcher.containsListener(l1, EVENT1));
        assertFalse(dispatcher.containsListener(l1, EVENT2));
        assertFalse(dispatcher.containsListener(l1, EVENT3));
        assertFalse(dispatcher.containsListener(l1, OTHER_EVENT1));

        dispatcher.removeListener(l2);
        assertFalse(dispatcher.containsListener(l2, EVENT1));
        assertFalse(dispatcher.containsListener(l2, EVENT2));
        assertFalse(dispatcher.containsListener(l2, EVENT3));
        assertFalse(dispatcher.containsListener(l2, OTHER_EVENT1));

        dispatcher.removeListener(new ListenerEventHistory());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeListenerFail1() {
        dispatcher.removeListener(null, (Enum<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeListenerFail2() {
        dispatcher.removeListener(l1, (Enum<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeListenerFail3() {
        dispatcher.removeListener(l1, EVENT1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeListenerFail4() {
        dispatcher.getPublicEventAPI().removeListener(null, (Set<Enum<?>>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeListenerFail5() {
        dispatcher.getPublicEventAPI().removeListener(l1, (Set<Enum<?>>) null);
    }

    @Test
    public void removeListener() {
        dispatcher.addListener(l1, EVENT1);
        dispatcher.addListener(l2, EVENT3, EVENT2);
        dispatcher.addListener(l3, EVENT1, EVENT3);

        assertTrue(dispatcher.containsListener(l1, EVENT1));
        assertFalse(dispatcher.containsListener(l1, EVENT2));
        assertFalse(dispatcher.containsListener(l1, EVENT3));
        assertFalse(dispatcher.containsListener(l1, OTHER_EVENT1));

        assertFalse(dispatcher.containsListener(l2, EVENT1));
        assertTrue(dispatcher.containsListener(l2, EVENT2));
        assertTrue(dispatcher.containsListener(l2, EVENT3));
        assertFalse(dispatcher.containsListener(l2, OTHER_EVENT1));

        assertTrue(dispatcher.containsListener(l3, EVENT1));
        assertFalse(dispatcher.containsListener(l3, EVENT2));
        assertTrue(dispatcher.containsListener(l3, EVENT3));
        assertFalse(dispatcher.containsListener(l3, OTHER_EVENT1));

        dispatcher.removeListener(l2, EVENT2, EVENT3);
        assertFalse(dispatcher.containsListener(l2, EVENT1));
        assertFalse(dispatcher.containsListener(l2, EVENT2));
        assertFalse(dispatcher.containsListener(l2, EVENT3));
        assertFalse(dispatcher.containsListener(l2, OTHER_EVENT1));
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void eventConstructorFail() {
        new Event(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void eventSetIssuerFail1() {
        new Event(EVENT1).setIssuer(null);
    }

    @Test(expected = IllegalStateException.class)
    public void eventSetIssuerFail2() {
        new Event(EVENT1, "I AM ISSUER").setIssuer("I WANT TO BE ISSUER");
    }

    @Test
    public void eventSetIssuer() {
        final Event e = new Event(EVENT1, "I AM ISSUER");
        assertEquals("I AM ISSUER", e.getIssuer());

        final Event event2 = new Event(EVENT1, null);
        assertNull(event2.getIssuer());
        event2.setIssuer("I AM THE NEW ISSUER");
        assertEquals("I AM THE NEW ISSUER", event2.getIssuer());
    }

    @Test
    public void removeTest() {

        final EventDispatcher disp = new EventDispatcher(EventTypes.values());
        final EventAPI eventAPI = disp.getPublicEventAPI();

        assertTrue(disp.listeners.isEmpty());
        eventAPI.addListener(l1, EVENT1, EVENT2, EVENT3);
        assertEquals(3, disp.listeners.size());
        assertTrue(disp.listeners.containsEntry(EVENT1, l1));
        assertTrue(eventAPI.containsListener(l1, EVENT1));
        assertTrue(eventAPI.containsListener(l1, EVENT2));
        assertTrue(eventAPI.containsListener(l1, EVENT3));

        eventAPI.removeListener(l1);
        assertTrue(disp.listeners.isEmpty());
        assertFalse(eventAPI.containsListener(l1, EVENT1));
        assertFalse(eventAPI.containsListener(l1, EVENT2));
        assertFalse(eventAPI.containsListener(l1, EVENT3));

    }

}
