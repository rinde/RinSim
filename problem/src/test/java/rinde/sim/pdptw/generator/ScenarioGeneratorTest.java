/**
 * 
 */
package rinde.sim.pdptw.generator;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Test;

import rinde.sim.scenario.Scenario;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ScenarioGeneratorTest {

  @Test
  public void test() {
    final ScenarioGenerator sg = ScenarioGenerator.builder()
        .setAnnouncementIntensityPerKm2(1.3d).setOrdersPerAnnouncement(1.3) //
        .setScale(.1, 5) //
        .setScenarioLength(240) //
        .build();
    final RandomGenerator rng = new MersenneTwister(123);

    for (int i = 0; i < 10; i++) {
      final Scenario s = sg.generate(rng);

      Metrics.checkTimeWindowStrictness(s);

      final List<Double> loads = Metrics.measureLoad(s);
      writeLoads(loads, new File("files/generator/load/scenario" + i + ".load"));

      // measure dynamism
      // measure load

      // System.out.println(s.size() + " " + Metrics.measureDynamism(s));
    }

  }

  @Test
  public void test2() {
    final ScenarioGenerator sg = ScenarioGenerator.builder()
        .setAnnouncementIntensityPerKm2(1.3d).setOrdersPerAnnouncement(1.3) //
        .setScale(.1, 5) //
        .setScenarioLength(480) //
        .build();
    final RandomGenerator rng = new MersenneTwister(123);

    final List<List<Double>> allLoads = newArrayList();
    for (int i = 0; i < 1000; i++) {
      final Scenario s = sg.generate(rng);
      Metrics.checkTimeWindowStrictness(s);
      final List<Double> loads = Metrics.measureLoad(s);
      allLoads.add(loads);
    }

    final ChiSquareTest tester = new ChiSquareTest();

    final List<Double> mean = Lists.transform(mean(allLoads),
        new Function<Double, Double>() {
          @Override
          @Nullable
          public Double apply(@Nullable Double input) {
            if (input.equals(0d)) {
              return Double.MIN_VALUE;
            }
            return input;
          }
        });

    for (int i = 0; i < 100; i++) {
      final Scenario s = sg.generate(rng);
      Metrics.checkTimeWindowStrictness(s);
      final List<Double> loads = newArrayList(Metrics.measureLoad(s));
      final int toAdd = mean.size() - loads.size();
      for (int j = 0; j < toAdd; j++) {
        loads.add(0d);
      }

      final Collection<Long> convertedLoads = Collections2.transform(loads,
          new Function<Double, Long>() {
            @Override
            @Nullable
            public Long apply(@Nullable Double input) {
              return DoubleMath.roundToLong(input * 1000000d,
                  RoundingMode.HALF_DOWN);
            }
          });

      System.out.println(tester.chiSquareTest(Doubles.toArray(mean),
          Longs.toArray(convertedLoads), 0.00000001d));
    }

    writeLoads(mean, new File(
        "files/generator/load/scenarios-mean.load"));
  }

  static List<Double> mean(List<List<Double>> lists) {
    final ImmutableList.Builder<Double> builder = ImmutableList.builder();
    boolean running = true;
    int i = 0;
    while (running) {
      running = false;
      double sum = 0d;
      for (final List<Double> list : lists) {
        if (i < list.size()) {
          running = true;
          sum += list.get(i);
        }
      }
      sum /= lists.size();
      builder.add(sum);
      i++;
    }
    return builder.build();
  }

  static void writeLoads(List<Double> loads, File f) {
    final StringBuilder sb = new StringBuilder();
    int i = 0;
    for (; i < loads.size(); i++) {
      sb.append(i).append(" ").append(loads.get(i)).append("\n");
    }
    sb.append(i).append(" ").append(0).append("\n");
    try {
      Files.createParentDirs(f);
      Files.write(sb.toString(), f, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
