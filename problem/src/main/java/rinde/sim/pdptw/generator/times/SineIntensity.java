package rinde.sim.pdptw.generator.times;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.Nullable;

/**
 * An intensity function characterized by:
 * <code>( amplitude * sin( (x*frequency*2pi) - (pi/2) ) ) + height</code>.
 * Instances can be created using {@link #builder()}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class SineIntensity implements IntensityFunction {

  private static final double HALF_PI = .5 * Math.PI;
  private static final double TWO_PI = 2d * Math.PI; // tau

  private final double amplitude;
  private final double frequency;
  private final double height;

  SineIntensity(Builder b) {
    amplitude = b.amplitude;
    frequency = b.frequency;
    height = b.height;
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
        (amplitude
            * Math.sin((x * frequency * TWO_PI) - (HALF_PI))
            )
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
   * Computes the area under this sine function and above y=0 in the range
   * [0,period). Where period is defined as <code>1/frequency</code>.
   * @return The computed area.
   */
  public double area() {
    final double a = amplitude;
    final double b = height;
    final double c = frequency;
    final double[] roots = roots();
    final double d = roots[0];
    final double e = roots[1];
    return (a * Math.sin(Math.PI * c * (d - e)) * Math.sin(80143857d
        / 51021164d - Math.PI * c * (d + e)))
        / (Math.PI * c) + b * (e - d);
  }

  double[] roots() {
    final double a = amplitude;
    // we need to cap height since if it is higher there are no roots
    final double b = Math.min(height, a);
    final double c = frequency;
    final double n1 = -1;
    final double n2 = 0;

    final double common = (Math.asin(b / a)) / (TWO_PI * c);
    final double rootA = (-0.25 / c) + common - n1 / c;
    final double rootB = (0.25 / c) - common - n2 / c;
    if (rootA > rootB) {
      return new double[] { rootB, rootA };
    } else {
      return new double[] { rootA, rootB };
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    double amplitude;
    double frequency;
    double height;
    double area;

    Builder() {
      amplitude = 1d;
      frequency = 1d;
      height = 0d;
      area = -1;
    }

    public Builder amplitude(double a) {
      amplitude = a;
      return this;
    }

    public Builder frequency(double f) {
      checkArgument(f > 0d);
      frequency = f;
      return this;
    }

    public Builder period(double p) {
      checkArgument(p > 0d);
      frequency = 1d / p;
      return this;
    }

    public Builder height(double h) {
      checkArgument(h > -1d);
      height = h;
      return this;
    }

    /**
     * Set the area of the sine function. This is defined as the area under the
     * sine function and above y=0 in the range [0,period). Where period is
     * defined as <code>1/frequency</code>.
     * @param a The area.
     * @return This, as per the builder pattern.
     */
    public Builder area(double a) {
      checkArgument(a > 0d);
      area = a;
      return this;
    }

    public SineIntensity build() {
      if (area > 0) {
        // first compute current area
        final double a = new SineIntensity(this).area();
        // compute factor to adapt amplitude and height
        final double factor = area / a;
        amplitude *= factor;
        height *= factor;
        return new SineIntensity(this);
      }
      return new SineIntensity(this);
    }
  }
}
