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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.UnmodifiableIterator;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Experiments {

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
      final ImmutableTable.Builder<DynamicPDPTWScenario, MASConfigurator, ImmutableList<StatisticsDTO>> resultBuilder = ImmutableTable
          .builder();

      for (final DynamicPDPTWScenario scenario : scen) {
        for (final MASConfigurator solution : conf) {
          final ImmutableList.Builder<StatisticsDTO> statsBuilder = ImmutableList
              .builder();
          for (int i = 0; i < repetitions; i++) {
            statsBuilder.add(singleRun(scenario, solution.configure(rng
                .nextLong()), objectiveFunction, showGui));
          }
          resultBuilder.put(scenario, solution, statsBuilder.build());
        }
      }
      return new ExperimentResults(resultBuilder.build());
    }
  }

  public static final class ExperimentResults {
    private final ImmutableTable<DynamicPDPTWScenario, MASConfigurator, ImmutableList<StatisticsDTO>> results;

    private ExperimentResults(
        ImmutableTable<DynamicPDPTWScenario, MASConfigurator, ImmutableList<StatisticsDTO>> results) {
      this.results = results;
    }

    public UnmodifiableIterator<Cell<DynamicPDPTWScenario, MASConfigurator, ImmutableList<StatisticsDTO>>> iterator() {
      return results.cellSet().iterator();
    }

    public ImmutableList<StatisticsDTO> getAllResultsFor(
        DynamicPDPTWScenario scenario, MASConfigurator configurator) {
      return results.get(scenario, configurator);
    }

  }

  // public static ImmutableList<StatisticsDTO> run(ScenarioParser sp,
  // ImmutableList<String> files, MASConfigurator config) {
  // for (final String f : files) {
  //
  // }
  // }
  //
  // public static ImmutableList<StatisticsDTO>
  // run(ImmutableList<DynamicPDPTWScenario> scenarios, MASConfigurator
  // config, ObjectiveFunction objFunc,boolean showGui){
  //
  //
  // for(final DynamicPDPTWScenario scen : scenarios ){
  // singleRun(scen, config.configure(seed), objFunc, showGui)
  // }
  // }

  static StatisticsDTO singleRun(DynamicPDPTWScenario scenario,
      MASConfiguration c, ObjectiveFunction objFunc, boolean showGui) {

    final StatisticsDTO stats = init(scenario, c).simulate();

    checkState(objFunc.isValidResult(stats), "The simulation did not result in a valid result: %s.", stats);

    return stats;
  }

  @VisibleForTesting
  static DynamicPDPTWProblem init(DynamicPDPTWScenario scenario,
      MASConfiguration config) {
    final DynamicPDPTWProblem problem = new DynamicPDPTWProblem(scenario, 123,
        config.getModels().toArray(new Model<?>[] {}));

    problem.addCreator(AddVehicleEvent.class, config.getVehicleCreator());
    if (config.getDepotCreator().isPresent()) {
      problem.addCreator(AddDepotEvent.class, config.getDepotCreator().get());
    }
    if (config.getParcelCreator().isPresent()) {
      problem.addCreator(AddParcelEvent.class, config.getParcelCreator().get());
    }
    return problem;
  }

}
