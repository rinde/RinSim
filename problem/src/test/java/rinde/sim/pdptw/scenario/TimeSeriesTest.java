package rinde.sim.pdptw.scenario;

import static org.junit.Assert.assertEquals;
import static rinde.sim.pdptw.scenario.TimeSeries.filter;
import static rinde.sim.pdptw.scenario.TimeSeries.homogenousPoisson;
import static rinde.sim.pdptw.scenario.TimeSeries.numEvents;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import rinde.sim.pdptw.scenario.TimeSeries.TimeSeriesGenerator;

/**
 * Tests for {@link TimeSeries}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class TimeSeriesTest {

  /**
   * Test whether the number of events filter works.
   */
  @Test
  public void testFilter() {
    final TimeSeriesGenerator original = homogenousPoisson(500, 20);
    final TimeSeriesGenerator filtered = filter(original, numEvents(20));
    final RandomGenerator rng = new MersenneTwister(123L);
    for (int i = 0; i < 10; i++) {
      assertEquals(20, filtered.generate(rng.nextLong()).size());
    }
  }
}
