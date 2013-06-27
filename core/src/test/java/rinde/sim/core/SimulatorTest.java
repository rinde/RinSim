/**
 * 
 */
package rinde.sim.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelManagerTest.DebugModel;
import rinde.sim.core.model.rng.RandomReceiver;
import rinde.sim.core.model.time.TickListener;
import rinde.sim.core.model.time.Time;
import rinde.sim.core.model.time.TimeController;
import rinde.sim.core.model.time.TimeLapse;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class SimulatorTest {

    private final long timeStep = 100L;

    @Test(expected = IllegalArgumentException.class)
    public void noTimeModel() {
        Simulator.builder().add(new DummyModel()).build();
    }

    @Test
    public void startTest() {

        final Simulator sim = Simulator.build(10);
        final TimeManager tm = new TimeManager(10);
        sim.register(tm);
        sim.start();

        assertEquals(10, tm.ticks);
    }

    @Test
    public void testRegister() {
        final Model m1 = new DummyModel();
        final Model m2 = new DebugModel(DummyObject2.class);
        final Model m3 = new DummyModelAsTickListener(DummyObject3.class);

        final Simulator sim = Simulator.build(10, m1, m3);

        sim.register(new DummyObject());

        final DummyObjectTickListener dotl = new DummyObjectTickListener();
        // should not fail
        sim.register(dotl);

        // should fail
        try {
            sim.register(new DummyObjectSimulationUser());
            fail();
        } catch (final IllegalArgumentException e) {}
        try {
            sim.unregister(new DummyObject());
            fail();
        } catch (final IllegalArgumentException e) {}
        try {
            sim.unregister(new DummyObjectTickListener());
            fail();
        } catch (final IllegalArgumentException e) {}

        sim.unregister(dotl);
    }

    @Test
    public void addRandomModel() {
        final long seed = 123;
        final RandomGenerator ref = new MersenneTwister(seed);
        final Simulator sim = Simulator.builder().addTimeModel(10)
                .addRandomModel(seed).build();
        final DebugRandomReceiver drr = new DebugRandomReceiver();
        sim.register(drr);

        assertEquals(1, drr.calls);

        for (int i = 0; i < 100; i++) {
            assertEquals(ref.nextLong(), drr.rng.nextLong());
        }

    }

    class DebugRandomReceiver implements RandomReceiver {

        int calls = 0;

        @Nullable
        protected RandomGenerator rng;

        @Override
        public void receiveRandom(RandomGenerator r) {
            calls++;
            rng = r;
        }

    }

    class TimeManager implements TimeController, TickListener {

        protected final int max;
        protected int ticks = 0;

        @Nullable
        protected Time time;

        public TimeManager(int maxTicks) {
            max = maxTicks;
        }

        @Override
        public void tick(TimeLapse timeLapse) {
            ticks++;

        }

        @Override
        public void afterTick(TimeLapse timeLapse) {
            if (ticks >= 10) {
                time.stop();
            }

        }

        @Override
        public void receiveTime(Time t) {
            time = t;
        }

    }

    class DummyObject {}

    class DummyObject2 {}

    class DummyObject3 {}

    class DummyObjectTickListener implements TickListener {
        @Override
        public void tick(TimeLapse tl) {}

        @Override
        public void afterTick(TimeLapse tl) {}
    }

    class DummyObjectSimulationUser implements SimulatorUser {
        @Nullable
        private SimulatorAPI receivedAPI;

        @Override
        public void setSimulator(SimulatorAPI api) {
            receivedAPI = api;
        }

        public SimulatorAPI getAPI() {
            checkNotNull(receivedAPI);
            return receivedAPI;
        }
    }

    class DummyModelAsTickListener<T> extends DebugModel<T> implements
            TickListener {

        public DummyModelAsTickListener(Class<T> type) {
            super(type);
        }

        @Override
        public void tick(TimeLapse tl) {}

        @Override
        public void afterTick(TimeLapse tl) {}

    }

    class LimitingTickListener implements TickListener, TimeController {
        private final int limit;
        private int tickCount;
        @Nullable
        private Time time;

        public LimitingTickListener(int tickLimit) {
            limit = tickLimit;
            tickCount = 0;
        }

        public void reset() {
            tickCount = 0;
        }

        @Override
        public void tick(TimeLapse tl) {
            tickCount++;
        }

        @SuppressWarnings("null")
        @Override
        public void afterTick(TimeLapse tl) {
            if (tickCount >= limit) {
                assertTrue(time.isPlaying());
                if (tl.getTime() > limit * tl.getTimeStep()) {
                    time.togglePlayPause();
                }
                time.stop();
                assertFalse(time.isPlaying());
                reset();
            }
        }

        @Override
        public void receiveTime(Time t) {
            time = t;
        }
    }
    //
    // class TickListenerImpl implements TickListener {
    // private int count = 0;
    // private long execTime;
    // private long afterTime;
    //
    // @Override
    // public void tick(TimeLapse tl) {
    // count++;
    // execTime = System.nanoTime();
    // }
    //
    // public long getExecTime() {
    // return execTime;
    // }
    //
    // public long getAfterExecTime() {
    // return afterTime;
    // }
    //
    // public int getTickCount() {
    // return count;
    // }
    //
    // @Override
    // public void afterTick(TimeLapse tl) {
    // afterTime = System.nanoTime();
    // }
    // }

}
