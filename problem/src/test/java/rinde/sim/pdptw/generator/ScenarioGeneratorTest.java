/**
 * 
 */
package rinde.sim.pdptw.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Ignore;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.scenario.Scenario;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ScenarioGeneratorTest {

  @Test
  @Ignore
  public void test() {
    final ScenarioGeneratorOld sg = ScenarioGeneratorOld.builder()
        .setAnnouncementIntensityPerKm2(1.3d)
        .setOrdersPerAnnouncement(1.3)
        .setScale(.1, 5)
        .setScenarioLength(240)
        .build();
    final RandomGenerator rng = new MersenneTwister(123);

    for (int i = 0; i < 10; i++) {
      final Scenario s = sg.generate(rng);

      Metrics.checkTimeWindowStrictness(s);

      final List<Double> loads = Metrics.measureLoad(s);
      Analysis.writeLoads(loads, new File("files/generator/load/scenario" + i
          + ".load"));

      // measure dynamism
      // measure load

      // System.out.println(s.size() + " " + Metrics.measureDynamism(s));
    }

  }

  @Test
  @Ignore
  public void test2() {
    final ScenarioGeneratorOld.Builder sg = ScenarioGeneratorOld.builder()
        .setAnnouncementIntensityPerKm2(0.3d).setOrdersPerAnnouncement(1.3) //
        .setScale(.1, 5) //
        .setScenarioLength(480);

    final RandomGenerator rng = new MersenneTwister(123);

    final List<List<Double>> allLoads = newArrayList();
    for (int i = 0; i < 10; i++) {
      final Scenario s = sg.build().generate(rng);
      Metrics.checkTimeWindowStrictness(s);
      final List<Double> loads = Metrics.measureLoad(s);
      allLoads.add(loads);
    }

    final ImmutableList<Double> mean = ImmutableList.copyOf(Lists.transform(
        mean(allLoads),
        new Function<Double, Double>() {
          @Override
          @Nullable
          public Double apply(@Nullable Double input) {
            if (input.equals(0d)) {
              return Double.MIN_VALUE;
            }
            return input;
          }
        }));

    final ScenarioGeneratorOld dsg = sg.addRequirement(
        new LoadRequirement(mean, 4, 6, false)).build();

    final List<Scenario> scenarios = dsg.generate(rng, 2).scenarios;

    for (int i = 0; i < scenarios.size(); i++) {
      final List<Point> points = Metrics.getServicePoints(scenarios.get(i));
      final List<Double> loads = newArrayList(Metrics.measureLoad(scenarios
          .get(i)));

      final String name = "files/generator/good/scenario-";
      Analysis.writeLocationList(points, new File(
          name + i + ".points"));

      Analysis.writeLoads(loads, new File(
          name + i + ".load"));

    }

    // sg.generate(rng,10,ScenarioRequirements
    // .withOrdersMin(30)
    // .withOrdersMax(50)
    // .withLoad(loadGraph)
    // .maxMeanDeviation(.3)
    // .maxMaxDeviation(2)
    // .

    // OR
    // DataSetGenerator.create(scenarioGenerator,rng)
    // .maxComputationTime(70)
    // .numScenarios(10)
    // and scenario requirements

    // int good = 0;
    // for (int i = 0; i < 1000; i++) {
    // final Scenario s = sg.generate(rng);
    // Metrics.checkTimeWindowStrictness(s);
    // final List<Double> loads = newArrayList(Metrics.measureLoad(s));
    // final int toAdd = mean.size() - loads.size();
    // for (int j = 0; j < toAdd; j++) {
    // loads.add(0d);
    // }
    //
    // final double[] deviations = abs(subtract(Doubles.toArray(mean),
    // Doubles.toArray(loads)));
    // final double m = StatUtils.mean(deviations);
    // final double min = Doubles.min(deviations);
    // final double max = Doubles.max(deviations);
    //
    // System.out.println(s.asList().size());
    // if (m < .3 && max < 2) {
    //
    // System.out.println("-");
    // System.out.println("mean: " + m);
    // System.out.println("min: " + min);
    // System.out.println("max: " + max);
    //
    // final List<Point> points = Metrics.getServicePoints(s);
    //
    // final String name = "files/generator/good/scenario-";
    // Analysis.writeLocationList(points, new File(
    // name + good + ".points"));
    //
    // writeLoads(loads, new File(
    // name + good + ".load"));
    // good++;
    // }
    // }

    Analysis.writeLoads(mean, new File(
        "files/generator/load/scenarios-mean.load"));
  }

  static double[] subtract(double[] arr1, double[] arr2) {
    checkArgument(arr1.length == arr2.length);
    final double[] res = new double[arr1.length];
    for (int i = 0; i < arr1.length; i++) {
      res[i] = arr1[i] - arr2[i];
    }
    return res;
  }

  static double[] abs(double[] arr) {
    final double[] res = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      res[i] = Math.abs(arr[i]);
    }
    return res;
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
}
