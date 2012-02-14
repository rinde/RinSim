/**
 * 
 */
package rinde.sim.event;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

	@Before
	public void setup() {
		l1 = new Listener() {
			@Override
			public void handleEvent(Event e) {
			}
		};
		l2 = new Listener() {
			@Override
			public void handleEvent(Event e) {
			}
		};
		l3 = new Listener() {
			@Override
			public void handleEvent(Event e) {
			}
		};
	}

	Listener l1, l2, l3;

	@Test
	public void removeTest() {

		EventDispatcher disp = new EventDispatcher(EventTypes.values());
		Events api = disp.getEvents();

		assertTrue(disp.listeners.isEmpty());
		api.addListener(l1, EventTypes.values());
		assertTrue(disp.listeners.size() == 3);
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
}
