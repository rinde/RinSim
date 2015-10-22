/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.math.RoundingMode;
import java.util.Iterator;

import javax.annotation.Nonnull;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.MersenneTwister;

import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Doubles;
import com.google.common.reflect.TypeToken;

/**
 * Utility class for {@link StochasticSupplier}.
 * @author Rinde van Lon
 */
public final class StochasticSuppliers {

  private StochasticSuppliers() {}

  /**
   * Create a {@link StochasticSupplier} that will always return the specified
   * value.
   * @param value The value which the supplier will return.
   * @param <T> Type of constant.
   * @return A supplier that always returns the specified value.
   */
  public static <T> StochasticSupplier<T> constant(T value) {
    return new ConstantSupplier<>(value);
  }

  /**
   * Checks whether the provided supplier is a constant created by
   * {@link #constant(Object)}.
   * @param supplier The supplier to check.
   * @param <T> The type this supplier generates.
   * @return <code>true</code> if the provided supplier is created by
   *         {@link #constant(Object)}, <code>false</code> otherwise.
   */
  public static <T> boolean isConstant(StochasticSupplier<T> supplier) {
    return supplier.getClass() == ConstantSupplier.class;
  }

  /**
   * Creates a {@link StochasticSupplier} that will always throw an
   * {@link IllegalArgumentException} with the specified <code>errorMsg</code>.
   * This can be useful when a default 'empty' supplier is needed.
   * @param errorMsg The error message of the exception.
   * @param <T> The type this supplier generates.
   * @return A supplier that always throws an exception.
   */
  public static <T> StochasticSupplier<T> empty(String errorMsg) {
    return new EmptySupplier<>(errorMsg);
  }

  /**
   * Decorates the specified {@link StochasticSupplier} such that when it
   * produces values which are not allowed by the specified predicate an
   * {@link IllegalArgumentException} is thrown.
   * @param supplier The supplier to be decorated.
   * @param predicate The predicate which specifies the contract to which the
   *          supplier should adhere.
   * @param <T> The type this supplier generates.
   * @return A supplier that is guaranteed to return values which match the
   *         given predicate or throw an {@link IllegalArgumentException}.
   */
  public static <T> StochasticSupplier<T> checked(
      StochasticSupplier<T> supplier,
      Predicate<T> predicate) {
    return new CheckedSupplier<>(supplier, predicate);
  }

  /**
   * Create a {@link StochasticSupplier} based on an {@link Iterable}. It will
   * return the values in the order as defined by the iterable. The resulting
   * supplier will throw an {@link IllegalArgumentException} when the iterable
   * is empty.
   * @param iter The iterable from which the values will be used.
   * @param <T> The type this supplier generates.
   * @return A supplier based on an iterable.
   */
  public static <T> StochasticSupplier<T> fromIterable(Iterable<T> iter) {
    return new IteratorSS<>(iter.iterator());
  }

  /**
   * Create a {@link StochasticSupplier} based on a {@link Supplier}.
   * @param supplier The supplier to adapt.
   * @param <T> The type this supplier generates.
   * @return The adapted supplier.
   */
  public static <T> StochasticSupplier<T> fromSupplier(Supplier<T> supplier) {
    return new SupplierAdapter<>(supplier);
  }

  /**
   * @return Builder for constructing {@link StochasticSupplier}s that produce
   *         normal (Gaussian) distributed numbers.
   */
  public static Builder normal() {
    return new Builder();
  }

  /**
   * Creates a {@link StochasticSupplier} that produces uniformly distributed
   * {@link Double}s.
   * @param lower The (inclusive) lower bound of the uniform distribution.
   * @param upper The (inclusive) upper bound of the uniform distribution.
   * @return The supplier.
   */
  public static StochasticSupplier<Double> uniformDouble(double lower,
      double upper) {
    return new DoubleDistributionSS(new UniformRealDistribution(
        new MersenneTwister(), lower, upper));
  }

  /**
   * Creates a {@link StochasticSupplier} that produces uniformly distributed
   * {@link Integer}s.
   * @param lower The (inclusive) lower bound of the uniform distribution.
   * @param upper The (inclusive) upper bound of the uniform distribution.
   * @return The supplier.
   */
  public static StochasticSupplier<Integer> uniformInt(int lower, int upper) {
    return new IntegerDistributionSS(new UniformIntegerDistribution(
        new MersenneTwister(), lower, upper));
  }

  /**
   * Creates a {@link StochasticSupplier} that produces uniformly distributed
   * {@link Long}s.
   * @param lower The (inclusive) lower bound of the uniform distribution.
   * @param upper The (inclusive) upper bound of the uniform distribution.
   * @return The supplier.
   */
  public static StochasticSupplier<Long> uniformLong(int lower, int upper) {
    return intToLong(uniformInt(lower, upper));
  }

  /**
   * Convert a {@link StochasticSupplier} of {@link Integer} to a supplier of
   * {@link Long}.
   * @param supplier The supplier to convert.
   * @return The converted supplier.
   */
  public static StochasticSupplier<Long> intToLong(
      StochasticSupplier<Integer> supplier) {
    return new IntToLongAdapter(supplier);
  }

  /**
   * Convert a {@link StochasticSupplier} of {@link Double} to a supplier of
   * {@link Integer}.
   * @param supplier The supplier to convert.
   * @return The converted supplier.
   */
  public static StochasticSupplier<Integer> roundDoubleToInt(
      StochasticSupplier<Double> supplier) {
    return new DoubleToIntAdapter(supplier);
  }

  /**
   * Convert a {@link StochasticSupplier} of {@link Double} to a supplier of
   * {@link Long}.
   * @param supplier The supplier to convert.
   * @return The converted supplier.
   */
  public static StochasticSupplier<Long> roundDoubleToLong(
      StochasticSupplier<Double> supplier) {
    return new DoubleToLongAdapter(supplier);
  }

  /**
   * @return A {@link StochasticSupplier} of {@link MersenneTwister}.
   */
  public static StochasticSupplier<MersenneTwister> mersenneTwister() {
    return MersenneTwisterSS.create();
  }

  /**
   * Builder for creating {@link StochasticSupplier}s that return a number with
   * a normal distribution.
   * @author Rinde van Lon
   */
  public static class Builder {
    static final double SMALLEST_DOUBLE = 0.000000000000001;
    static final int MAX_ITERATIONS = 1000000;
    static final double STEP_SIZE_DENOMINATOR = 1.5d;
    private double mean;
    private double std;
    private double lowerBound;
    private double upperBound;
    private OutOfBoundStrategy outOfBoundStrategy;

    Builder() {
      mean = 0;
      std = 1;
      lowerBound = Double.NEGATIVE_INFINITY;
      upperBound = Double.POSITIVE_INFINITY;
      outOfBoundStrategy = OutOfBoundStrategy.REDRAW;
    }

    /**
     * Set the mean of the normal distribution.
     * @param m The mean. Default value: <code>0</code>.
     * @return This, as per the builder pattern.
     */
    public Builder mean(double m) {
      mean = m;
      return this;
    }

    /**
     *
     * @param sd The standard deviation. Default value: <code>1</code>.
     * @return This, as per the builder pattern.
     */
    public Builder std(double sd) {
      std = sd;
      return this;
    }

    /**
     *
     * @param var The variance. Default value: <code>1</code>.
     * @return This, as per the builder pattern.
     */
    public Builder variance(double var) {
      std = Math.sqrt(var);
      return this;
    }

    /**
     * Truncates the normal distribution using the lower and upper bounds. In
     * case a number is drawn outside the bounds:
     * <code> x &lt; lower || x &gt; upper</code> the out of bound strategy
     * defines what will happen. See {@link #redrawWhenOutOfBounds()} and
     * {@link #roundWhenOutOfBounds()} for the options. Note that calling this
     * method may change the effective mean and standard deviation of the normal
     * distribution. If this is undesired you can choose to scale the mean of
     * the distribution, see {@link #scaleMean()} for more details.
     * @param lower The lower bound. Default value:
     *          {@link Double#NEGATIVE_INFINITY}.
     * @param upper The upper bound. Default value:
     *          {@link Double#POSITIVE_INFINITY}.
     * @return This, as per the builder pattern.
     */
    public Builder bounds(double lower, double upper) {
      lowerBound = lower;
      upperBound = upper;
      return this;
    }

    /**
     * Sets the lower bound, see {@link #bounds(double, double)} for more
     * information.
     * @param lower The lower bound.
     * @return This, as per the builder pattern.
     */
    public Builder lowerBound(double lower) {
      lowerBound = lower;
      return this;
    }

    /**
     * Sets the upper bound, see {@link #bounds(double, double)} for more
     * information.
     * @param upper The upper bound.
     * @return This, as per the builder pattern.
     */
    public Builder upperBound(double upper) {
      upperBound = upper;
      return this;
    }

    /**
     * Calling this method will set the out of bounds strategy to redraw. This
     * means that when a number is drawn from the distribution that is out of
     * bounds a new number will be drawn. This will continue until a value is
     * found within bounds. Note that when the bounds are small relative to the
     * distribution this may result in a large number of attempts. By default
     * this strategy is enabled.
     * @return This, as per the builder pattern.
     */
    public Builder redrawWhenOutOfBounds() {
      outOfBoundStrategy = OutOfBoundStrategy.REDRAW;
      return this;
    }

    /**
     * Calling this method will set the out of bounds strategy to redraw. This
     * means that when a number is drawn from the distribution that is out of
     * bounds the number will be rounded to the nearest bound. By default this
     * strategy is disabled.
     * @return This, as per the builder pattern.
     */
    public Builder roundWhenOutOfBounds() {
      outOfBoundStrategy = OutOfBoundStrategy.ROUND;
      return this;
    }

    /**
     * Scale the normal distribution such that the effective mean is as given by
     * {@link #mean(double)} in case a lower bound was set. This method can only
     * be called if the following requirements are met:
     * <ul>
     * <li>Lower bound must be set</li>
     * <li>Out of bound strategy: {@link #redrawWhenOutOfBounds()}.</li>
     * </ul>
     * Note that this method overwrites any previous (but not subsequent) calls
     * to {@link #mean(double)}. If after calling this method the bounds and/or
     * out of bound strategy are changed this may yield unexpected results.
     * Also, using an upper bound is currently not supported.
     * <p>
     * For more information about how the effective mean of the truncated normal
     * distribution is calculated, see this <a href=
     * "https://en.wikipedia.org/wiki/Truncated_normal_distribution#Moments" >
     * Wikipedia article</a>.
     * @return This, as per the builder pattern.
     */
    public Builder scaleMean() {
      checkArgument(!Double.isInfinite(lowerBound),
          "A lower bound must be set in order to scale the mean.");
      checkArgument(Double.isInfinite(upperBound),
          "Scaling the mean with an upper bound is currently not supported.");
      checkArgument(OutOfBoundStrategy.REDRAW == outOfBoundStrategy);

      double stepSize = 1;
      double curMean = mean;
      double dir = 0;
      double effectiveMean;
      int iterations = 0;
      do {
        effectiveMean = computeEffectiveMean(curMean, std, lowerBound);
        // save direction
        final double oldDir = dir;
        if (effectiveMean > mean) {
          dir = 1d;
        } else {
          dir = -1d;
        }
        // if direction changed decrease step size
        if (dir != oldDir && oldDir != 0) {
          stepSize /= STEP_SIZE_DENOMINATOR;
        }
        // apply step
        if (effectiveMean > mean) {
          curMean -= stepSize;
        } else {
          curMean += stepSize;
        }
        iterations++;
        checkState(iterations < MAX_ITERATIONS,
            "Could not converge. Target mean: %s, effective mean: %s.", mean,
            effectiveMean);
      } while (Math.abs(effectiveMean - mean) > SMALLEST_DOUBLE);
      mean = curMean;
      return this;
    }

    /*
     * Computes effective mean using
     * https://en.wikipedia.org/wiki/Truncated_normal_distribution#Moments .
     */
    private static double computeEffectiveMean(double m, double s, double lb) {
      final NormalDistribution normal = new NormalDistribution();
      final double alpha = (lb - m) / s;
      final double pdf = normal.density(alpha);
      final double cdf = normal.cumulativeProbability(alpha);
      final double lambda = pdf / (1 - cdf);
      return m + s * lambda;
    }

    /**
     * @return A {@link StochasticSupplier} that draws double values from a
     *         normal distribution.
     */
    public StochasticSupplier<Double> buildDouble() {
      checkArgument(mean + std >= lowerBound);
      checkArgument(mean + std <= upperBound);
      final RealDistribution distribution = new NormalDistribution(mean, std);
      if (Doubles.isFinite(lowerBound) || Doubles.isFinite(upperBound)) {
        return new BoundedDoubleDistSS(distribution, upperBound,
            lowerBound, outOfBoundStrategy);
      }
      return new DoubleDistributionSS(distribution);
    }

    /**
     * @return A {@link StochasticSupplier} that draws integer values from a
     *         normal distribution.
     */
    public StochasticSupplier<Integer> buildInteger() {
      integerChecks();
      return roundDoubleToInt(buildDouble());
    }

    /**
     * @return A {@link StochasticSupplier} that draws long values from a normal
     *         distribution.
     */
    public StochasticSupplier<Long> buildLong() {
      integerChecks();
      return roundDoubleToLong(buildDouble());
    }

    void integerChecks() {
      checkArgument(Double.isInfinite(lowerBound)
          || DoubleMath.isMathematicalInteger(lowerBound));
      checkArgument(Double.isInfinite(upperBound)
          || DoubleMath.isMathematicalInteger(upperBound));
    }
  }

  /**
   * Abstract implementation providing a default {@link #toString()}
   * implementation.
   * @author Rinde van Lon
   * @param <T> The type of objects that this supplier creates.
   */
  public abstract static class AbstractStochasticSupplier<T> implements
      StochasticSupplier<T>, Serializable {
    private static final long serialVersionUID = 992219257352250656L;

    @Override
    public String toString() {
      return new TypeToken<T>(getClass()) {
        private static final long serialVersionUID = 4641163444574558674L;
      }.getRawType().getSimpleName() + "Supplier";
    }
  }

  enum OutOfBoundStrategy {
    ROUND, REDRAW
  }

  private static class IntToLongAdapter extends
      AbstractStochasticSupplier<Long> {
    private static final long serialVersionUID = 3638307177262422449L;
    private final StochasticSupplier<Integer> supplier;

    IntToLongAdapter(StochasticSupplier<Integer> supp) {
      supplier = supp;
    }

    @Override
    public Long get(long seed) {
      return Long.valueOf(supplier.get(seed));
    }
  }

  private static class DoubleToIntAdapter extends
      AbstractStochasticSupplier<Integer> {
    private static final long serialVersionUID = 3086452659883375531L;
    private final StochasticSupplier<Double> supplier;

    DoubleToIntAdapter(StochasticSupplier<Double> supp) {
      supplier = supp;
    }

    @Override
    public Integer get(long seed) {
      return DoubleMath.roundToInt(supplier.get(seed), RoundingMode.HALF_UP);
    }
  }

  private static class DoubleToLongAdapter extends
      AbstractStochasticSupplier<Long> {
    private static final long serialVersionUID = -8846720318135533333L;
    private final StochasticSupplier<Double> supplier;

    DoubleToLongAdapter(StochasticSupplier<Double> supp) {
      supplier = supp;
    }

    @Override
    public Long get(long seed) {
      return DoubleMath.roundToLong(supplier.get(seed), RoundingMode.HALF_UP);
    }
  }

  private static class IntegerDistributionSS extends
      AbstractStochasticSupplier<Integer> {
    private static final long serialVersionUID = -7967542154741162460L;
    private final IntegerDistribution distribution;

    IntegerDistributionSS(IntegerDistribution id) {
      distribution = id;
    }

    @Override
    public Integer get(long seed) {
      distribution.reseedRandomGenerator(seed);
      return distribution.sample();
    }
  }

  private static class BoundedDoubleDistSS extends
      AbstractStochasticSupplier<Double> {
    private static final long serialVersionUID = -6738290534532097051L;
    private final RealDistribution distribution;
    private final double lowerBound;
    private final double upperBound;
    private final OutOfBoundStrategy outOfBoundStrategy;

    BoundedDoubleDistSS(RealDistribution rd, double upper,
        double lower, OutOfBoundStrategy strategy) {
      checkArgument(strategy == OutOfBoundStrategy.REDRAW
          || strategy == OutOfBoundStrategy.ROUND);
      distribution = rd;
      lowerBound = lower;
      upperBound = upper;
      outOfBoundStrategy = strategy;
    }

    @Override
    public Double get(long seed) {
      distribution.reseedRandomGenerator(seed);
      double val = distribution.sample();
      if (outOfBoundStrategy == OutOfBoundStrategy.REDRAW) {
        while (!isInBounds(val)) {
          val = distribution.sample();
        }
      } else if (val < lowerBound) {
        val = lowerBound;
      } else if (val >= upperBound) {
        val = upperBound;
      }
      return val;
    }

    boolean isInBounds(double val) {
      return val >= lowerBound && val < upperBound;
    }
  }

  private static class DoubleDistributionSS extends
      AbstractStochasticSupplier<Double> {
    private static final long serialVersionUID = -5853417575632121095L;
    private final RealDistribution distribution;

    DoubleDistributionSS(RealDistribution rd) {
      distribution = rd;
    }

    @Override
    public Double get(long seed) {
      distribution.reseedRandomGenerator(seed);
      return distribution.sample();
    }
  }

  private static class IteratorSS<T> extends
      AbstractStochasticSupplier<T> {
    private static final long serialVersionUID = 3151363361183354655L;
    private final Iterator<T> iterator;

    IteratorSS(Iterator<T> it) {
      iterator = it;
    }

    @Override
    public T get(long seed) {
      if (iterator.hasNext()) {
        return iterator.next();
      }
      throw new IllegalStateException("This supplier is exhausted.");
    }
  }

  private static final class ConstantSupplier<T>
      extends AbstractStochasticSupplier<T> {
    private static final long serialVersionUID = -5017806121674846656L;
    private final T value;

    ConstantSupplier(T v) {
      value = v;
    }

    @Override
    @Nonnull
    public T get(long seed) {
      return value;
    }

    @Override
    public String toString() {
      return String.format("%s.constant(%s)",
          StochasticSuppliers.class.getSimpleName(),
          value);
    }
  }

  private static final class EmptySupplier<T>
      extends AbstractStochasticSupplier<T> {
    private static final long serialVersionUID = 1993638453016457007L;
    private final String message;

    EmptySupplier(String msg) {
      message = msg;
    }

    @Override
    @Nonnull
    public T get(long seed) {
      throw new IllegalArgumentException(message);
    }

    @Override
    public String toString() {
      return String.format("%s.empty()",
          StochasticSuppliers.class.getSimpleName());
    }
  }

  private static class SupplierAdapter<T>
      extends AbstractStochasticSupplier<T> {
    private static final long serialVersionUID = 1388067842132493130L;
    private final Supplier<T> supplier;

    SupplierAdapter(Supplier<T> sup) {
      supplier = sup;
    }

    @Override
    public T get(long seed) {
      return supplier.get();
    }
  }

  private static class CheckedSupplier<T> implements StochasticSupplier<T> {
    private final StochasticSupplier<T> supplier;
    private final Predicate<T> predicate;

    CheckedSupplier(StochasticSupplier<T> sup, Predicate<T> pred) {
      supplier = sup;
      predicate = pred;
    }

    @Override
    public T get(long seed) {
      final T value = supplier.get(seed);
      checkArgument(predicate.apply(value),
          "The supplier generated an invalid value: %s.", value);
      return value;
    }
  }

  @AutoValue
  abstract static class MersenneTwisterSS implements
      StochasticSupplier<MersenneTwister> {

    @Override
    public MersenneTwister get(long seed) {
      return new MersenneTwister(seed);
    }

    static MersenneTwisterSS create() {
      return new AutoValue_StochasticSuppliers_MersenneTwisterSS();
    }
  }
}
