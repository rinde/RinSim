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

  public static <T> SupplierRng<T> constant(T value) {
    return new ConstantSupplierRng<T>(value);
  }

  public static Builder normal() {
    return new Builder();
  }

  public static SupplierRng<Double> uniformDouble(double lower, double upper) {
    return new DoubleDistributionSupplierRng(new UniformRealDistribution(
        new MersenneTwister(), lower, upper));
  }

  public static SupplierRng<Integer> uniformInt(int lower, int upper) {
    return new IntegerDistributionSupplierRng(new UniformIntegerDistribution(
        new MersenneTwister(), lower, upper));
  }

  public static SupplierRng<Long> uniformLong(int lower, int upper) {
    return intToLong(uniformInt(lower, upper));
  }

  public static SupplierRng<Long> intToLong(SupplierRng<Integer> supplier) {
    return new IntToLongAdapter(supplier);
  }

  public static SupplierRng<Integer> roundDoubleToInt(
      SupplierRng<Double> supplier) {
    return new DoubleToIntAdapter(supplier);
  }

  public static SupplierRng<Long> roundDoubleToLong(SupplierRng<Double> supplier) {
    return new DoubleToLongAdapter(supplier);
  }

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

    public Builder mean(double m) {
      mean = m;
      return this;
    }

    public Builder std(double s) {
      std = s;
      return this;
    }

    public Builder variance(double v) {
      std = Math.sqrt(v);
      return this;
    }

    public Builder bounds(double lower, double upper) {
      lowerBound = lower;
      upperBound = upper;
      return this;
    }

    public Builder lowerBound(double l) {
      lowerBound = l;
      return this;
    }

    public Builder upperBound(double u) {
      upperBound = u;
      return this;
    }

    public Builder redrawWhenOutOfBounds() {
      outOfBoundStrategy = OutOfBoundStrategy.REDRAW;
      return this;
    }

    public Builder roundWhenOutOfBounds() {
      outOfBoundStrategy = OutOfBoundStrategy.ROUND;
      return this;
    }

    public SupplierRng<Double> doubleSupplier() {
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

    void integerChecks() {
      checkArgument(Double.isInfinite(lowerBound)
          || DoubleMath.isMathematicalInteger(lowerBound));
      checkArgument(Double.isInfinite(upperBound)
          || DoubleMath.isMathematicalInteger(upperBound));
    }

    public SupplierRng<Integer> integerSupplier() {
      integerChecks();
      return roundDoubleToInt(doubleSupplier());
    }

    public SupplierRng<Long> longSupplier() {
      integerChecks();
      return roundDoubleToLong(doubleSupplier());
    }
  }

  /**
   * Abstract implementation providing a default {@link #toString()}
   * implementation.
   * @param <T> The type of objects that this supplier creates.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static abstract class AbstractSupplierRng<T> implements SupplierRng<T> {
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
      return new Long(supplier.get(seed));
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

    public IntegerDistributionSupplierRng(IntegerDistribution id) {
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

    public BoundedDoubleDistSupplierRng(RealDistribution rd, double upper,
        double lower, OutOfBoundStrategy strategy) {
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

    public DoubleDistributionSupplierRng(RealDistribution rd) {
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
