/**
 * 
 */
package rinde.sim.scenario;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static rinde.sim.event.pdp.StandardType.ADD_TRUCK;
import static rinde.sim.event.pdp.StandardType.REMOVE_TRUCK;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.math.random.MersenneTwister;
import org.apache.commons.math.random.RandomGenerator;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.util.IO;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ScenarioTest {

	// @Ignore
	@Test
	public void testReadWrite() {
		List<Point> points = new ArrayList<Point>();
		for (int i = 0; i < 1000; i++) {
			points.add(new Point(Math.random(), Math.random()));
		}

		Scenario original = randomScenario(new MersenneTwister(123), 10, points);

		assertEquals(10, original.size());

		IO.serialize(original, "files/original.scen");
		Scenario copied = IO.deserialize("files/original.scen", Scenario.class);

		assertEquals(10, copied.size());

		assertEquals(original, copied);

		IO.serialize(copied, "files/copied.scen");
		assertEquals(IO.deserialize("files/original.scen", Scenario.class), IO.deserialize("files/copied.scen", Scenario.class));

		(new File("files/original.scen")).delete();
		(new File("files/copied.scen")).delete();
	}

	@Test
	public void equals() {
		List<TimedEvent> events1 = newArrayList(new TimedEvent(ADD_TRUCK, 0));
		List<TimedEvent> events2 = newArrayList(new TimedEvent(ADD_TRUCK, 0));
		List<TimedEvent> events3 = newArrayList(new TimedEvent(ADD_TRUCK, 1));
		List<TimedEvent> events4 = newArrayList(new TimedEvent(ADD_TRUCK, 1), new TimedEvent(ADD_TRUCK, 2));

		assertFalse(new Scenario(events1).equals(new Object()));
		assertTrue(new Scenario(events1).equals(new Scenario(events2)));
		assertFalse(new Scenario(events1).equals(new Scenario(events3)));
		assertFalse(new Scenario(events1).equals(new Scenario(events4)));
	}

	@Test
	public void testSorting() {
		List<TimedEvent> events = new ArrayList<TimedEvent>(10);
		events.add(new AddObjectEvent(0, new Point(1, 0)));
		events.add(new AddObjectEvent(0, new Point(1, 0)));
		events.add(new AddObjectEvent(1, new Point(1, 1)));
		events.add(new AddObjectEvent(2, new Point(1, 0)));
		events.add(new AddObjectEvent(3, new Point(1, 2)));
		events.add(new AddObjectEvent(3, new Point(1, 3)));
		events.add(new AddObjectEvent(4, new Point(2, 0)));
		events.add(new AddObjectEvent(5, new Point(4, 0)));
		Collections.reverse(events);

		Scenario s = new Scenario(events);

		List<TimedEvent> res = s.asList();
		assertFalse(res.equals(events));
		assertEquals(events.size(), res.size());

		Collections.reverse(res);

		assertTrue(res.equals(events));
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void constructorFail1() {
		new Scenario(null, null);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void constructorFail2() {
		List<TimedEvent> events = newArrayList();
		new Scenario(events, null);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void constructorFail3() {
		new Scenario(asList(new TimedEvent(ADD_TRUCK, 1L)), null);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void constructorFail4() {
		new Scenario(asList(new TimedEvent(ADD_TRUCK, 1L)), new HashSet<Enum<?>>());
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void constructorFail5() {
		new Scenario((Collection<TimedEvent>) null);
	}

	@Test
	public void testCreateScenarioByCopying() {

		Scenario s = new ScenarioBuilder(ADD_TRUCK).addEvent(new AddObjectEvent(100, new Point(0, 0)))
				.addEvent(new AddObjectEvent(200, new Point(0, 0))).addEvent(new AddObjectEvent(300, new Point(0, 0)))
				.build();

		assertEquals(3, s.asList().size());

		Scenario s2 = new Scenario(s);

		assertEquals(3, s.asList().size());
		assertEquals(3, s2.asList().size());

		assertEquals(s.peek(), s2.peek());
		final TimedEvent sP0 = s.poll();

		assertEquals(2, s.asList().size());
		assertEquals(3, s2.asList().size());

		final TimedEvent s2P0 = s2.poll();

		assertEquals(2, s.asList().size());
		assertEquals(2, s2.asList().size());

		assertEquals(sP0, s2P0);

	}

	@Test
	public void timedEventEquals() {
		assertFalse(new AddObjectEvent(10, new Point(10, 0)).equals(new TimedEvent(ADD_TRUCK, 10)));
		assertFalse(new TimedEvent(ADD_TRUCK, 10).equals(null));
		assertFalse(new TimedEvent(ADD_TRUCK, 10).equals(new TimedEvent(REMOVE_TRUCK, 10)));
		assertTrue(new TimedEvent(REMOVE_TRUCK, 10).equals(new TimedEvent(REMOVE_TRUCK, 10)));
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void timedEventConstructorFail() {
		new TimedEvent(ADD_TRUCK, -1);
	}

	public static Scenario randomScenario(RandomGenerator gen, int numTrucks, List<Point> positions) {
		ScenarioBuilder res = new ScenarioBuilder(ADD_TRUCK);
		int size = positions.size();
		for (int i = 0; i < numTrucks; i++) {
			res.addEvent(new AddObjectEvent(0L, positions.get(gen.nextInt(size))));
		}
		return res.build();
	}
}

class AddObjectEvent extends TimedEvent {

	private static final long serialVersionUID = 5946753206998904050L;

	public final Point pos;

	public AddObjectEvent(String[] parts) {
		this(Long.parseLong(parts[1]), Point.parsePoint(parts[2]));
	}

	public AddObjectEvent(long pTime, Point pPos) {
		super(ADD_TRUCK, pTime);
		pos = pPos;
		hashCode();
		toString();
	}

	@Override
	public String toString() {
		return super.toString() + "|" + pos;
	}

}
