/**
 * 
 */
package rinde.sim.scenario;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math.random.MersenneTwister;
import org.apache.commons.math.random.RandomGenerator;
import org.junit.Ignore;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.event.pdp.StandardType;
import rinde.sim.util.IO;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ScenarioTest {

//	@Ignore
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
	public void testSorting() {
		List<AddObjectEvent> events = new ArrayList<AddObjectEvent>(10);
		events.add(new AddObjectEvent(0, new Point(1,0)));
		events.add(new AddObjectEvent(0, new Point(1,0)));
		events.add(new AddObjectEvent(1, new Point(1,1)));
		events.add(new AddObjectEvent(2, new Point(1,0)));
		events.add(new AddObjectEvent(3, new Point(1,2)));
		events.add(new AddObjectEvent(3, new Point(1,3)));
		events.add(new AddObjectEvent(4, new Point(2,0)));
		events.add(new AddObjectEvent(5, new Point(4,0)));
		Collections.reverse(events);
		
		Scenario s = new Scenario(events);
		
		List<TimedEvent> res = s.asList();
		assertFalse(res.equals(events));
		assertEquals(events.size(), res.size());
		
		Collections.reverse(res);
		
		assertTrue(res.equals(events));
	}
	
	@Test
	public void testCreateScenarioByCopying() {
		Scenario s = new Scenario();
		s.add(new AddObjectEvent(100, new Point(0,0)));
		s.add(new AddObjectEvent(200, new Point(0,0)));
		s.add(new AddObjectEvent(300, new Point(0,0)));
		
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

	public static Scenario randomScenario(RandomGenerator gen, int numTrucks,  List<Point> positions) {
		Scenario res = new Scenario();

		int size = positions.size();
		
		for (int i = 0; i < numTrucks; i++) {
			res.add(new AddObjectEvent(0L, positions.get(gen.nextInt(size))));
		}
		return res;
	}
}

class AddObjectEvent extends TimedEvent {

	private static final long serialVersionUID = 5946753206998904050L;
	
	public final Point pos;

	public AddObjectEvent(String[] parts) {
		this(Long.parseLong(parts[1]), Point.parsePoint(parts[2]));
	}

	public AddObjectEvent(long time, Point pos) {
		super(StandardType.ADD_TRUCK, time);
		this.pos = pos;
	}

	@Override
	public String toString() {
		return super.toString() + "|" + pos;
	}

}
