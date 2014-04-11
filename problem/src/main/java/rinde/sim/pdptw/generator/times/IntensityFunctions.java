package rinde.sim.pdptw.generator.times;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;

/**
 * Utilities for {@link IntensityFunction} instances.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class IntensityFunctions {
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
