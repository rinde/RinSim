/**
 * 
 */
package rinde.sim.pdptw.generator.times;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.pdptw.generator.ScenarioGenerator;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
@RunWith(value = Parameterized.class)
public class HomogenousPoissonProcessTest {

  private final PoissonProcess poisson;

  /**
   * @param atg The process to test.
   */
  public HomogenousPoissonProcessTest(PoissonProcess atg) {
    poisson = atg;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { PoissonProcess.homogenous(60d, 10) },
        { PoissonProcess.homogenous(60d, 100) },
        { PoissonProcess.homogenous(180d, 10) },
        { PoissonProcess.homogenous(180d, 100) }
    });
  }

  /**
   * Tests whether the Poisson process has a Poisson distribution.
   */
  @Test
  public void testPoissonDistribution() {
    final double length = poisson.getLength();
    final Frequency f = new Frequency();

    final RandomGenerator rng = new MersenneTwister(0);
    for (int i = 0; i < 100; i++) {
      final List<Long> list = ScenarioGenerator.convert(poisson
          .generate(rng));
      ascendingOrderTest(list);
      // add the number of announcements
      f.addValue(list.size());
    }
    assertTrue(isPoissonProcess(f, poisson.intensity, length, 0.001));
    assertFalse(isPoissonProcess(f, 2, length, 0.01));
    assertFalse(isPoissonProcess(f, 0.1, length, 0.0001));
    assertFalse(isPoissonProcess(f, 15, length, 0.001));
    assertFalse(isPoissonProcess(f, 1000, length, 0.0001));
  }

  /**
   * Tests determinism of arrival times generators, given the same random number
   * generator and seed they should always return the same sequence.
   */
  @Test
  public void determinismTest() {
    final RandomGenerator outer = new MersenneTwister(123);

    for (int i = 0; i < 100; i++) {
      final long seed = outer.nextLong();
      final RandomGenerator inner = new MersenneTwister(seed);
      final List<Double> list1 = poisson.generate(inner);
      for (int j = 0; j < 100; j++) {
        inner.setSeed(seed);
        final List<Double> list2 = poisson.generate(inner);
        assertEquals(list1, list2);
      }
    }
  }

  /**
   * Checks whether the observations conform to a Poisson process with the
   * specified intensity. Uses a chi square test with the specified confidence.
   * The null hypothesis is that the observations are the result of a poisson
   * process.
   * @param observations
   * @param intensity
   * @param confidence
   * @return <code>true</code> if the observations
   */
  static boolean isPoissonProcess(Frequency observations, double intensity,
      double length, double confidence) {
    final PoissonDistribution pd = new PoissonDistribution(length * intensity);

    final Iterator<?> it = observations.valuesIterator();
    final long[] observed = new long[observations.getUniqueCount()];
    final double[] expected = new double[observations.getUniqueCount()];

    int index = 0;
    while (it.hasNext()) {
      final Long l = (Long) it.next();
      observed[index] = observations.getCount(l);
      expected[index] = pd.probability(l.intValue())
          * observations.getSumFreq();
      if (expected[index] == 0) {
        return false;
      }
      index++;
    }
    final double chi = TestUtils.chiSquareTest(expected, observed);
    return !(chi < confidence);
  }

  static void ascendingOrderTest(List<? extends Number> arrivalTimes) {
    Number prev = 0;
    for (final Number l : arrivalTimes) {
      assertTrue(prev.doubleValue() <= l.doubleValue());
      prev = l;
    }
  }
}
