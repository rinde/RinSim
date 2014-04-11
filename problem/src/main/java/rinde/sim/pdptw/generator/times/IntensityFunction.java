package rinde.sim.pdptw.generator.times;

import javax.annotation.Nullable;

import com.google.common.base.Function;

/**
 * Represents a function <code>f(x)</code> that returns the intensity at time
 * <code>x</code>. This function can be used to characterize an
 * {@link ArrivalTimesGenerator}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
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
