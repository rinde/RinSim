package rinde.sim.pdptw.generator;

import javax.measure.Measure;
import javax.measure.quantity.Duration;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.scenario.Scenario;
import rinde.sim.util.spec.Specification.ISpecification;

import com.google.common.collect.ImmutableList;

public class DataSetGenerator {

  private final ScenarioGenerator generator;
  private final ISpecification<Scenario> specification;
  private final long maxMilliSeconds;
  private final int numScenarios;

  DataSetGenerator(ScenarioGenerator gen, ISpecification<Scenario> spec,
      long maxCompTime, int numScen) {
    generator = gen;
    specification = spec;
    maxMilliSeconds = maxCompTime;
    numScenarios = numScen;

  }

  public ImmutableList<Scenario> generate(RandomGenerator rng) {
    final long start = System.currentTimeMillis();
    long duration;

    final ImmutableList.Builder<Scenario> scenarioListBuilder = ImmutableList
        .builder();
    int foundScenarios = 0;
    do {
      final Scenario s = generator.generate(rng);
      if (specification.isSatisfiedBy(s)) {
        scenarioListBuilder.add(s);
        foundScenarios++;
      }
      duration = System.currentTimeMillis() - start;
    } while (duration < maxMilliSeconds && foundScenarios < numScenarios);

    // TODO
    // perhaps add some statistics about number of attempts

    return scenarioListBuilder.build();
  }

  public static Builder create(ScenarioGenerator generator) {
    return new Builder(generator);
  }

  private static final class Builder {
    private final ScenarioGenerator generator;

    Builder(ScenarioGenerator g) {
      generator = g;
    }

    public Builder numScenarios(int num) {
      return this;
    }

    public Builder maxComputationTime(Measure<Long, Duration> time) {
      return this;
    }

    public Builder addSpecification(ISpecification<Scenario> spec) {
      return this;
    }

    public DataSetGenerator build() {
      return null;
    }
  }
}
