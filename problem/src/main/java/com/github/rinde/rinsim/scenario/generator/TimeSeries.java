package com.github.rinde.rinsim.scenario.generator;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.scenario.generator.IntensityFunctions.IntensityFunction;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractSequentialIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;

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
    checkArgument(length > 0d);
    checkArgument(numEvents > 0);
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
    checkArgument(length > 0d);
    checkArgument(function.getMax() > 0d);
    return new NonHomogenous(length, function);
  }

  /**
   * Creates a non-homogenous Poisson process of the specified length. The
   * intensity is specified by the {@link StochasticSupplier}. Each time
   * {@link TimeSeriesGenerator#generate(long)} is called, a new
   * {@link IntensityFunction} is requested from the {@link StochasticSupplier}.
   * The non-homogenous Poisson process is implemented using the thinning method
   * as described in [1].
   * <p>
   * <b>References</b>
   * <ol>
   * <li>Lewis, P.A.W. and Shedler, G.S. Simulation of nonhomogenous Poisson
   * processes by thinning. Naval Research Logistic Quarterly 26, (1979),
   * 403–414.</li>
   * </ol>
   * @param length The length of Poisson process, all generated times will be in
   *          the interval [0,length).
   * @param functionSupplier The intensity function supplier.
   * @return A newly constructed non-homogenous Poisson process
   *         {@link TimeSeriesGenerator}.
   */
  public static TimeSeriesGenerator nonHomogenousPoisson(double length,
      StochasticSupplier<IntensityFunction> functionSupplier) {
    checkArgument(length > 0d);
    return new SuppliedNonHomogenous(length, functionSupplier);
  }

  /**
   * Creates a {@link TimeSeriesGenerator} using a uniform distribution for the
   * inter arrival times of events.
   * @param length The length of the time series, all generated times will be in
   *          the interval [0,length).
   * @param numEvents The total number of events in the time series (on
   *          average).
   * @param maxDeviation The maximum deviation from the mean for the uniform
   *          distribution.
   * @return A {@link TimeSeriesGenerator} based on a uniform distribution.
   */
  public static TimeSeriesGenerator uniform(double length, int numEvents,
      double maxDeviation) {
    checkArgument(length > 0d);
    checkArgument(numEvents > 0);
    checkArgument(maxDeviation > 0d);
    final double average = length / numEvents;
    return new UniformTimeSeries(length, average,
        StochasticSuppliers.constant(maxDeviation));
  }

  /**
   * Creates a {@link TimeSeriesGenerator} using a uniform distribution for the
   * inter arrival times of events. The spread of the uniform distribution is
   * defined by the <code>maxDeviation</code> {@link StochasticSupplier}, this
   * means that each time a time series is generated a different max deviation
   * settings is used.
   * @param length The length of the time series, all generated times will be in
   *          the interval [0,length).
   * @param numEvents The total number of events in the time series (on
   *          average).
   * @param maxDeviation A supplier that is used for max deviation values.
   * @return A {@link TimeSeriesGenerator} based on a uniform distribution, each
   *         time series that is generated has a different max deviation drawn
   *         from the supplier.
   */
  public static TimeSeriesGenerator uniform(double length, int numEvents,
      StochasticSupplier<Double> maxDeviation) {
    checkArgument(length > 0d);
    checkArgument(numEvents > 0);
    final double average = length / numEvents;
    return new UniformTimeSeries(length, average, StochasticSuppliers.checked(
        maxDeviation, Range.atLeast(0d)));
  }

  /**
   * Creates a {@link TimeSeriesGenerator} that uses a truncated normal
   * distribution for the inter arrival times of events. The normal distribution
   * is truncated at a lower bound of <code>0</code> since it is not allowed (it
   * makes no sense) to have negative inter arrival times. For more information
   * about the normal distribution see {@link StochasticSuppliers#normal()}.
   * @param length The length of the time series, all generated times will be in
   *          the interval [0,length).
   * @param numEvents The total number of events in the time series (on
   *          average).
   * @param sd The standard deviation of the normal distribution.
   * @return A {@link TimeSeriesGenerator} based on a normal distribution.
   */
  public static TimeSeriesGenerator normal(double length, int numEvents,
      double sd) {
    checkArgument(length > 0d);
    checkArgument(numEvents > 0);
    final double average = length / numEvents;
    return toTimeSeries(length, StochasticSuppliers.normal()
        .mean(average)
        .std(sd)
        .lowerBound(0d)
        .redrawWhenOutOfBounds()
        .scaleMean()
        .buildDouble());
  }

  /**
   * Converts a {@link StochasticSupplier} of {@link Double}s into a
   * {@link TimeSeriesGenerator}. Each time in the time series is created by
   * <code>t[n] = t[n-1] + ss.get(..)</code>, here <code>ss</code> is the
   * {@link Double} supplier.
   * @param length The length of the time series, all generated times will be in
   *          the interval [0,length).
   * @param interArrivalTimesSupplier The supplier to use for computing event
   *          inter arrival times.
   * @return A new {@link TimeSeriesGenerator} based on the supplier.
   */
  public static TimeSeriesGenerator toTimeSeries(double length,
      StochasticSupplier<Double> interArrivalTimesSupplier) {
    return new SupplierTimeSeries(length, interArrivalTimesSupplier);
  }

  /**
   * Decorates the specified {@link TimeSeriesGenerator} such that it only
   * generates time series which conform to the specified {@link Predicate}.
   * Predicates can be combined by using the methods provided by
   * {@link com.google.common.base.Predicates}. Note that when an impossible
   * {@link Predicate} is specified, such as
   * {@link com.google.common.base.Predicates#alwaysFalse()} the resulting
   * {@link TimeSeriesGenerator} will enter an infinite loop.
   * @param tsg The {@link TimeSeriesGenerator} to filter.
   * @param predicate All returned {@link TimeSeriesGenerator}s will conform to
   *          this predicate.
   * @return A filtered generator.
   */
  public static TimeSeriesGenerator filter(TimeSeriesGenerator tsg,
      Predicate<List<Double>> predicate) {
    return new FilteredTSG(tsg, predicate);
  }

  /**
   * Creates a {@link Predicate} for a specified number of events. This
   * predicate only accepts time series with exactly <code>num</code> events.
   * @param num The number of events a time series should have.
   * @return A newly created predicate.
   */
  public static Predicate<List<Double>> numEventsPredicate(final int num) {
    return new Predicate<List<Double>>() {
      @Override
      public boolean apply(@Nullable List<Double> input) {
        checkArgument(input != null);
        return input.size() == num;
      }
    };
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

  static class FilteredTSG implements TimeSeriesGenerator {
    private final TimeSeriesGenerator delegate;
    private final Predicate<List<Double>> predicate;
    private final RandomGenerator rng;

    FilteredTSG(TimeSeriesGenerator tsg, Predicate<List<Double>> pred) {
      delegate = tsg;
      predicate = pred;
      rng = new MersenneTwister();
    }

    @Override
    public ImmutableList<Double> generate(long seed) {
      rng.setSeed(seed);
      while (true) {
        final ImmutableList<Double> timeSeries = delegate.generate(rng
            .nextLong());
        if (predicate.apply(timeSeries)) {
          return timeSeries;
        }
      }
    }
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
      return new TimeSeriesIterator(new ExponentialDistribution(rng,
          1d / intensity,
          ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY),
          length);
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

  static class SuppliedNonHomogenous implements TimeSeriesGenerator {
    final double length;
    final StochasticSupplier<IntensityFunction> lambdSup;
    final RandomGenerator rng;

    SuppliedNonHomogenous(double l,
        StochasticSupplier<IntensityFunction> funcSup) {
      length = l;
      lambdSup = funcSup;
      rng = new MersenneTwister();
    }

    @Override
    public ImmutableList<Double> generate(long seed) {
      rng.setSeed(seed);
      final TimeSeriesGenerator tsg = new NonHomogenous(length,
          lambdSup.get(rng.nextLong()));
      return tsg.generate(rng.nextLong());
    }
  }

  static class SupplierTimeSeries implements TimeSeriesGenerator {
    private final double length;
    private final StochasticSupplier<Double> supplier;

    SupplierTimeSeries(double len, StochasticSupplier<Double> sup) {
      length = len;
      supplier = sup;
    }

    @Override
    public ImmutableList<Double> generate(long seed) {
      return ImmutableList.copyOf(new SupplierIterator(length, supplier,
          new MersenneTwister(seed)));
    }

  }

  static class SupplierIterator extends AbstractSequentialIterator<Double> {
    private final double length;
    private final StochasticSupplier<Double> supplier;
    private final RandomGenerator randomNumberGenerator;

    SupplierIterator(double len, StochasticSupplier<Double> sup,
        RandomGenerator rng) {
      super(next(0d, len, sup, rng));
      length = len;
      supplier = sup;
      randomNumberGenerator = rng;
    }

    @Nullable
    @Override
    protected Double computeNext(Double previous) {
      return next(previous, length, supplier, randomNumberGenerator);
    }

    @Nullable
    static Double next(Double prev, double len,
        StochasticSupplier<Double> supplier, RandomGenerator rng) {
      final double nextVal = prev + getValue(supplier, rng);
      if (nextVal < len) {
        return nextVal;
      }
      return null;
    }

    static double getValue(StochasticSupplier<Double> ed, RandomGenerator rng) {
      final double sample = ed.get(rng.nextLong());
      checkArgument(
          sample >= 0d,
          "A StochasticSupplier used in a TimeSeries may not return negative values, was: %s.",
          sample);
      return sample;
    }
  }

  static class UniformTimeSeries implements TimeSeriesGenerator {
    static final double SMALLEST_DEVIATION = .0000001;
    private final RandomGenerator rng;
    private final double length;
    private final double average;
    private final StochasticSupplier<Double> deviationSupplier;

    UniformTimeSeries(double len, double avg, StochasticSupplier<Double> dev) {
      rng = new MersenneTwister();
      length = len;
      average = avg;
      deviationSupplier = dev;
    }

    @Override
    public ImmutableList<Double> generate(long seed) {
      rng.setSeed(seed);

      double deviation = deviationSupplier.get(rng.nextLong());
      deviation = Math.min(average, deviation);
      final double lowerBound = average - deviation;
      final double upperBound = average + deviation;

      if (deviation < SMALLEST_DEVIATION) {
        return ImmutableList.copyOf(new FixedTimeSeriesIterator(rng, length,
            average));
      }
      return ImmutableList.copyOf(new TimeSeriesIterator(
          new UniformRealDistribution(rng, lowerBound, upperBound), length));
    }
  }

  static class NormalTimeSeries implements TimeSeriesGenerator {
    private final double length;
    private final RealDistribution distribution;

    NormalTimeSeries(double len, double avg, double sd) {
      length = len;
      distribution = new NormalDistribution(avg, sd);
    }

    @Override
    public ImmutableList<Double> generate(long seed) {
      distribution.reseedRandomGenerator(seed);
      return ImmutableList.copyOf(new TimeSeriesIterator(
          distribution, length));
    }
  }

  static class FixedTimeSeriesIterator extends
      AbstractSequentialIterator<Double> {

    private final double length;
    private final double average;

    protected FixedTimeSeriesIterator(RandomGenerator rng, double len,
        double avg) {
      super(rng.nextDouble() * avg);
      length = len;
      average = avg;
    }

    @Nullable
    @Override
    protected Double computeNext(Double prev) {
      final double nextVal = prev + average;
      if (nextVal < length) {
        return nextVal;
      }
      return null;
    }
  }

  static class TimeSeriesIterator extends AbstractSequentialIterator<Double> {
    private final RealDistribution ed;
    private final double length;

    TimeSeriesIterator(RealDistribution distr, double len) {
      super(next(0d, len, distr));
      length = len;
      ed = distr;
    }

    @Nullable
    @Override
    protected Double computeNext(Double previous) {
      return next(previous, length, ed);
    }

    @Nullable
    static Double next(Double prev, double len, RealDistribution ed) {
      final double nextVal = prev + getFirstPositive(ed);
      if (nextVal < len) {
        return nextVal;
      }
      return null;
    }

    static double getFirstPositive(RealDistribution ed) {
      double sample = ed.sample();
      while (sample < 0) {
        sample = ed.sample();
      }
      return sample;
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
