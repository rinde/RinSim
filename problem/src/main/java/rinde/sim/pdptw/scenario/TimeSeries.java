package rinde.sim.pdptw.scenario;

import java.util.Iterator;

import javax.annotation.Nullable;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.pdptw.scenario.IntensityFunctions.IntensityFunction;

import com.google.common.base.Predicate;
import com.google.common.collect.AbstractSequentialIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

/**
 * Utilities for generating time series.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class TimeSeries {
  private TimeSeries() {}

  /**
   * Creates a homogenous Poisson process of the specified length. The intensity
   * is calculated as <code>numEvents / length</code>.
   * @param length The length of Poisson process, all generated times will be in
   *          the interval [0,length).
   * @param numEvents The number of events that will be generated (on average).
   * @return A newly constructed Poisson process {@link TimeSeriesGenerator}.
   */
  public static TimeSeriesGenerator homogenousPoisson(double length,
      int numEvents) {
    return new PoissonProcess(length, numEvents / length);
  }

  /**
   * Creates a non-homogenous Poisson process of the specified length. The
   * intensity is specified by the {@link IntensityFunction}. The non-homogenous
   * Poisson process is implemented using the thinning method as described in
   * [1].
   * <p>
   * <b>References</b>
   * <ol>
   * <li>Lewis, P.A.W. and Shedler, G.S. Simulation of nonhomogenous Poisson
   * processes by thinning. Naval Research Logistic Quarterly 26, (1979),
   * 403–414.</li>
   * </ol>
   * @param length The length of Poisson process, all generated times will be in
   *          the interval [0,length).
   * @param function The intensity function.
   * @return A newly constructed non-homogenous Poisson process
   *         {@link TimeSeriesGenerator}.
   */
  public static TimeSeriesGenerator nonHomogenousPoisson(double length,
      IntensityFunction function) {
    return new NonHomogenous(length, function);
  }

  /**
   * Generator of a time series.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface TimeSeriesGenerator {
    /**
     * Should generate a time series.
     * @param seed The random seed to use.
     * @return An immutable list of times in ascending order, may contain
     *         duplicates.
     */
    ImmutableList<Double> generate(long seed);
  }

  static class PoissonProcess implements TimeSeriesGenerator {

    /**
     * Random generator used for drawing random numbers.
     */
    protected final RandomGenerator rng;

    final double length;
    final double intensity;

    PoissonProcess(double len, double intens) {
      length = len;
      intensity = intens;
      rng = new MersenneTwister();
    }

    /**
     * All times will be in the interval [0,length)
     * @return The upper bound of the interval.
     */
    public double getLength() {
      return length;
    }

    // internal use only!
    Iterator<Double> iterator() {
      return new PoissonIterator(rng, intensity, length);
    }

    @Override
    public ImmutableList<Double> generate(long seed) {
      rng.setSeed(seed);
      return ImmutableList.copyOf(iterator());
    }
  }

  static class NonHomogenous extends PoissonProcess {
    final IntensityFunction lambd;

    NonHomogenous(double l, IntensityFunction func) {
      super(l, func.getMax());
      lambd = func;
    }

    @Override
    public Iterator<Double> iterator() {
      return Iterators.filter(super.iterator(), new NHPredicate(rng, lambd));
    }
  }

  static class PoissonIterator extends
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

  static class NHPredicate implements Predicate<Double> {
    private final RandomGenerator rng;
    private final IntensityFunction lambda;
    private final double lambdaMax;

    NHPredicate(RandomGenerator r, IntensityFunction l) {
      rng = r;
      lambda = l;
      lambdaMax = lambda.getMax();
    }

    @Override
    public boolean apply(Double input) {
      return rng.nextDouble() <= lambda.apply(input) / lambdaMax;
    }
  }

}
