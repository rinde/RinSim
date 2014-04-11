package rinde.sim.pdptw.generator.times;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.common.primitives.Doubles;

/**
 * An intensity function characterized by:
 * <code>f(x) = amplitude * sin(x * frequency * 2pi - pi * phaseShift) + height</code>
 * . Instances are immutable and can be created using {@link #builder()}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class SineIntensity implements IntensityFunction {
  private static final double HALF_PI = .5 * Math.PI;
  private static final double TWO_PI = 2d * Math.PI;

  private final double amplitude;
  private final double frequency;
  private final double height;
  private final double phaseShift;

  SineIntensity(Builder b) {
    amplitude = b.amplitude;
    frequency = b.frequency;
    height = b.height;
    phaseShift = b.phaseShift;
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
    final double rootA = -0.25 / c + common - n1 / c;
    final double rootB = 0.25 / c - common - n2 / c;
    if (rootA > rootB) {
      return new double[] { rootB, rootA };
    } else {
      return new double[] { rootA, rootB };
    }
  }

  /**
   * @return A new builder for creating {@link SineIntensity} instances.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for creating {@link SineIntensity} instances.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static class Builder {
    double amplitude;
    double frequency;
    double height;
    double area;
    double phaseShift;

    Builder() {
      amplitude = 1d;
      frequency = 1d;
      height = 0d;
      area = -1;
      phaseShift = .5d;
    }

    /**
     * Sets the amplitude of the {@link SineIntensity} that will be created by
     * this builder. Default value: 1.
     * @param a Must be positive.
     * @return This, as per the builder pattern.
     */
    public Builder amplitude(double a) {
      checkArgument(a > 0d);
      checkArgument(Doubles.isFinite(a));
      amplitude = a;
      return this;
    }

    /**
     * Sets the frequency of the {@link SineIntensity} that will be created by
     * this builder. Default value: 1.
     * @param f Must be positive.
     * @return This, as per the builder pattern.
     */
    public Builder frequency(double f) {
      checkArgument(f > 0d);
      checkArgument(Doubles.isFinite(f));
      frequency = f;
      return this;
    }

    /**
     * Sets the period of the {@link SineIntensity} that will be created by this
     * builder. Default value: 1.
     * @param p Must be positive.
     * @return This, as per the builder pattern.
     */
    public Builder period(double p) {
      checkArgument(p > 0d);
      checkArgument(Doubles.isFinite(p));
      frequency = 1d / p;
      return this;
    }

    /**
     * Sets the height of the {@link SineIntensity} that will be created by this
     * builder. Default value: 0.
     * @param h Must be <code> > -1</code>.
     * @return This, as per the builder pattern.
     */
    public Builder height(double h) {
      checkArgument(h > -1d);
      checkArgument(Doubles.isFinite(h));
      height = h;
      return this;
    }

    /**
     * Set the area of the sine function. This is defined as the area under the
     * sine function and above y=0 in the range [0,period). Where period is
     * defined as <code>1/frequency</code>. When calling this method, the
     * amplitude and height of the created {@link SineIntensity} will be
     * adjusted such that it has the specified area. When this method is not
     * called no adjustments will be made.
     * @param a The area. Must be positive.
     * @return This, as per the builder pattern.
     */
    public Builder area(double a) {
      checkArgument(a > 0d);
      checkArgument(Doubles.isFinite(a));
      area = a;
      return this;
    }

    /**
     * Sets the phaseShift of the {@link SineIntensity} that will be created by
     * this builder. Default value: 1/2.
     * @param s The phase shift.
     * @return This, as per the builder pattern.
     */
    public Builder phaseShift(double s) {
      checkArgument(Doubles.isFinite(s));
      phaseShift = s;
      return this;
    }

    /**
     * @return A new instance.
     */
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
