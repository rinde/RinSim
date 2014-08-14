package com.github.rinde.rinsim.scenario.generator;

import static com.github.rinde.rinsim.util.StochasticSuppliers.checked;
import static com.github.rinde.rinsim.util.StochasticSuppliers.constant;
import static com.github.rinde.rinsim.util.StochasticSuppliers.isConstant;
import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.Nullable;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;

/**
 * Utilities for {@link IntensityFunction} instances.
 * @author Rinde van Lon
 */
public final class IntensityFunctions {
  private IntensityFunctions() {}

  /**
   * Wraps an {@link IntensityFunction} into a {@link UnivariateFunction}.
   * @param intFunc The input function.
   * @return The wrapped function.
   */
  public static UnivariateFunction asUnivariateFunction(
      IntensityFunction intFunc) {
    return new IntensityFunctionWrapper(intFunc);
  }

  /**
   * Compute the area of a sine intensity by using numerical approximation. The
   * range for which the area is computed is defined by a lower bound of
   * <code>0</code> and an upper bound of <code>length</code>.
   * @param s The intensity function to integrate.
   * @param lb The lower bound of the range.
   * @param ub The upper bound of the range.
   * @return The area.
   */
  public static double areaByIntegration(IntensityFunction s, double lb,
      double ub) {
    final UnivariateIntegrator ri = new RombergIntegrator(16, 32);
    final double val = ri.integrate(10000000,
        asUnivariateFunction(s), lb, ub);
    return val;
  }

  /**
   * @return A new builder for creating sine {@link IntensityFunction}
   *         instances.
   */
  public static SineIntensityBuilder sineIntensity() {
    return new SineIntensityBuilder();
  }

  /**
   * Represents a function <code>f(x)</code> that returns the intensity at time
   * <code>x</code>. This function can be used to characterize an
   * {@link com.github.rinde.rinsim.scenario.generator.TimeSeries.TimeSeriesGenerator}
   * .
   * @author Rinde van Lon
   */
  public interface IntensityFunction extends Function<Double, Double> {

    /**
     * @return The global maximum intensity.
     */
    double getMax();

    // overridden to remove @Nullable at return argument
    @Override
    Double apply(@Nullable Double input);
  }

  /**
   * An intensity function characterized by:
   * <code>f(x) = amplitude * sin(x * frequency * 2pi - pi * phaseShift) + height</code>
   * . Instances are immutable and can be created using {@link #sineIntensity()}
   * .
   * @author Rinde van Lon
   */
  static class SineIntensity implements IntensityFunction {
    private static final double HALF_PI = .5 * Math.PI;
    private static final double TWO_PI = 2d * Math.PI;
    private static final double ONE_FOURTH = .25d;

    final double amplitude;
    final double height;
    private final double frequency;
    private final double phaseShift;

    SineIntensity(SineIntensityBuilder b, long seed) {
      final RandomGenerator rng = new MersenneTwister(seed);
      amplitude = b.amplitudeSup.get(rng.nextLong());
      frequency = b.frequencySup.get(rng.nextLong());
      height = b.heightSup.get(rng.nextLong());
      phaseShift = b.phaseShiftSup.get(rng.nextLong());
    }

    @Override
    public double getMax() {
      return amplitude + height;
    }

    @Override
    public Double apply(@Nullable Double x) {
      if (x == null) {
        throw new IllegalArgumentException();
      }
      return Math.max(0d,
          amplitude
              * Math.sin(x * frequency * TWO_PI - Math.PI * phaseShift)
              + height);
    }

    /**
     * @return The amplitude of this sine function.
     */
    public double getAmplitude() {
      return amplitude;
    }

    /**
     * @return The frequency of this sine function.
     */
    public double getFrequency() {
      return frequency;
    }

    /**
     * @return The height of this sine function.
     */
    public double getHeight() {
      return height;
    }

    /**
     * @return The phase shift of this sine function.
     */
    public double getPhaseShift() {
      return phaseShift;
    }

    @Override
    public String toString() {
      return new StringBuilder().append("{f(x) = ")
          .append(amplitude).append(" * ")
          .append("sin(x * ").append(frequency)
          .append(" * 2pi - pi * ").append(phaseShift)
          .append(") + ").append(height).append("}")
          .toString();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(amplitude, frequency, height, phaseShift);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == null) {
        return false;
      }
      if (this == o) {
        return true;
      }
      if (getClass() != o.getClass()) {
        return false;
      }
      final SineIntensity other = (SineIntensity) o;
      return Objects.equal(amplitude, other.amplitude) &&
          Objects.equal(frequency, other.frequency) &&
          Objects.equal(height, other.height) &&
          Objects.equal(phaseShift, other.phaseShift);
    }

    /**
     * Computes the area under this sine function and above y=0 in the range
     * [0,period). Where period is defined as <code>1/frequency</code>.
     * @return The computed area.
     */
    public double area() {
      // in this computation the phase shift is ignored as it doesn't have any
      // effect for one period.
      final double a = amplitude;
      final double b = height;
      final double c = frequency;
      final double[] roots = roots();
      final double d = roots[0];
      final double e = roots[1];
      return a * Math.sin(Math.PI * c * (d - e))
          * Math.sin(HALF_PI - Math.PI * c * (d + e))
          / (Math.PI * c) + b * (e - d);
    }

    double[] roots() {
      final double a = amplitude;
      // we need to cap height since if it is higher there are no roots
      final double b = Math.min(height, a);
      final double c = frequency;
      final double n1 = -1;
      final double n2 = 0;

      final double common = Math.asin(b / a) / (TWO_PI * c);
      final double rootA = -ONE_FOURTH / c + common - n1 / c;
      final double rootB = ONE_FOURTH / c - common - n2 / c;
      if (rootA > rootB) {
        return new double[] { rootB, rootA };
      } else {
        return new double[] { rootA, rootB };
      }
    }
  }

  /**
   * A builder for creating sine {@link IntensityFunction} instances.
   * @author Rinde van Lon
   */
  public static class SineIntensityBuilder {
    private static final double DEFAULT_AMPLITUDE = 1d;
    private static final double DEFAULT_FREQUENCY = 1d;
    private static final double DEFAULT_HEIGHT = 0d;
    private static final double DEFAULT_PHASE_SHIFT = .5d;
    private static final Predicate<Double> POSITIVE = Range.open(0d,
        Double.MAX_VALUE);
    private static final Predicate<Double> GREATER_THAN_MINUS_ONE = Range
        .openClosed(-1d, Double.MAX_VALUE);
    private static final Predicate<Double> FINITE = Range.closed(
        Double.MIN_VALUE, Double.MAX_VALUE);

    StochasticSupplier<Double> amplitudeSup;
    StochasticSupplier<Double> frequencySup;
    StochasticSupplier<Double> heightSup;
    Optional<Double> area;
    StochasticSupplier<Double> phaseShiftSup;

    SineIntensityBuilder() {
      amplitudeSup = constant(DEFAULT_AMPLITUDE);
      frequencySup = constant(DEFAULT_FREQUENCY);
      heightSup = constant(DEFAULT_HEIGHT);
      area = Optional.absent();
      phaseShiftSup = constant(DEFAULT_PHASE_SHIFT);
    }

    SineIntensityBuilder(SineIntensityBuilder b) {
      amplitudeSup = b.amplitudeSup;
      frequencySup = b.frequencySup;
      heightSup = b.heightSup;
      area = b.area;
      phaseShiftSup = b.phaseShiftSup;
    }

    /**
     * Sets the amplitude of the {@link IntensityFunction} that will be created
     * by this builder. Default value: 1.
     * @param a Must be positive.
     * @return This, as per the builder pattern.
     */
    public SineIntensityBuilder amplitude(double a) {
      checkArgument(a > 0d);
      checkArgument(Doubles.isFinite(a));
      amplitudeSup = constant(a);
      return this;
    }

    /**
     * Sets the {@link StochasticSupplier} that will be used to generate the
     * amplitude of the {@link IntensityFunction} that will be created by this
     * builder. Default value: 1.
     * @param a Must be positive.
     * @return This, as per the builder pattern.
     */
    public SineIntensityBuilder amplitude(StochasticSupplier<Double> a) {
      amplitudeSup = checked(a, POSITIVE);
      return this;
    }

    /**
     * Sets the frequency of the {@link IntensityFunction} that will be created
     * by this builder. Default value: 1.
     * @param f Must be positive.
     * @return This, as per the builder pattern.
     */
    public SineIntensityBuilder frequency(double f) {
      checkArgument(f > 0d);
      checkArgument(Doubles.isFinite(f));
      frequencySup = constant(f);
      return this;
    }

    /**
     * Sets the {@link StochasticSupplier} that will be used to generate the
     * frequency of the {@link IntensityFunction} that will be created by this
     * builder. Default value: 1.
     * @param f Must be positive.
     * @return This, as per the builder pattern.
     */
    public SineIntensityBuilder frequency(StochasticSupplier<Double> f) {
      frequencySup = checked(f, POSITIVE);
      return this;
    }

    /**
     * Sets the period of the {@link IntensityFunction} that will be created by
     * this builder. Default value: 1.
     * @param p Must be positive.
     * @return This, as per the builder pattern.
     */
    public SineIntensityBuilder period(double p) {
      checkArgument(p > 0d);
      checkArgument(Doubles.isFinite(p));
      frequencySup = constant(1d / p);
      return this;
    }

    /**
     * Sets the height of the {@link IntensityFunction} that will be created by
     * this builder. Default value: 0. Typical values range between
     * <code>-1</code> and <code>1</code>. If the height is close to
     * <code>-1</code> almost the entire function will be negative. If the
     * height is <code>1</code> or higher the entire function will be positive.
     * @param h Must be <code> &gt; -1</code>.
     * @return This, as per the builder pattern.
     */
    public SineIntensityBuilder height(double h) {
      checkArgument(h > -1d);
      checkArgument(Doubles.isFinite(h));
      heightSup = constant(h);
      return this;
    }

    /**
     * Sets the {@link StochasticSupplier} that will be used to generate the
     * height of the {@link IntensityFunction} that will be created by this
     * builder. Default value: 0. Typical values range between <code>-1</code>
     * and <code>1</code>. If the height is close to <code>-1</code> almost the
     * entire function will be negative. If the height is <code>1</code> or
     * higher the entire function will be positive.
     * @param h Must be <code> &gt; -1</code>.
     * @return This, as per the builder pattern.
     */
    public SineIntensityBuilder height(StochasticSupplier<Double> h) {
      heightSup = checked(h, GREATER_THAN_MINUS_ONE);
      return this;
    }

    /**
     * Set the area of the sine function. This is defined as the area under the
     * sine function and above y=0 in the range [0,period). Where period is
     * defined as <code>1/frequency</code>. If the area is set to <code>n</code>
     * the expected number of events in a single period is <code>n</code>.
     * <p>
     * When calling this method, the amplitude and height of the created
     * {@link IntensityFunction} will be adjusted such that it has the specified
     * area. When this method is not called no adjustments will be made.
     * @param a The area. Must be positive.
     * @return This, as per the builder pattern.
     */
    public SineIntensityBuilder area(double a) {
      checkArgument(a > 0d, "Area must be positive, is %s.", a);
      checkArgument(Doubles.isFinite(a));
      area = Optional.of(a);
      return this;
    }

    /**
     * Sets the phaseShift of the {@link IntensityFunction} that will be created
     * by this builder. Default value: 1/2.
     * @param s The phase shift.
     * @return This, as per the builder pattern.
     */
    public SineIntensityBuilder phaseShift(double s) {
      checkArgument(Doubles.isFinite(s));
      phaseShiftSup = constant(s);
      return this;
    }

    /**
     * Sets the {@link StochasticSupplier} that will be used to generate the
     * phaseShift of the {@link IntensityFunction} that will be created by this
     * builder. Default value: 1/2.
     * @param s The phase shift.
     * @return This, as per the builder pattern.
     */
    public SineIntensityBuilder phaseShift(StochasticSupplier<Double> s) {
      phaseShiftSup = checked(s, FINITE);
      return this;
    }

    /**
     * Creates a new instance of a sine {@link IntensityFunction}. This method
     * requires constant values to be set. For using supplied values see
     * {@link #buildStochasticSupplier()}.
     * @return A new instance.
     */
    public IntensityFunction build() {
      checkArgument(isConstant(amplitudeSup),
          "Amplitude should be a constant (not a supplier).");
      checkArgument(isConstant(frequencySup),
          "Frequency should be a constant (not a supplier).");
      checkArgument(isConstant(heightSup),
          "Height should be a constant (not a supplier).");
      checkArgument(isConstant(phaseShiftSup),
          "PhaseShift should be a constant (not a supplier).");
      return build(0L);
    }

    /**
     * @return A {@link StochasticSupplier} that creates sine
     *         {@link IntensityFunction} instances.
     */
    public StochasticSupplier<IntensityFunction> buildStochasticSupplier() {
      return new SineIntensityFunctionSupplier(this);
    }

    IntensityFunction build(long seed) {
      if (area.isPresent()) {
        final SineIntensity ins = new SineIntensity(this, seed);
        // first compute current area
        final double a = ins.area();
        // compute factor to adapt amplitude and height
        final double factor = area.get() / a;

        // store values
        final StochasticSupplier<Double> ampl = amplitudeSup;
        final StochasticSupplier<Double> hei = heightSup;

        // temporarily overwrite values
        amplitudeSup = constant(ins.amplitude * factor);
        heightSup = constant(ins.height * factor);
        final SineIntensity si = new SineIntensity(this, seed);

        // restore values
        amplitudeSup = ampl;
        heightSup = hei;
        return si;
      }
      return new SineIntensity(this, seed);
    }

    SineIntensityBuilder copy() {
      return new SineIntensityBuilder(this);
    }
  }

  private static class SineIntensityFunctionSupplier implements
      StochasticSupplier<IntensityFunction> {
    private final SineIntensityBuilder builder;

    SineIntensityFunctionSupplier(SineIntensityBuilder b) {
      builder = b.copy();
    }

    @Override
    public IntensityFunction get(long seed) {
      return builder.build(seed);
    }
  }

  private static class IntensityFunctionWrapper implements UnivariateFunction {
    private final IntensityFunction function;

    IntensityFunctionWrapper(IntensityFunction func) {
      function = func;
    }

    @Override
    public double value(double x) {
      return function.apply(x);
    }
  }
}
