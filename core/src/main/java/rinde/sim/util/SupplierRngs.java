package rinde.sim.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.RoundingMode;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.random.MersenneTwister;

import com.google.common.math.DoubleMath;
import com.google.common.primitives.Doubles;
import com.google.common.reflect.TypeToken;

/**
 * Utility class for {@link SupplierRng}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class SupplierRngs {

  private SupplierRngs() {}

  /**
   * Create a {@link SupplierRng} that will always return the specified value.
   * @param value The value which the supplier will return.
   * @param <T> Type of constant.
   * @return A supplier that always returns the specified value.
   */
  public static <T> SupplierRng<T> constant(T value) {
    return new ConstantSupplierRng<T>(value);
  }

  /**
   * @return Builder for constructing {@link SupplierRng}s that produce normal
   *         (Gaussian) distributed numbers.
   */
  public static Builder normal() {
    return new Builder();
  }

  /**
   * Creates a {@link SupplierRng} that produces uniformly distributed
   * {@link Double}s.
   * @param lower The lower bound of the uniform distribution.
   * @param upper The upper bound of the uniform distribution.
   * @return The supplier.
   */
  public static SupplierRng<Double> uniformDouble(double lower, double upper) {
    return new DoubleDistributionSupplierRng(new UniformRealDistribution(
        new MersenneTwister(), lower, upper));
  }

  /**
   * Creates a {@link SupplierRng} that produces uniformly distributed
   * {@link Integer}s.
   * @param lower The lower bound of the uniform distribution.
   * @param upper The upper bound of the uniform distribution.
   * @return The supplier.
   */
  public static SupplierRng<Integer> uniformInt(int lower, int upper) {
    return new IntegerDistributionSupplierRng(new UniformIntegerDistribution(
        new MersenneTwister(), lower, upper));
  }

  /**
   * Creates a {@link SupplierRng} that produces uniformly distributed
   * {@link Long}s.
   * @param lower The lower bound of the uniform distribution.
   * @param upper The upper bound of the uniform distribution.
   * @return The supplier.
   */
  public static SupplierRng<Long> uniformLong(int lower, int upper) {
    return intToLong(uniformInt(lower, upper));
  }

  /**
   * Convert a {@link SupplierRng} of {@link Integer} to a supplier of
   * {@link Long}.
   * @param supplier The supplier to convert.
   * @return The converted supplier.
   */
  public static SupplierRng<Long> intToLong(SupplierRng<Integer> supplier) {
    return new IntToLongAdapter(supplier);
  }

  /**
   * Convert a {@link SupplierRng} of {@link Double} to a supplier of
   * {@link Integer}.
   * @param supplier The supplier to convert.
   * @return The converted supplier.
   */
  public static SupplierRng<Integer> roundDoubleToInt(
      SupplierRng<Double> supplier) {
    return new DoubleToIntAdapter(supplier);
  }

  /**
   * Convert a {@link SupplierRng} of {@link Double} to a supplier of
   * {@link Long}.
   * @param supplier The supplier to convert.
   * @return The converted supplier.
   */
  public static SupplierRng<Long> roundDoubleToLong(SupplierRng<Double> supplier) {
    return new DoubleToLongAdapter(supplier);
  }

  /**
   * Builder for creating {@link SupplierRng}s that return a number with a
   * normal distribution.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static class Builder {
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
     * Sets the lower and upper bounds of the normal distribution. In case a
     * number is drawn outside the bounds: <code> x < lower || x > upper</code>
     * the out of bound strategy defines what will happen. See
     * {@link #redrawWhenOutOfBounds()} and {@link #roundWhenOutOfBounds()} for
     * the options.
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
     * @return A {@link SupplierRng} that draws double values from a normal
     *         distribution.
     */
    public SupplierRng<Double> buildDouble() {
      checkArgument(mean + std >= lowerBound);
      checkArgument(mean + std <= upperBound);
      final RealDistribution distribution = new NormalDistribution(mean, std);
      if (Doubles.isFinite(lowerBound) || Doubles.isFinite(upperBound)) {
        return new BoundedDoubleDistSupplierRng(distribution, upperBound,
            lowerBound, outOfBoundStrategy);
      } else {
        return new DoubleDistributionSupplierRng(distribution);
      }
    }

    /**
     * @return A {@link SupplierRng} that draws integer values from a normal
     *         distribution.
     */
    public SupplierRng<Integer> buildInteger() {
      integerChecks();
      return roundDoubleToInt(buildDouble());
    }

    /**
     * @return A {@link SupplierRng} that draws long values from a normal
     *         distribution.
     */
    public SupplierRng<Long> buildLong() {
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
   * @param <T> The type of objects that this supplier creates.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public abstract static class AbstractSupplierRng<T> implements SupplierRng<T> {
    @SuppressWarnings("serial")
    @Override
    public String toString() {
      return new TypeToken<T>(getClass()) {}.getRawType().getSimpleName();
    }
  }

  enum OutOfBoundStrategy {
    ROUND, REDRAW
  }

  private static class IntToLongAdapter extends
      AbstractSupplierRng<Long> {
    private final SupplierRng<Integer> supplier;

    IntToLongAdapter(SupplierRng<Integer> supp) {
      supplier = supp;
    }

    @Override
    public Long get(long seed) {
      return Long.valueOf(supplier.get(seed));
    }
  }

  private static class DoubleToIntAdapter extends
      AbstractSupplierRng<Integer> {
    private final SupplierRng<Double> supplier;

    DoubleToIntAdapter(SupplierRng<Double> supp) {
      supplier = supp;
    }

    @Override
    public Integer get(long seed) {
      return DoubleMath.roundToInt(supplier.get(seed), RoundingMode.HALF_UP);
    }
  }

  private static class DoubleToLongAdapter extends
      AbstractSupplierRng<Long> {
    private final SupplierRng<Double> supplier;

    DoubleToLongAdapter(SupplierRng<Double> supp) {
      supplier = supp;
    }

    @Override
    public Long get(long seed) {
      return DoubleMath.roundToLong(supplier.get(seed), RoundingMode.HALF_UP);
    }
  }

  private static class IntegerDistributionSupplierRng extends
      AbstractSupplierRng<Integer> {
    private final IntegerDistribution distribution;

    IntegerDistributionSupplierRng(IntegerDistribution id) {
      distribution = id;
    }

    @Override
    public Integer get(long seed) {
      distribution.reseedRandomGenerator(seed);
      return distribution.sample();
    }
  }

  private static class BoundedDoubleDistSupplierRng extends
      AbstractSupplierRng<Double> {
    private final RealDistribution distribution;
    private final double lowerBound;
    private final double upperBound;
    private final OutOfBoundStrategy outOfBoundStrategy;

    BoundedDoubleDistSupplierRng(RealDistribution rd, double upper,
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

  private static class DoubleDistributionSupplierRng extends
      AbstractSupplierRng<Double> {
    private final RealDistribution distribution;

    DoubleDistributionSupplierRng(RealDistribution rd) {
      distribution = rd;
    }

    @Override
    public Double get(long seed) {
      distribution.reseedRandomGenerator(seed);
      return distribution.sample();
    }
  }

  private static class ConstantSupplierRng<T> extends AbstractSupplierRng<T> {
    private final T value;

    ConstantSupplierRng(T v) {
      value = v;
    }

    @Override
    public T get(long seed) {
      return value;
    }
  }
}
