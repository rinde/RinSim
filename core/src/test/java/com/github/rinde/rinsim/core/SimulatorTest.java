package com.github.rinde.rinsim.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.SimulatorUser;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.google.common.collect.Sets;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class SimulatorTest {

  private final Measure<Long, Duration> timeStep = Measure
      .valueOf(100L, SI.SECOND);
  private Simulator simulator;

  @Before
  public void setUp() {
    simulator = new Simulator(new MersenneTwister(123), timeStep);
    Simulator.SimulatorEventType.valueOf("STOPPED");// just for test coverage of
                                                    // the
    // enum
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

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterTooEarly() {
    simulator.register(new DummyObject());
  }

  @Test(expected = IllegalStateException.class)
  public void testRegisterModelTooLate() {
    simulator.configure();
    simulator.register(new DummyModel());
  }

  @Test
  public void testRegister() {
    assertTrue(simulator.getModels().isEmpty());
    final DummyModel m1 = new DummyModel();
    final DummyModel m2 = new DummyModel();
    final DummyModelAsTickListener m3 = new DummyModelAsTickListener();
    assertTrue(simulator.register(m1));
    assertFalse(simulator.register((Object) m1));
    assertTrue(simulator.register(m2));
    assertTrue(simulator.register(m3));
    assertFalse(simulator.register(m3));

    assertEquals(Arrays.asList(m1, m2, m3), simulator.getModels());
    simulator.configure();

    assertTrue(simulator.register(new DummyObject()));

    final DummyObjectTickListener dotl = new DummyObjectTickListener();
    assertFalse(simulator.register(dotl));

    assertEquals(Sets.newHashSet(m3, dotl), simulator.getTickListeners());

    final DummyObjectSimulationUser dosu = new DummyObjectSimulationUser();
    assertFalse(simulator.register(dosu));
    assertEquals(simulator, dosu.getAPI());

    simulator.unregister(new DummyObject());
    simulator.unregister(new DummyObjectTickListener());

  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnregisterModel() {
    simulator.unregister(new DummyModel());
  }

  @Test(expected = IllegalStateException.class)
  public void testUnregisterTooEarly() {
    simulator.unregister(new Object());
  }

  @Test
  public void testStartWithoutConfiguring() {
    final LimitingTickListener ltl = new LimitingTickListener(simulator, 3);
    simulator.addTickListener(ltl);
    simulator.start();
    assertEquals(3 * timeStep.getValue(), simulator.getCurrentTime());
  }

  @Test
  public void testStart() {
    simulator.configure();
    final LimitingTickListener ltl = new LimitingTickListener(simulator, 3);
    simulator.addTickListener(ltl);
    simulator.start();
    assertEquals(3 * timeStep.getValue(), simulator.getCurrentTime());

    simulator.unregister(new Object());
    simulator.togglePlayPause();
    assertEquals(6 * timeStep.getValue(), simulator.getCurrentTime());
    simulator.resetTime();
    assertEquals(0, simulator.getCurrentTime());
  }

  @Test
  public void testGetRnd() {
    assertNotNull(simulator.getRandomGenerator());
  }

  class DummyObject {}

  class DummyObjectTickListener implements TickListener {
    @Override
    public void tick(TimeLapse tl) {}

    @Override
    public void afterTick(TimeLapse tl) {}
  }

  class DummyObjectSimulationUser implements SimulatorUser {
    private SimulatorAPI receivedAPI;

    @Override
    public void setSimulator(SimulatorAPI api) {
      receivedAPI = api;
    }

    public SimulatorAPI getAPI() {
      return receivedAPI;
    }
  }

  class DummyModelAsTickListener extends DummyModel implements TickListener {

    @Override
    public void tick(TimeLapse tl) {}

    @Override
    public void afterTick(TimeLapse tl) {}

  }

  class LimitingTickListener implements TickListener {
    private final int limit;
    private int tickCount;
    private final Simulator sim;

    public LimitingTickListener(Simulator s, int tickLimit) {
      sim = s;
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

    @Override
    public void afterTick(TimeLapse tl) {
      if (tickCount >= limit) {
        assertTrue(sim.isPlaying());
        if (tl.getTime() > limit * tl.getTimeStep()) {
          sim.togglePlayPause();
        }
        sim.stop();
        assertFalse(sim.isPlaying());
        reset();
      }
    }
  }

  class TickListenerImpl implements TickListener {
    private int count = 0;
    private long execTime;
    private long afterTime;

    @Override
    public void tick(TimeLapse tl) {
      count++;
      execTime = System.nanoTime();
    }

    public long getExecTime() {
      return execTime;
    }

    public long getAfterExecTime() {
      return afterTime;
    }

    public int getTickCount() {
      return count;
    }

    @Override
    public void afterTick(TimeLapse tl) {
      afterTime = System.nanoTime();
    }
  }

}
