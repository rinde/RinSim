package rinde.sim.pdptw.generator.times;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.base.Predicate;
import com.google.common.collect.AbstractSequentialIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

/**
 * Implementation of a Poisson process. Two different flavors exist:
 * <ul>
 * <li>{@link #homogenous(double, int)} A homogenous Poisson process
 * characterized by a constant intensity value.</li>
 * <li>{@link #nonHomogenous(double, IntensityFunction)} A non-homogenous
 * Poisson process with a variable intensity as described by an
 * {@link IntensityFunction}. This implementation is based on the method as
 * described in [1].</li>
 * </ul>
 * <p>
 * <b>References</b>
 * <ol>
 * <li>Lewis, P.A.W. and Shedler, G.S. Simulation of nonhomogenous Poisson
 * processes by thinning. Naval Research Logistic Quarterly 26, (1979), 403–414.
 * </li>
 * </ol>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class PoissonProcess implements ArrivalTimesGenerator {
  final double length;
  final double intensity;

  PoissonProcess(double len, double intens) {
    length = len;
    intensity = intens;
  }

  /**
   * All times will be in the interval [0,length)
   * @return
   */
  public double getLength() {
    return length;
  }

  public Iterator<Double> iterator(RandomGenerator rng) {
    return new PoissonIterator(rng, intensity, length);
  }

  @Override
  public ImmutableList<Double> generate(RandomGenerator rng) {
    return ImmutableList.copyOf(iterator(rng));
  }

  public static PoissonProcess nonHomogenous(double length,
      IntensityFunction function) {
    return new NonHomogenous(length, function);
  }

  public static PoissonProcess homogenous(double length, int numEvents) {
    return new PoissonProcess(length, numEvents / length);
  }

  private static class NonHomogenous extends PoissonProcess {
    private final IntensityFunction lambd;

    NonHomogenous(double l, IntensityFunction func) {
      super(l, func.getMax());
      lambd = func;
    }

    @Override
    public Iterator<Double> iterator(RandomGenerator rng) {
      return Iterators.filter(super.iterator(rng), new NHPredicate(rng, lambd));
    }
  }

  private static class PoissonIterator extends
      AbstractSequentialIterator<Double> {
    private final ExponentialDistribution ed;
    private final double length;

    PoissonIterator(ExponentialDistribution distr, double len) {
      super(next(0d, len, distr));
      length = len;
      ed = distr;
    }

    PoissonIterator(RandomGenerator rng, double intensity, double len) {
      this(new ExponentialDistribution(rng, 1d / intensity,
          ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY), len);
    }

    @Nullable
    @Override
    protected Double computeNext(Double previous) {
      return next(previous, length, ed);
    }

    @Nullable
    static Double next(Double prev, double len, ExponentialDistribution ed) {
      final double nextVal = prev + ed.sample();
      if (nextVal < len) {
        return nextVal;
      }
      return null;
    }
  }

  private static class NHPredicate implements Predicate<Double> {
    RandomGenerator rng;
    IntensityFunction lambda;
    double lambdaMax;

    NHPredicate(RandomGenerator r, IntensityFunction l) {
      rng = r;
      lambda = l;
    }

    @Override
    public boolean apply(Double input) {
      return rng.nextDouble() <= (lambda.apply(input) / lambdaMax);
    }
  }
}
