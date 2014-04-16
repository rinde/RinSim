package rinde.sim.pdptw.generator.times;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.pdptw.generator.times.PoissonProcess.NHPredicate;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

/**
 * Tests the the predicate part of the {@link PoissonProcess}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
@RunWith(Parameterized.class)
public class PoissonIntensityTest {

  private final IntensityFunction intensityFunction;

  /**
   * @param inFunc The intensity function to test the predicate with.
   */
  public PoissonIntensityTest(IntensityFunction inFunc) {
    intensityFunction = inFunc;
  }

  /**
   * @return The test configs.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { SineIntensity.builder().area(10).period(20).build() },
        { SineIntensity.builder().area(10).height(.5).period(20).build() },
        { SineIntensity.builder().area(10).height(-.5).period(20).build() },
        { SineIntensity.builder().area(10).phaseShift(0).period(20).build() },
        { SineIntensity.builder().area(10).height(1).period(20).build() }
    });
  }

  /**
   * Tests whether the predicate approximates the intensity function when a
   * large number of runs is done.
   */
  @Test
  public void intensityApproximationPredicateTest() {
    final RandomGenerator rng = new MersenneTwister(123);
    final Predicate<Double> pred = new NHPredicate(rng, intensityFunction);

    final ImmutableList.Builder<Double> b = ImmutableList.builder();
    for (int i = 0; i < 100; i++) {
      b.add(new Double(i));
    }
    final ImmutableList<Double> doubles = b.build();
    final Multiset<Double> ms = TreeMultiset.create();
    final int repetitions = 10000;
    for (int i = 0; i < repetitions; i++) {
      ms.addAll(Collections2.filter(doubles, pred));
    }
    for (final Multiset.Entry<Double> entry : ms.entrySet()) {
      final double prob = intensityFunction.apply(entry.getElement())
          / intensityFunction.getMax();
      final double observation = entry.getCount() / (double) repetitions;
      assertEquals(prob, observation, 0.015);
    }
  }

  /**
   * Tests whether the Poisson process (crudely) approximates the intensity
   * function when a large number of runs is done.
   */
  @Test
  public void intensityApproximationPoissonProcessTest() {
    final RandomGenerator rng = new MersenneTwister(123);

    final PoissonProcess pp = PoissonProcess.nonHomogenous(100d,
        intensityFunction);

    final Multiset<Double> ms = TreeMultiset.create();
    final int repetitions = 10000;
    for (int i = 0; i < repetitions; i++) {
      final List<Double> times = pp.generate(rng.nextLong());
      for (final Double d : times) {
        ms.add(new Double(Math.ceil(d)));
      }
    }
    for (final Multiset.Entry<Double> entry : ms.entrySet()) {
      final double exp = IntensityFunctions.areaByIntegration(
          intensityFunction,
          entry.getElement() - 1d, entry.getElement());

      final double observation = entry.getCount() / (double) repetitions;
      assertEquals(exp, observation, 0.05);
    }
  }
}
