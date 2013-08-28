/**
 * 
 */
package rinde.sim.pdptw.experiments;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.model.Model;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DynamicPDPTWProblem;
import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.pdptw.common.ObjectiveFunction;
import rinde.sim.pdptw.common.ScenarioParser;
import rinde.sim.pdptw.common.StatsTracker.StatisticsDTO;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public final class Experiments {

  // TODO add strict mode which checks whether there are not too many
  // vehicles/parcels/depots?

  private Experiments() {}

  // TODO make sure Central can reuse this code

  // experimentOn(scenario).addSolution(config).addObjectiveFunction(objFunc).showGui().setMasterSeed(123).repeat(10).run()
  // ..addSolution(config1).addSolution(config2).addScenario(scen1)

  public static ExperimentSetup experiment(ObjectiveFunction objectiveFunction) {
    return new ExperimentSetup(objectiveFunction);
  }

  public static final class ExperimentSetup {
    private final ObjectiveFunction objectiveFunction;
    private final ImmutableList.Builder<MASConfigurator> configurators;
    private final ImmutableList.Builder<DynamicPDPTWScenario> scenarios;
    private boolean showGui;
    private int repetitions;
    private long masterSeed;

    private ExperimentSetup(ObjectiveFunction objectiveFunction) {
      this.objectiveFunction = objectiveFunction;
      configurators = ImmutableList.builder();
      scenarios = ImmutableList.builder();
      showGui = false;
      repetitions = 1;
      masterSeed = 0L;
    }

    public ExperimentSetup repeat(int repetitions) {
      checkArgument(repetitions > 0);
      this.repetitions = repetitions;
      return this;
    }

    public ExperimentSetup showGui() {
      showGui = true;
      return this;
    }

    public ExperimentSetup addSolution(MASConfigurator config) {
      configurators.add(config);
      return this;
    }

    public ExperimentSetup addScenario(DynamicPDPTWScenario scenario) {
      scenarios.add(scenario);
      return this;
    }

    public ExperimentSetup addScenarios(List<DynamicPDPTWScenario> scenarios) {
      this.scenarios.addAll(scenarios);
      return this;
    }

    public ExperimentSetup addScenarioProvider(ScenarioProvider scenarioProvider) {
      scenarios.addAll(scenarioProvider.provide());
      return this;
    }

    public ExperimentSetup addScenarios(
        ScenarioParser<? extends DynamicPDPTWScenario> parser,
        List<String> files) {
      for (final String file : files) {
        scenarios.add(parser.parse(file));
      }
      return this;
    }

    public ExperimentSetup withRandomSeed(long seed) {
      masterSeed = seed;
      return this;
    }

    public ExperimentResults perform() {
      final ImmutableList<DynamicPDPTWScenario> scen = scenarios.build();
      final ImmutableList<MASConfigurator> conf = configurators.build();

      checkArgument(!scen.isEmpty());
      checkArgument(!conf.isEmpty());
      final RandomGenerator rng = new MersenneTwister(masterSeed);
      final ImmutableList.Builder<Result> resultBuilder = ImmutableList
          .builder();

      final List<Long> seeds = ExperimentUtil
          .generateDistinct(rng, repetitions);
      for (final DynamicPDPTWScenario scenario : scen) {
        for (final MASConfigurator solution : conf) {
          for (int i = 0; i < repetitions; i++) {
            final long seed = seeds.get(i);
            final StatisticsDTO stats = singleRun(scenario, solution.configure(seed), objectiveFunction, showGui);
            resultBuilder.add(new Result(stats, scenario, solution, seed));
          }

        }
      }
      return new ExperimentResults(this, resultBuilder.build());
    }
  }

  public static class Result {
    public final StatisticsDTO stats;
    public final DynamicPDPTWScenario scenario;
    public final MASConfigurator masConfigurator;
    public final long seed;

    private Result(StatisticsDTO stats, DynamicPDPTWScenario scenario,
        MASConfigurator masConfigurator, long seed) {
      this.stats = stats;
      this.scenario = scenario;
      this.masConfigurator = masConfigurator;
      this.seed = seed;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this) //
          .add("stats", stats) //
          .add("scenario", scenario) //
          .add("masConfigurator", masConfigurator) //
          .add("seed", seed).toString();
    }
  }

  public static final class ExperimentResults {
    public final ObjectiveFunction objectiveFunction;
    public final ImmutableList<MASConfigurator> configurators;
    public final ImmutableList<DynamicPDPTWScenario> scenarios;
    public final boolean showGui;
    public final int repetitions;
    public final long masterSeed;
    public final ImmutableList<Result> results;

    private ExperimentResults(ExperimentSetup exp, ImmutableList<Result> results) {
      objectiveFunction = exp.objectiveFunction;
      configurators = exp.configurators.build();
      scenarios = exp.scenarios.build();
      showGui = exp.showGui;
      repetitions = exp.repetitions;
      masterSeed = exp.masterSeed;
      this.results = results;
    }
  }

  static StatisticsDTO singleRun(DynamicPDPTWScenario scenario,
      MASConfiguration c, ObjectiveFunction objFunc, boolean showGui) {

    final StatisticsDTO stats = init(scenario, c, showGui).simulate();

    checkState(objFunc.isValidResult(stats), "The simulation did not result in a valid result: %s.", stats);

    return stats;
  }

  @VisibleForTesting
  static DynamicPDPTWProblem init(DynamicPDPTWScenario scenario,
      MASConfiguration config, boolean showGui) {
    final DynamicPDPTWProblem problem = new DynamicPDPTWProblem(scenario, 123,
        config.getModels().toArray(new Model<?>[] {}));

    problem.addCreator(AddVehicleEvent.class, config.getVehicleCreator());
    if (config.getDepotCreator().isPresent()) {
      problem.addCreator(AddDepotEvent.class, config.getDepotCreator().get());
    }
    if (config.getParcelCreator().isPresent()) {
      problem.addCreator(AddParcelEvent.class, config.getParcelCreator().get());
    }
    if (showGui) {
      problem.enableUI();
    }
    return problem;
  }

}
