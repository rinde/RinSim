package com.github.rinde.rinsim.scenario.generator;

import static com.github.rinde.rinsim.scenario.generator.TimeSeries.filter;
import static com.github.rinde.rinsim.scenario.generator.TimeSeries.homogenousPoisson;
import static com.github.rinde.rinsim.scenario.generator.TimeSeries.numEvents;
import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import com.github.rinde.rinsim.scenario.generator.TimeSeries;
import com.github.rinde.rinsim.scenario.generator.TimeSeries.TimeSeriesGenerator;

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
