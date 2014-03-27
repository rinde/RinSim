package rinde.sim.pdptw.generator;

import java.io.File;
import java.util.List;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import rinde.sim.pdptw.generator.times.ArrivalTimesGenerator;
import rinde.sim.pdptw.generator.times.NHPoissonProcess;
import rinde.sim.pdptw.generator.times.NHPoissonProcess.IntensityFunctionWrapper;
import rinde.sim.pdptw.generator.times.NHPoissonProcess.SineIntensity;

import com.google.common.collect.ImmutableList;

public class NHPoissonProcessTest {

  @Test
  public void test() {
    final long length = 28800;

    final int orders = 200;

    final RandomGenerator rng = new MersenneTwister(123);
    for (int j = 0; j < 10; j++) {

      final double relHeight = -.8 + (j * .2);
      final double freq = 1d / 3600d;
      final double min = j * .10d;
      final SineIntensity intensity = new SineIntensity(
          1d, freq, relHeight, min);

      final UnivariateIntegrator ri = new RombergIntegrator(16, 32);// new
      // TrapezoidIntegrator();//
      // new
      // SimpsonIntegrator();//
      // new
      // RombergIntegrator();

      final double val = ri.integrate(10000000, new IntensityFunctionWrapper(
          intensity), 0, length);
      System.out
          .printf("area: %1.3f, relative height: %1.3f%n", val, relHeight);

      final double newAmpl = orders / val;

      final SineIntensity finalIntensity = new SineIntensity(
          newAmpl, freq, relHeight, newAmpl * min);

      // final List<Long> list = newArrayList();
      // for (long l = 0; l < length; l++) {
      // final int expected = DoubleMath.roundToInt(10d *
      // intensity.apply(l),
      // RoundingMode.HALF_DOWN);
      // for (int i = 0; i < expected; i++) {
      // list.add(l);
      // }
      // }
      // Analysis.writeTimes(length, list, new File(
      // "files/test/times/sine-" + j + ".times"));

      final ArrivalTimesGenerator generator = new NHPoissonProcess(length,
          finalIntensity);

      double max = 0;
      double sum = 0;
      final int tot = 10000;
      for (int i = 0; i < tot; i++) {
        final List<Double> times = generator.generate(rng);

        final double dyn = Metrics.measureDynamism(times, length);
        sum += dyn;
        max = Math.max(max, dyn);
        if (i < 3) {
          System.out.printf("%1.3f%% %d%n", dyn * 100, times.size());
          Analysis.writeTimes(length, times, new File(
              "files/test/times/orders" + j + "-" + i + ".times"));
        }
      }
      System.out.println("-----------------> " + max + " " + (sum / tot));
    }
  }

  static ImmutableList<Double> convert(List<Long> in) {
    final ImmutableList.Builder<Double> builder = ImmutableList.builder();
    for (final Long l : in) {
      builder.add(new Double(l));
    }
    return builder.build();
  }

}
