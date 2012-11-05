/**
 * 
 */
package rinde.sim.problem.common;

import static com.google.common.collect.Sets.newHashSet;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class TestDynamicPDPTWProblem {

	/**
	 * Checks for the absence of a creator for AddVehicleEvent.
	 */
	@Test(expected = IllegalStateException.class)
	public void testSimulate() {
		final Set<TimedEvent> events = newHashSet(new TimedEvent(DummyEvent.DUMMY, 10));
		new DynamicPDPTWProblem(new DummyScenario(events), 123).simulate();
	}

	enum DummyEvent {
		DUMMY
	}

	class DummyScenario extends DynamicPDPTWScenario {

		public DummyScenario(Set<TimedEvent> events) {
			super(events, new HashSet<Enum<?>>(java.util.Arrays.asList(DummyEvent.values())));
		}

		@Override
		public Point getMin() {
			return new Point(0, 0);
		}

		@Override
		public Point getMax() {
			return new Point(10, 10);
		}

		@Override
		public TimeWindow getTimeWindow() {
			return TimeWindow.ALWAYS;
		}

		@Override
		public long getTickSize() {
			return 1;
		}

		@Override
		public double getMaxSpeed() {
			return 1.0;
		}

	}

}
