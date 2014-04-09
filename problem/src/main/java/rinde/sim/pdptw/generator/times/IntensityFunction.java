package rinde.sim.pdptw.generator.times;

import javax.annotation.Nullable;

import org.apache.commons.math3.analysis.UnivariateFunction;

import com.google.common.base.Function;

public interface IntensityFunction extends Function<Double, Double> {

  double getMax();

  // overridden to remove @Nullable at return argument
  @Override
  Double apply(@Nullable Double input);

  public static class IntensityFunctionWrapper implements UnivariateFunction {
    IntensityFunction function;

    public IntensityFunctionWrapper(IntensityFunction func) {
      function = func;
    }

    @Override
    public double value(double x) {
      return function.apply(x);
    }
  }

}
