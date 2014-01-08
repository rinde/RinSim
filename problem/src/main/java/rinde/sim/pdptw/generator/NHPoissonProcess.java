package rinde.sim.pdptw.generator;

import java.math.RoundingMode;

import javax.annotation.Nullable;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

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
  public ImmutableList<Long> generate(RandomGenerator rng) {
    final ExponentialDistribution ed = new ExponentialDistribution(rng,
        1d / lambdaMax,
        ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    long sum = 0;

    final ImmutableList.Builder<Long> builder = ImmutableList.builder();
    while (sum < length) {
      sum += DoubleMath
          .roundToLong(ed.sample(), RoundingMode.HALF_DOWN);
      if (sum < length && rng.nextDouble() <= (lambd.apply(sum) / lambdaMax)) {
        builder.add(sum);
      }
    }
    return builder.build();
  }

  public interface IntensityFunction extends Function<Long, Double> {

    double getMax();

    // overridden to remove @Nullable at return argument
    @Override
    Double apply(@Nullable Long input);
  }

  public static class SineIntensity implements IntensityFunction {

    private final double ampl;
    private final double freq;

    public SineIntensity(double amplitude, double frequency) {
      ampl = amplitude;
      freq = frequency;
    }

    @Override
    public double getMax() {
      return ampl;// * 2;
    }

    @Override
    public Double apply(@Nullable Long input) {
      if (input == null) {
        throw new IllegalArgumentException();
      }
      return ampl
          * (Math.sin((input * freq * Math.PI * 2d) - (.5 * Math.PI)) + 0d);
    }

  }
}
