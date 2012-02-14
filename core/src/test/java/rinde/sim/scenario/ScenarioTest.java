/**
 * 
 */
package rinde.sim.scenario;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

	@Test
	public void testReadWrite() {
		List<Point> points = new ArrayList<Point>();
		for (int i = 0; i < 1000; i++) {
			points.add(new Point(Math.random(), Math.random()));
		}

		Scenario original = randomScenario(new MersenneTwister(123), 10, 20, 1000, points);

		IO.serialize(original, "files/original.scen");
		Scenario copied = IO.deserialize("files/original.scen", Scenario.class);

		assertEquals(original, copied);

		IO.serialize(copied, "files/copied.scen");
		assertEquals(IO.deserialize("files/original.scen", Scenario.class), IO.deserialize("files/copied.scen", Scenario.class));

		(new File("files/original.scen")).delete();
		(new File("files/copied.scen")).delete();
	}

	public static Scenario randomScenario(RandomGenerator gen, int numTrucks, int numPackages, long lastPackageDispatchTime, List<Point> positions) {
		List<SerializedScenarioEvent> events = new ArrayList<SerializedScenarioEvent>();

		for (int i = 0; i < numTrucks; i++) {
			events.add(new AddObjectEvent(0L, positions.get(gen.nextInt(positions.size()))));
		}
		Collections.sort(events);
		return new Scenario(events);
	}

}

class AddObjectEvent extends SerializedScenarioEvent {
	private static final long serialVersionUID = 5946753206998904050L;
	public final Point pos;

	public AddObjectEvent(String[] parts) {
		this(Long.parseLong(parts[1]), Point.parsePoint(parts[2]));
	}

	public AddObjectEvent(long time, Point pos) {
		super(time);
		this.pos = pos;
	}

	@Override
	public String toString() {
		return super.toString() + "|" + pos;
	}

}
