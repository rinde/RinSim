package rinde.sim.pdptw.generator.loc;

import static com.google.common.base.Preconditions.checkArgument;
import static rinde.sim.util.SupplierRngs.uniformDouble;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.util.SupplierRng;
import rinde.sim.util.SupplierRngs;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class Locations {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    Optional<Double> xMin, yMin, xMax, yMax, xMean, yMean, xSd, ySd;
    Optional<Boolean> redraw;

    Builder() {
      xMin = yMin = xMax = yMax = xMean = yMean = xSd = ySd = Optional.absent();
      redraw = Optional.absent();
    }

    public Builder squareByArea(double area) {
      return square(Math.sqrt(area));
    }

    public Builder square(double side) {
      return xMin(0d).yMin(0d).xMax(side).yMax(side);
    }

    public Builder xMin(double x) {
      xMin = Optional.of(Double.valueOf(x));
      return this;
    }

    public Builder yMin(double y) {
      yMin = Optional.of(Double.valueOf(y));
      return this;
    }

    public Builder min(double min) {
      return xMin(min).yMin(min);
    }

    public Builder min(Point p) {
      return xMin(p.x).yMin(p.y);
    }

    public Builder xMax(double x) {
      xMax = Optional.of(Double.valueOf(x));
      return this;
    }

    public Builder yMax(double y) {
      yMax = Optional.of(Double.valueOf(y));
      return this;
    }

    public Builder max(double max) {
      return xMax(max).yMax(max);
    }

    public Builder max(Point max) {
      return xMax(max.x).yMax(max.y);
    }

    public Builder mean(Point center) {
      return xMean(center.x).yMean(center.y);
    }

    public Builder mean(double m) {
      return xMean(m).yMean(m);
    }

    public Builder xMean(double x) {
      xMean = Optional.of(Double.valueOf(x));
      return this;
    }

    public Builder yMean(double y) {
      yMean = Optional.of(Double.valueOf(y));
      return this;
    }

    public Builder std(double s) {
      return xStd(s).yStd(s);
    }

    public Builder std(Point p) {
      return xStd(p.x).yStd(p.y);
    }

    public Builder xStd(double x) {
      xSd = Optional.of(Double.valueOf(x));
      return this;
    }

    public Builder yStd(double y) {
      ySd = Optional.of(Double.valueOf(y));
      return this;
    }

    public Builder redrawWhenOutOfBounds() {
      redraw = Optional.of(Boolean.TRUE);
      return this;
    }

    public Builder roundWhenOutOfBounds() {
      redraw = Optional.of(Boolean.FALSE);
      return this;
    }

    // min/max takes precedence over mean/sd
    public LocationGenerator uniform() {
      final SupplierRng<Double> x = uniformVar(xMin, xMax, xMean, xSd);
      final SupplierRng<Double> y = uniformVar(yMin, yMax, yMean, ySd);
      return new SupplierLocGen(x, y);
    }

    public LocationGenerator normal() {
      return new SupplierLocGen(
          normalVar(xMin, xMax, xMean, xSd, redraw),
          normalVar(yMin, yMax, yMean, ySd, redraw));
    }

    private static SupplierRng<Double> uniformVar(Optional<Double> min,
        Optional<Double> max, Optional<Double> mean, Optional<Double> std) {
      if (min.isPresent() && max.isPresent()) {
        checkArgument(min.get() < max.get());
        return uniformDouble(min.get(), max.get());
      } else if (mean.isPresent() && std.isPresent()) {
        final double length = Math.sqrt(12) * std.get();
        final double minn = mean.get() - length;
        final double maxx = mean.get() + length;
        return uniformDouble(minn, maxx);
      } else {
        throw new IllegalArgumentException();
      }
    }

    private static SupplierRng<Double> normalVar(Optional<Double> min,
        Optional<Double> max, Optional<Double> mean, Optional<Double> std,
        Optional<Boolean> redraw) {
      checkArgument(min.isPresent());
      checkArgument(max.isPresent());
      checkArgument(mean.isPresent());
      checkArgument(std.isPresent());
      checkArgument(redraw.isPresent());
      final SupplierRngs.Builder builder = SupplierRngs.normal()
          .mean(mean.get())
          .std(std.get())
          .lowerBound(min.get())
          .upperBound(max.get());
      if (redraw.get()) {
        builder.redrawWhenOutOfBounds();
      } else {
        builder.roundWhenOutOfBounds();
      }
      return builder.doubleSupplier();
    }
  }

  private static class SupplierLocGen implements LocationGenerator {
    private final SupplierRng<Double> xSupplier;
    private final SupplierRng<Double> ySupplier;

    SupplierLocGen(SupplierRng<Double> xSup, SupplierRng<Double> ySup) {
      xSupplier = xSup;
      ySupplier = ySup;
    }

    @Override
    public ImmutableList<Point> generate(int numOrders, RandomGenerator rng) {
      final ImmutableList.Builder<Point> locs = ImmutableList.builder();
      for (int i = 0; i < numOrders; i++) {
        locs.add(new Point(
            xSupplier.get(rng.nextLong()),
            ySupplier.get(rng.nextLong())));
      }
      return locs.build();
    }
  }
}
