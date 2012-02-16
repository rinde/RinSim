/**
 * 
 */
package rinde.sim.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		simulator.removeTickListener(tl);
		simulator.tick();
		assertEquals(1, tl.getTickCount());
	}

	@Test
	public void testTickOrder() {
		assertEquals(100L, simulator.getTimeStep());
		TickListenerImpl normal = new TickListenerImpl();
		TickListenerImpl after = new TickListenerImpl();
		simulator.addTickListener(normal);
		simulator.addAfterTickListener(after);
		simulator.tick();
		assertTrue(normal.getExecTime() < after.getExecTime());

	}

	class TickListenerImpl implements TickListener {
		private int count = 0;
		private long execTime;

		@Override
		public void tick(long currentTime, long timeStep) {
			count++;
			execTime = System.nanoTime();
		}

		public long getExecTime() {
			return execTime;
		}

		public int getTickCount() {
			return count;
		}
	}

}
