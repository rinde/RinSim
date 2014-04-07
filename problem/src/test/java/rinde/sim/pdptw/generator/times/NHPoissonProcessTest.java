package rinde.sim.pdptw.generator.times;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.generator.Analysis;
import rinde.sim.pdptw.generator.Metrics;
import rinde.sim.pdptw.generator.times.NHPoissonProcess.IntensityFunctionWrapper;
import rinde.sim.pdptw.generator.times.NHPoissonProcess.SineIntensity;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.io.Files;

public class NHPoissonProcessTest {

  @Test
  public void test() throws IOException {
    final int numSamples = 1000;
    final long lengthOfScenario = 28800;
    final int orders = 200;
    final List<Point> dataPoints = newArrayList();
    final RandomGenerator rng = new MersenneTwister(123);

    final List<Double> relHeights = newArrayList();
    for (int i = 0; i < 10; i++) {
      relHeights.add(-.999 + (i * .001));
    }
    for (int i = 0; i < 100; i++) {
      relHeights.add(-.99 + (i * .05));
    }
    // for (int i = 0; i < 50; i++) {
    // relHeights.add(3.99 + (i * .5));
    // }

    final BufferedWriter writer = Files.newWriter(new File(
        "files/test/times/relheight-dynamism.txt"), Charsets.UTF_8);

    for (int i = 0; i < relHeights.size(); i++) {
      final double d = relHeights.get(i);
      final double relHeight = d;// -.99 + (j * .05);
      final double freq = 1d / 3600d;
      final double min = 0;// j * .10d;
      final SineIntensity intensity = new SineIntensity(
          1d, freq, relHeight, min);

      final UnivariateIntegrator ri = new RombergIntegrator(16, 32);// new
      // TrapezoidIntegrator();//
      // new
      // SimpsonIntegrator();//
      // new
      // RombergIntegrator();

      final double val = ri.integrate(10000000, new IntensityFunctionWrapper(
          intensity), 0, lengthOfScenario);
      System.out
          .printf("%1d relative height: %1.3f%n", i, relHeight);

      final double newAmpl = orders / val;

      final SineIntensity finalIntensity = new SineIntensity(
          newAmpl, freq, relHeight, newAmpl * min);
      final double compensatedArea = ri.integrate(10000000,
          new IntensityFunctionWrapper(
              finalIntensity), 0, lengthOfScenario);
      // System.out.printf("compensated area: %1.3f%n", compensatedArea);

      final List<Double> sineTimes = FluentIterable
          .from(
              ContiguousSet.create(Range.closedOpen(0L, lengthOfScenario),
                  DiscreteDomain.longs()))
          .transform(Conversion.LONG_TO_DOUBLE)
          .transform(finalIntensity)
          .toList();

      Analysis.writeLoads(
          sineTimes,
          new File(
              "files/test/times/sine/sine-"
                  + Strings.padStart(Integer.toString(i), 2, '0') + ".intens"));

      final ArrivalTimesGenerator generator = new NHPoissonProcess(
          lengthOfScenario,
          finalIntensity);

      double max = 0;
      double sum = 0;

      final StandardDeviation sd = new StandardDeviation();
      final List<Double> dynamismValues = newArrayList();
      for (int j = 0; j < numSamples; j++) {
        final List<Double> times = generator.generate(rng);

        final double dyn = Metrics.measureDynamism(times, lengthOfScenario);
        dynamismValues.add(dyn);
        sd.increment(dyn);
        sum += dyn;
        max = Math.max(max, dyn);
        if (j < 3) {
          // System.out.printf("%1.3f%% %d%n", dyn * 100, times.size());
          Analysis.writeTimes(
              lengthOfScenario,
              times,
              new File(
                  "files/test/times/orders"
                      + Strings.padStart(Integer.toString(i), 2, '0') + "_" + j
                      + "-" + (dyn * 100)
                      + ".times"));
        }
      }

      try {
        writer.append(Double.toString(relHeight));
        writer.append(" ");
        writer.append(Joiner.on(" ").join(dynamismValues).toString());
        writer.append("\n");

      } catch (final IOException e) {
        checkState(false);
      }
      System.out.printf(" > dyn %1.3f+-%1.3f%n",
          +(sum / numSamples), sd.getResult());
      dataPoints.add(new Point(relHeight, (sum / numSamples)));
    }
    writer.close();
    Analysis.writeLocationList(dataPoints, new File(
        "files/test/times/intensity-analysis.txt"));
  }

  private enum Conversion implements Function<Long, Double> {
    LONG_TO_DOUBLE {
      @Override
      public Double apply(Long input) {
        return new Double(input);
      }
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
