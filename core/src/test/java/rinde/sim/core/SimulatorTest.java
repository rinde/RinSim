/**
 * 
 */
package rinde.sim.core;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class SimulatorTest {

	Simulator simulator;

	@Before
	public void setUp() {
		simulator = new Simulator(new MersenneTwister(123), 100L);
	}

	@Test
	public void testTicks() {
		assertEquals(0L, simulator.getCurrentTime());
		TickListenerImpl tl = new TickListenerImpl();
		assertEquals(0, tl.getTickCount());
		simulator.addTickListener(tl);
		simulator.tick();
		assertEquals(100L, simulator.getCurrentTime());
		assertEquals(1, tl.getTickCount());
	}

	class TickListenerImpl implements TickListener {
		private int count = 0;

		@Override
		public void tick(long currentTime, long timeStep) {
			count++;
		}

		public int getTickCount() {
			return count;
		}

	}
}
