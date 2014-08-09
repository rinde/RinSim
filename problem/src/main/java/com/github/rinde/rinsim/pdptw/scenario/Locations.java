package com.github.rinde.rinsim.pdptw.scenario;

import static com.github.rinde.rinsim.util.StochasticSuppliers.uniformDouble;
import static com.google.common.base.Preconditions.checkArgument;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * Utility class for creating {@link LocationGenerator}s.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class Locations {
  private Locations() {}

  /**
   * @return A {@link Builder} for creating {@link LocationGenerator}s.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A location generator generates locations for orders (aka tasks).
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface LocationGenerator {
    /**
     * Should generate locations for the specified number of orders (aka tasks).
     * There should be enough locations for each order. Typically this is
     * predefined as a ratio, e.g. <code>1:2</code> in case origin and
     * destination is required for each order.
     * @param seed The random seed.
     * @param numOrders The number of orders for which a location is required.
     * @return A list of locations for the orders.
     */
    ImmutableList<Point> generate(long seed, int numOrders);

    /**
     * @return The expected center of all generated locations.
     */
    Point getCenter();

    /**
     * @return A position representing the lowest possible coordinates.
     */
    Point getMin();

    /**
     * @return A position representing the highest possible coordinates.
     */
    Point getMax();
  }

  /**
   * A builder for creating {@link LocationGenerator}s.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static class Builder {
    Optional<Double> xMin;
    Optional<Double> yMin;
    Optional<Double> xMax;
    Optional<Double> yMax;
    Optional<Double> xMean;
    Optional<Double> yMean;
    Optional<Double> xSd;
    Optional<Double> ySd;
    Optional<Boolean> redraw;

    Builder() {
      xMin = Optional.absent();
      yMin = Optional.absent();
      xMax = Optional.absent();
      yMax = Optional.absent();
      xMean = Optional.absent();
      yMean = Optional.absent();
      xSd = Optional.absent();
      ySd = Optional.absent();
      redraw = Optional.absent();
    }

    /**
     * Sets the borders of the location generator such that it spans the
     * specified area. The min position is <code>(0,0)</code>, and max is
     * <code>(sqrt(area),sqrt(area))</code>.
     * @param area The area to use.
     * @return This, as per the builder pattern.
     */
    public Builder squareByArea(double area) {
      return square(Math.sqrt(area));
    }

    /**
     * Sets the borders of the location generator such that the min position is
     * <code>(0,0)</code> and the max position is <code>(side,side)</code>.
     * @param side The side of a square.
     * @return This, as per the builder pattern.
     */
    public Builder square(double side) {
      return xMin(0d).yMin(0d).xMax(side).yMax(side);
    }

    /**
     * Sets the minimum x value that the location generator will generate.
     * @param x The minimum x value.
     * @return This, as per the builder pattern.
     */
    public Builder xMin(double x) {
      xMin = Optional.of(Double.valueOf(x));
      return this;
    }

    /**
     * Sets the minimum y value that the location generator will generate.
     * @param y The minimum y value.
     * @return This, as per the builder pattern.
     */
    public Builder yMin(double y) {
      yMin = Optional.of(Double.valueOf(y));
      return this;
    }

    /**
     * Sets the minimum x and y values that the location generator will
     * generate.
     * @param min The minimum x and y value.
     * @return This, as per the builder pattern.
     */
    public Builder min(double min) {
      return xMin(min).yMin(min);
    }

    /**
     * Sets the minimum position the the location generator will generate.
     * @param p The minimum position.
     * @return This, as per the builder pattern.
     */
    public Builder min(Point p) {
      return xMin(p.x).yMin(p.y);
    }

    /**
     * Sets the maximum x value that the location generator will generate.
     * @param x The maximum x value.
     * @return This, as per the builder pattern.
     */
    public Builder xMax(double x) {
      xMax = Optional.of(Double.valueOf(x));
      return this;
    }

    /**
     * Sets the maximum y value that the location generator will generate.
     * @param y The maximum y value.
     * @return This, as per the builder pattern.
     */
    public Builder yMax(double y) {
      yMax = Optional.of(Double.valueOf(y));
      return this;
    }

    /**
     * Sets the maximum x and y values the location generator will generate.
     * @param max The maximum x and y values.
     * @return This, as per the builder pattern.
     */
    public Builder max(double max) {
      return xMax(max).yMax(max);
    }

    /**
     * Sets the maximum position the the location generator will generate.
     * @param max The maximum position.
     * @return This, as per the builder pattern.
     */
    public Builder max(Point max) {
      return xMax(max.x).yMax(max.y);
    }

    /**
     * Sets the position that the location generator will use as mean.
     * @param mean The mean position.
     * @return This, as per the builder pattern.
     */
    public Builder mean(Point mean) {
      return xMean(mean.x).yMean(mean.y);
    }

    /**
     * Sets the mean position that the location generator will use as mean.
     * @param m The x and y value of the mean position.
     * @return This, as per the builder pattern.
     */
    public Builder mean(double m) {
      return xMean(m).yMean(m);
    }

    /**
     * Sets the x mean that the location generator will use.
     * @param x The x mean.
     * @return This, as per the builder pattern.
     */
    public Builder xMean(double x) {
      xMean = Optional.of(Double.valueOf(x));
      return this;
    }

    /**
     * Sets the y mean that the location generator will use.
     * @param y The y mean.
     * @return This, as per the builder pattern.
     */
    public Builder yMean(double y) {
      yMean = Optional.of(Double.valueOf(y));
      return this;
    }

    /**
     * Sets the standard deviation that the location generator will use.
     * @param s The standard deviation.
     * @return This, as per the builder pattern.
     */
    public Builder std(double s) {
      return xStd(s).yStd(s);
    }

    /**
     * Sets the standard deviations for x and y that the location generator will
     * use.
     * @param p A point indicating the x and y standard deviations.
     * @return This, as per the builder pattern.
     */
    public Builder std(Point p) {
      return xStd(p.x).yStd(p.y);
    }

    /**
     * Sets the x standard deviation that the location generator will use.
     * @param x The x standard deviation.
     * @return This, as per the builder pattern.
     */
    public Builder xStd(double x) {
      xSd = Optional.of(Double.valueOf(x));
      return this;
    }

    /**
     * Sets the y standard deviation that the location generator will use.
     * @param y The y standard deviation.
     * @return This, as per the builder pattern.
     */
    public Builder yStd(double y) {
      ySd = Optional.of(Double.valueOf(y));
      return this;
    }

    /**
     * When this option is used and a normal distributed location generator is
     * used it will draw a new random number in case the previous is out of
     * bounds.
     * @return This, as per the builder pattern.
     */
    public Builder redrawWhenOutOfBounds() {
      redraw = Optional.of(Boolean.TRUE);
      return this;
    }

    /**
     * When this option is used and a normal distributed location generator is
     * used it will round all random numbers such that they fit in the bounds.
     * @return This, as per the builder pattern.
     */
    public Builder roundWhenOutOfBounds() {
      redraw = Optional.of(Boolean.FALSE);
      return this;
    }

    /**
     * Create a uniform distributed {@link LocationGenerator}. The min and max
     * values set to this builder take precedence over the mean and standard
     * deviations. In case the boundaries were set any specified means and
     * standard deviations are ignored when generating the locations. However,
     * the mean as supplied to this builder will be returned in the
     * {@link LocationGenerator#getCenter()} method.
     * @return A uniform distributed generator.
     */
    public LocationGenerator uniform() {
      final Point xMinMax = getUniformMinMax(xMin, xMax, xMean, xSd);
      final Point yMinMax = getUniformMinMax(yMin, yMax, yMean, ySd);
      final Point min = new Point(xMinMax.x, yMinMax.x);
      final Point max = new Point(xMinMax.y, yMinMax.y);
      final double xCenter = getUniformCenter(xMean, min.x, max.x);
      final double yCenter = getUniformCenter(yMean, min.y, max.y);

      return new SupplierLocGen(
          min, max, new Point(xCenter, yCenter),
          uniformDouble(min.x, max.x),
          uniformDouble(min.y, max.y));
    }

    /**
     * Create a normal distributed {@link LocationGenerator}.
     * @return A normal distributed generator.
     */
    public LocationGenerator normal() {
      final StochasticSupplier<Double> xSup = normalVar(xMin, xMax, xMean, xSd, redraw);
      final StochasticSupplier<Double> ySup = normalVar(yMin, yMax, yMean, ySd, redraw);
      return new SupplierLocGen(
          new Point(xMin.get(), yMin.get()),
          new Point(xMax.get(), yMax.get()),
          new Point(xMean.get(), yMean.get()),
          xSup, ySup);
    }

    private static double getUniformCenter(Optional<Double> mean, double min,
        double max) {
      if (mean.isPresent()) {
        return mean.get();
      } else {
        return (max - min) / 2d;
      }
    }

    private static Point getUniformMinMax(Optional<Double> min,
        Optional<Double> max, Optional<Double> mean, Optional<Double> std) {
      if (min.isPresent() && max.isPresent()) {
        checkArgument(min.get() < max.get());
        return new Point(min.get(), max.get());
      } else if (mean.isPresent() && std.isPresent()) {
        final double length = Math.sqrt(12) * std.get();
        final double minn = mean.get() - length;
        final double maxx = mean.get() + length;
        return new Point(minn, maxx);
      } else {
        throw new IllegalArgumentException();
      }
    }

    private static StochasticSupplier<Double> normalVar(Optional<Double> min,
        Optional<Double> max, Optional<Double> mean, Optional<Double> std,
        Optional<Boolean> redraw) {
      checkArgument(min.isPresent());
      checkArgument(max.isPresent());
      checkArgument(mean.isPresent());
      checkArgument(std.isPresent());
      checkArgument(redraw.isPresent());
      final StochasticSuppliers.Builder builder = StochasticSuppliers.normal()
          .mean(mean.get())
          .std(std.get())
          .lowerBound(min.get())
          .upperBound(max.get());
      if (redraw.get()) {
        builder.redrawWhenOutOfBounds();
      } else {
        builder.roundWhenOutOfBounds();
      }
      return builder.buildDouble();
    }
  }

  private static class SupplierLocGen implements LocationGenerator {
    private final Point min;
    private final Point max;
    private final Point center;
    private final StochasticSupplier<Double> xSupplier;
    private final StochasticSupplier<Double> ySupplier;
    private final RandomGenerator rng;

    SupplierLocGen(Point mi, Point ma, Point ce, StochasticSupplier<Double> xSup,
        StochasticSupplier<Double> ySup) {
      min = mi;
      max = ma;
      center = ce;
      xSupplier = xSup;
      ySupplier = ySup;
      rng = new MersenneTwister();
    }

    @Override
    public ImmutableList<Point> generate(long seed, int numOrders) {
      rng.setSeed(seed);
      final ImmutableList.Builder<Point> locs = ImmutableList.builder();
      for (int i = 0; i < numOrders; i++) {
        locs.add(new Point(
            xSupplier.get(rng.nextLong()),
            ySupplier.get(rng.nextLong())));
      }
      return locs.build();
    }

    @Override
    public Point getMin() {
      return min;
    }

    @Override
    public Point getMax() {
      return max;
    }

    @Override
    public Point getCenter() {
      return center;
    }
  }
}
