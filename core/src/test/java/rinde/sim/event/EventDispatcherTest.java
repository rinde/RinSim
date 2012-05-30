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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.event.pdp.StandardType;

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

	HistoryKeeper l1, l2, l3;

	EventDispatcher dispatcher;

	@Before
	public void setup() {
		l1 = new HistoryKeeper();
		l2 = new HistoryKeeper();
		l3 = new HistoryKeeper();

		StandardType.valueOf("ADD_TRUCK");

		dispatcher = new EventDispatcher(EVENT1, EVENT2, EVENT3);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void constructorFail() {
		new EventDispatcher((EventTypes[]) null);
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
		dispatcher.addListener(l2, EVENT1, EVENT2);
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
	public void addListenerFail1() {
		dispatcher.addListener(null, (Enum<?>) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addListenerFail2() {
		dispatcher.addListener(l1, (Enum<?>) null);
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
		dispatcher.addListener(l1, (new Enum<?>[] {}));
	}

	@Test(expected = IllegalArgumentException.class)
	public void removeListenerForAllTypesFail() {
		dispatcher.removeListenerForAllTypes(null);
	}

	@Test
	public void removeListenerForAllTypes() {
		dispatcher.addListener(l1, EVENT1);
		dispatcher.addListener(l2, EVENT1, EVENT2);
		dispatcher.addListener(l3, EVENT1, EVENT2, EVENT3);

		assertTrue(dispatcher.containsListener(l1, EVENT1));
		assertFalse(dispatcher.containsListener(l1, EVENT2));
		assertFalse(dispatcher.containsListener(l1, EVENT3));
		assertFalse(dispatcher.containsListener(l1, OTHER_EVENT1));

		assertTrue(dispatcher.containsListener(l2, EVENT1));
		assertTrue(dispatcher.containsListener(l2, EVENT2));
		assertFalse(dispatcher.containsListener(l2, EVENT3));
		assertFalse(dispatcher.containsListener(l2, OTHER_EVENT1));

		assertTrue(dispatcher.containsListener(l3, EVENT1));
		assertTrue(dispatcher.containsListener(l3, EVENT2));
		assertTrue(dispatcher.containsListener(l3, EVENT3));
		assertFalse(dispatcher.containsListener(l3, OTHER_EVENT1));

		dispatcher.removeListenerForAllTypes(l3);
		assertFalse(dispatcher.containsListener(l3, EVENT1));
		assertFalse(dispatcher.containsListener(l3, EVENT2));
		assertFalse(dispatcher.containsListener(l3, EVENT3));
		assertFalse(dispatcher.containsListener(l3, OTHER_EVENT1));

		dispatcher.removeListenerForAllTypes(l1);
		assertFalse(dispatcher.containsListener(l1, EVENT1));
		assertFalse(dispatcher.containsListener(l1, EVENT2));
		assertFalse(dispatcher.containsListener(l1, EVENT3));
		assertFalse(dispatcher.containsListener(l1, OTHER_EVENT1));

		dispatcher.removeListener(l2);
		assertFalse(dispatcher.containsListener(l2, EVENT1));
		assertFalse(dispatcher.containsListener(l2, EVENT2));
		assertFalse(dispatcher.containsListener(l2, EVENT3));
		assertFalse(dispatcher.containsListener(l2, OTHER_EVENT1));

		dispatcher.removeListenerForAllTypes(new HistoryKeeper());
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
		dispatcher.removeListener(null, (Enum<?>[]) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void removeListenerFail5() {
		dispatcher.removeListener(l1, (Enum<?>[]) null);
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
		Event e = new Event(EVENT1, "I AM ISSUER");
		assertEquals("I AM ISSUER", e.getIssuer());

		Event event2 = new Event(EVENT1, null);
		assertNull(event2.getIssuer());
		event2.setIssuer("I AM THE NEW ISSUER");
		assertEquals("I AM THE NEW ISSUER", event2.getIssuer());
	}

	@Test
	public void removeTest() {

		EventDispatcher disp = new EventDispatcher(EventTypes.values());
		Events api = disp.getEvents();

		assertTrue(disp.listeners.isEmpty());
		api.addListener(l1, EventTypes.values());
		assertEquals(3, disp.listeners.size());
		assertTrue(disp.listeners.containsEntry(EventTypes.EVENT1, l1));
		assertTrue(api.containsListener(l1, EventTypes.EVENT1));
		assertTrue(api.containsListener(l1, EventTypes.EVENT2));
		assertTrue(api.containsListener(l1, EventTypes.EVENT3));

		api.removeListener(l1);
		assertTrue(disp.listeners.isEmpty());
		assertFalse(api.containsListener(l1, EventTypes.EVENT1));
		assertFalse(api.containsListener(l1, EventTypes.EVENT2));
		assertFalse(api.containsListener(l1, EventTypes.EVENT3));

	}

	class HistoryKeeper implements Listener {

		private final List<Event> history;

		public HistoryKeeper() {
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
			List<Enum<?>> types = new ArrayList<Enum<?>>();
			for (Event e : history) {
				types.add(e.eventType);
			}
			return types;
		}
	}
}
