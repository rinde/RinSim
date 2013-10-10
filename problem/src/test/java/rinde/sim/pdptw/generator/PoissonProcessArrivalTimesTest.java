/**
 * 
 */
package rinde.sim.pdptw.generator;

import static com.google.common.collect.Sets.newHashSet;
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

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
@RunWith(value = Parameterized.class)
public class PoissonProcessArrivalTimesTest {

  private final PoissonProcessArrivalTimes arrivalTimesGenerator;

  public PoissonProcessArrivalTimesTest(PoissonProcessArrivalTimes atg) {
    arrivalTimesGenerator = atg;
  }

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { /* */
    { new PoissonProcessArrivalTimes(60, 10, 1.3) }, /* */
    { new PoissonProcessArrivalTimes(60, 10, 3) }, /* */
    { new PoissonProcessArrivalTimes(180, 10, 3.66) }, /* */
    { new PoissonProcessArrivalTimes(60, 5.5, 1.46513) } /* */
    });
  }

  @Test
  public void testPoissonProcess() {
    final long scenarioLength = arrivalTimesGenerator.getScenarioLength();
    final double expectedDynamism = 1 / arrivalTimesGenerator
        .getOrdersPerAnnouncement();
    final Frequency f = new Frequency();

    final RandomGenerator rng = new MersenneTwister(0);
    for (int i = 0; i < 100; i++) {
      final List<Long> list = arrivalTimesGenerator.generate(rng);
      dynamismTest(list, expectedDynamism);
      ascendingOrderTest(list);

      // add the number of announcements
      f.addValue(newHashSet(list).size());
    }
    assertTrue(isPoissonProcess(f, arrivalTimesGenerator.getGlobalAnnouncementIntensity(), 0.001, scenarioLength));
    assertFalse(isPoissonProcess(f, arrivalTimesGenerator.getGlobalAnnouncementIntensity() + 2, 0.01, scenarioLength));
    assertFalse(isPoissonProcess(f, 0.1, 0.0001, scenarioLength));
    assertFalse(isPoissonProcess(f, 15, 0.001, scenarioLength));
    assertFalse(isPoissonProcess(f, 1000, 0.0001, scenarioLength));

  }

  @Test
  public void determinismTest() {
    final RandomGenerator outer = new MersenneTwister(123);

    for (int i = 0; i < 100; i++) {
      final long seed = outer.nextLong();
      final RandomGenerator inner = new MersenneTwister(seed);
      final List<Long> list1 = arrivalTimesGenerator.generate(inner);
      for (int j = 0; j < 100; j++) {
        inner.setSeed(seed);
        final List<Long> list2 = arrivalTimesGenerator.generate(inner);
        assertEquals(list1, list2);
      }
    }
  }

  // tests the goodness of fit of the observed frequencies to the expected
  // poisson distribution with specified intensity.
  // we use a confidence interval of 0.1% which means that we only return
  // false
  // in case we know 99.9% sure that the observations do not match the
  // expected
  // distribution.
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
      double confidence, long scenarioLength) {
    final double lengthFactor = scenarioLength / 60d;
    final PoissonDistribution pd = new PoissonDistribution(intensity
        * lengthFactor);
    final long observed[] = new long[observations.getUniqueCount()];
    final double[] expected = new double[observations.getUniqueCount()];

    final Iterator<?> it = observations.valuesIterator();
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

  /**
   * This tests whether the measured dynamism is as close as possible to the
   * expected dynamism.
   * @param arrivalTimes
   * @param expectedDynamism
   */
  static void dynamismTest(List<Long> arrivalTimes, double expectedDynamism) {

    final int announcements = newHashSet(arrivalTimes).size();

    final int orders = arrivalTimes.size();

    final double actualDynamism = announcements / (double) orders;
    final double dynUp = (announcements + 1) / (double) orders;
    final double dynDown = (announcements - 1) / (double) orders;

    final double actualDist = Math.abs(actualDynamism - expectedDynamism);
    final double distUp = Math.abs(dynUp - expectedDynamism);
    final double distDown = Math.abs(dynDown - expectedDynamism);
    assertTrue(announcements + " " + actualDist + " " + distUp + " " + distDown, actualDist < distUp
        && actualDist < distDown);
  }

  static void ascendingOrderTest(List<Long> arrivalTimes) {
    long prev = 0;
    for (final long l : arrivalTimes) {
      assertTrue(prev <= l);
      prev = l;
    }
  }

}
