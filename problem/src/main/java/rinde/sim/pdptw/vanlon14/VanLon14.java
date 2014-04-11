package rinde.sim.pdptw.vanlon14;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.pdptw.common.DynamicPDPTWScenario.ProblemClass;
import rinde.sim.pdptw.common.ScenarioIO;
import rinde.sim.pdptw.generator.Analysis;
import rinde.sim.pdptw.generator.LoadRequirement;
import rinde.sim.pdptw.generator.Metrics;
import rinde.sim.pdptw.generator.ScenarioGeneratorOld;
import rinde.sim.pdptw.generator.ScenarioGeneratorOld.ScenarioFactory;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;

public class VanLon14 {

  public enum Dynamism {
    LOW(1d / 3d),
    MEDIUM(2d / 3d),
    HIGH(1.0);

    private final double value;

    Dynamism(double val) {
      value = val;
    }

    public double getValue() {
      return value;
    }
  }

  public enum Scale {
    SMALL(10d),
    MEDIUM(20d),
    LARGE(30d);

    private final double value;

    Scale(double val) {
      value = val;
    }

    public double getValue() {
      return value;
    }
  }

  public enum ExperimentClass implements ProblemClass {
    LOW_SMALL(Dynamism.LOW, Scale.SMALL),

    LOW_MEDIUM(Dynamism.LOW, Scale.MEDIUM),

    LOW_LARGE(Dynamism.LOW, Scale.LARGE),

    MEDIUM_SMALL(Dynamism.MEDIUM, Scale.SMALL),

    MEDIUM_MEDIUM(Dynamism.MEDIUM, Scale.MEDIUM),

    MEDIUM_LARGE(Dynamism.MEDIUM, Scale.LARGE),

    HIGH_SMALL(Dynamism.HIGH, Scale.SMALL),

    HIGH_MEDIUM(Dynamism.HIGH, Scale.MEDIUM),

    HIGH_LARGE(Dynamism.HIGH, Scale.LARGE);

    private final Dynamism dynamism;
    private final Scale scale;

    ExperimentClass(Dynamism d, Scale s) {
      dynamism = d;
      scale = s;
    }

    public Dynamism getDynamism() {
      return dynamism;
    }

    public Scale getScale() {
      return scale;
    }

    @Override
    public String getId() {
      return dynamism.name() + "-" + scale.name();
    }
  }

  static ImmutableList<Double> generateMeanLoadGraph(
      ScenarioGeneratorOld<?> generator, RandomGenerator rng, int numSamples) {
    final List<List<Double>> allLoads = newArrayList();
    for (int i = 0; i < numSamples; i++) {
      final Scenario s = generator.generate(rng);
      Metrics.checkTimeWindowStrictness(s);
      final List<Double> loads = Metrics.measureRelativeLoad(s);
      allLoads.add(loads);
    }
    final ImmutableList<Double> meanLoadGraph = ImmutableList.copyOf(Lists
        .transform(
            mean(allLoads),
            new Function<Double, Double>() {
              @Override
              @Nullable
              public Double apply(@Nullable Double input) {
                if (input == null || input.equals(0d)) {
                  return Double.MIN_VALUE;
                }
                return input;
              }
            }));
    return meanLoadGraph;
  }

  public static void generateDataSet(File parentDir) {

    final File dataSetDir = new File(parentDir, "vanlon14/");

    try {
      Files.createParentDirs(dataSetDir);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    checkState(dataSetDir.exists() || dataSetDir.mkdir());

    final DateTimeFormatter formatter = ISODateTimeFormat
        .dateHourMinuteSecondMillis();
    final RandomGenerator rng = new MersenneTwister(123);

    // 480 minutes or 8 hours
    final long dayLength = 480L;
    // 0.1 vehicle per km2
    final double vehicleDensity = .1d;
    // number of orders per km2 per hour
    final double orderIntensity = .20d;

    // number of samples drawn from each problem class for generating mean load
    // graph
    final int numSamples = 50;
    // number of scenarios to generate for each problem class
    final int numScenarios = 5;

    // deviation of the load graph
    final double maxMeanDeviation = .2;
    final double maxMaxDeviation = .5;

    final List<List<Double>> allLoads = newArrayList();
    for (final ExperimentClass ec : ExperimentClass.values()) {
      final ScenarioGeneratorOld<VanLon14Scenario> generator =
          ScenarioGeneratorOld
              .builder(new VanLon14ScenarioFactory(
                  ec))
              .setOrderIntensityAndDynamism(orderIntensity,
                  ec.getDynamism().getValue())
              .setScale(vehicleDensity, ec.getScale().getValue())
              .setScenarioLength(dayLength)
              .build();
      final List<Double> meanLoads = generateMeanLoadGraph(generator, rng,
          numSamples);

      Analysis.writeLoads(meanLoads, new File(dataSetDir, "mean"
          + ec.name().toLowerCase() + ".load"));
      allLoads.add(meanLoads);
    }
    final ImmutableList<Double> meanLoadGraph = ImmutableList.copyOf(Lists
        .transform(
            mean(allLoads),
            new Function<Double, Double>() {
              @Override
              @Nullable
              public Double apply(@Nullable Double input) {
                if (input == null || input.equals(0d)) {
                  return Double.MIN_VALUE;
                }
                return input;
              }
            }));

    Analysis.writeLoads(meanLoadGraph, new File(dataSetDir, "global.load"));

    // generation of scenarios starts here

    for (final ExperimentClass ec : ExperimentClass.values()) {
      final double scaledOrderIntensity = orderIntensity;
      // if (ec.getScale() == Scale.MEDIUM) {
      // scaledOrderIntensity = .22d;
      // }
      // else if (ec.getScale() == Scale.LARGE) {
      // scaledOrderIntensity = .4d;
      // }
      // System.out.println(ec);
      // System.out.println("order intensity: " + scaledOrderIntensity
      // );
      final ScenarioGeneratorOld<VanLon14Scenario> generator =
          ScenarioGeneratorOld
              .builder(new VanLon14ScenarioFactory(
                  ec))
              .setOrderIntensityAndDynamism(
                  scaledOrderIntensity,
                  ec.getDynamism().getValue())
              .setScale(vehicleDensity,
                  ec.getScale().getValue())
              .setScenarioLength(dayLength)
              .addRequirement(
                  new LoadRequirement(meanLoadGraph, maxMeanDeviation,
                      maxMaxDeviation, true))
              .build();

      final List<VanLon14Scenario> scenarios = generator.generate(rng,
          numScenarios).scenarios;

      final File dir = new File("files/dataset/vanlon14/"
          + ec.name().toLowerCase() + "/");
      try {
        Files.createParentDirs(dir);
        checkState(dir.exists() || dir.mkdir(), "Could not create dir %s.", dir);

        for (int i = 0; i < scenarios.size(); i++) {
          final VanLon14Scenario s = scenarios.get(i);
          final String scenarioName = ec
              .name()
              .toLowerCase() + "-" + i;
          Files.write(ScenarioIO.write(s),
              new File(dir, scenarioName + ".json"), Charsets.UTF_8);

          Analysis.writeLoads(Metrics.measureRelativeLoad(s), new File(dir,
              scenarioName + ".load"));
          Analysis.writeLocationList(Metrics.getServicePoints(s), new File(dir,
              scenarioName + ".points"));

          Analysis.writeTimes(s.getTimeWindow().end,
              Metrics.getArrivalTimes(s), new File(dir,
                  scenarioName + ".times"));

          Metrics.checkTimeWindowStrictness(s);

          final ImmutableMap.Builder<String, Object> properties = ImmutableMap
              .<String, Object> builder()
              .put("generation_date",
                  formatter.print(System.currentTimeMillis()))
              // FIXME
              // .put("dynamism", Metrics.measureDynamismOld(s))
              .put("vehicle_speed_kmh", Metrics.getVehicleSpeed(s));

          final ImmutableMultiset<Enum<?>> eventTypes = Metrics
              .getEventTypeCounts(s);
          for (final Multiset.Entry<Enum<?>> en : eventTypes.entrySet()) {
            properties.put(en.getElement().name(), en.getCount());
          }
          final double parcelToVehicleRatio = (double) eventTypes
              .count(PDPScenarioEvent.ADD_PARCEL)
              / eventTypes.count(PDPScenarioEvent.ADD_VEHICLE);
          properties.put("parcel_to_vehicle_ratio", parcelToVehicleRatio);

          // System.out.println(parcelToVehicleRatio + " " + eventTypes
          // .count(PDPScenarioEvent.ADD_PARCEL) + " "
          // + eventTypes.count(PDPScenarioEvent.ADD_VEHICLE));

          Files.write(
              Joiner.on("\n").withKeyValueSeparator(" = ")
                  .join(properties.build()),
              new File(dir, scenarioName + ".properties"), Charsets.UTF_8);
        }
      } catch (final IOException e) {
        throw new IllegalStateException(e);
      }
    }

  }

  public static class VanLon14ScenarioFactory implements
      ScenarioFactory<VanLon14Scenario> {
    private final ProblemClass pc;

    public VanLon14ScenarioFactory(
        ProblemClass problemClass) {
      pc = problemClass;
    }

    @Override
    public VanLon14Scenario create(List<TimedEvent> events,
        ScenarioGeneratorOld<VanLon14Scenario> generator, int instanceNumber) {

      final TimeWindow tw = new TimeWindow(0, generator.getScenarioLength());
      final Point min = generator.getMinPoint();
      final Point max = generator.getMaxPoint();
      final long tickSize = generator.getTickSize();
      return new VanLon14Scenario(events, tw, min, max, tickSize, pc,
          instanceNumber);
    }
  }

  // use DoubleMath.mean(..) instead
  @Deprecated
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
