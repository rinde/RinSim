package rinde.sim.pdptw.generator;

import javax.annotation.Nullable;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of a non-homogenous Poisson process based on [1].
 * 
 * <ol>
 * <li>Lewis, P.A.W. and Shedler, G.S. Simulation of nonhomogenous Poisson
 * processes by thinning. Naval Research Logistic Quarterly 26, (1979), 403–414.
 * </li>
 * </ol>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class NHPoissonProcess implements ArrivalTimesGenerator {

  private final long length;
  private final IntensityFunction lambd;
  private final double lambdaMax;

  public NHPoissonProcess(long scenarioLength, IntensityFunction lambda) {
    length = scenarioLength;
    lambd = lambda;
    lambdaMax = lambda.getMax();
  }

  @Override
  public ImmutableList<Double> generate(RandomGenerator rng) {
    final ExponentialDistribution ed = new ExponentialDistribution(rng,
        1d / lambdaMax,
        ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    double sum = 0d;

    final ImmutableList.Builder<Double> builder = ImmutableList.builder();
    while (sum < length) {
      sum += ed.sample();
      if (sum < length && rng.nextDouble() <= (lambd.apply(sum) / lambdaMax)) {
        builder.add(sum);
      }
    }
    return builder.build();
  }

  public interface IntensityFunction extends Function<Double, Double> {

    double getMax();

    // overridden to remove @Nullable at return argument
    @Override
    Double apply(@Nullable Double input);
  }

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

  public static class SineIntensity implements IntensityFunction {

    public final double ampl;
    public final double freq;
    public final double height;

    public SineIntensity(double amplitude, double frequency, double relHeight,
        double absHeight) {
      ampl = amplitude;
      freq = frequency;
      height = (relHeight * amplitude) + absHeight;
      // System.out.println(amplitude + " " + frequency + "  " + relHeight);
    }

    @Override
    public double getMax() {
      return ampl + height;// * 2;
    }

    @Override
    public Double apply(@Nullable Double input) {
      if (input == null) {
        throw new IllegalArgumentException();
      }

      return Math.max(0, (ampl
          * Math.sin(input * freq * Math.PI * 2d)) + height);
    }
  }
}
