/**
 * 
 */
package rinde.sim.core.model.time;

import org.junit.Test;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class TimeModelTest {

    @Test
    public void test() {

        final TimeModel tm = new TimeModel(123);

        System.out.println(tm.getModelLinks());

    }

    @Test
    public void testTicks() {
        assertEquals(0L, simulator.getCurrentTime());
        final TickListenerImpl tl = new TickListenerImpl();
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
        final TickListenerImpl normal = new TickListenerImpl();
        simulator.addTickListener(normal);
        simulator.tick();
        assertTrue(normal.getExecTime() < normal.getAfterExecTime());
    }
}
