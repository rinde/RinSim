/**
 * 
 */
package rinde.sim.pdptw.experiments;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

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

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Experiments {

  // TODO add strict mode which checks whether there are not too many
  // vehicles/parcels/depots?

  private Experiments() {}

  // TODO make sure Central can reuse this code

  // TODO builder pattern?

  // experimentOn(scenario).addSolution(config).addObjectiveFunction(objFunc).showGui().setMasterSeed(123).repeat(10).run()
  // ..addSolution(config1).addSolution(config2).addScenario(scen1)

  public static class ExperimentBuilder {
    private final ObjectiveFunction objectiveFunction;
    private final List<SimulatorConfigurator> solutions;
    private final List<DynamicPDPTWScenario> scenarios;
    private boolean showGui;
    private int repetitions;
    private long masterSeed;

    private ExperimentBuilder(ObjectiveFunction objectiveFunction) {
      this.objectiveFunction = objectiveFunction;
      solutions = newArrayList();
      scenarios = newArrayList();
      showGui = false;
      repetitions = 1;
      masterSeed = 0L;
    }

    public ExperimentBuilder repeat(int repetitions) {
      checkArgument(repetitions > 0);
      this.repetitions = repetitions;
      return this;
    }

    public ExperimentBuilder showGui() {
      showGui = true;
      return this;
    }

    public ExperimentBuilder addSolution(SimulatorConfigurator config) {
      solutions.add(config);
      return this;
    }

    public ExperimentBuilder addScenario(DynamicPDPTWScenario scenario) {
      scenarios.add(scenario);
      return this;
    }

    public ExperimentBuilder addScenarios(List<DynamicPDPTWScenario> scenarios) {
      scenarios.addAll(scenarios);
      return this;
    }

    public ExperimentBuilder addScenarios(
        ScenarioParser<? extends DynamicPDPTWScenario> parser,
        List<String> files) {
      for (final String file : files) {
        scenarios.add(parser.parse(file));
      }
      return this;
    }

    public ExperimentBuilder withRandomSeed(long seed) {
      masterSeed = seed;
      return this;
    }

    public ImmutableTable<DynamicPDPTWScenario, SimulatorConfigurator, ImmutableList<StatisticsDTO>> start() {
      checkArgument(!scenarios.isEmpty());
      checkArgument(!solutions.isEmpty());
      final RandomGenerator rng = new MersenneTwister(masterSeed);
      final ImmutableTable.Builder<DynamicPDPTWScenario, SimulatorConfigurator, ImmutableList<StatisticsDTO>> resultBuilder = ImmutableTable
          .builder();

      for (final DynamicPDPTWScenario scenario : scenarios) {
        for (final SimulatorConfigurator solution : solutions) {
          final ImmutableList.Builder<StatisticsDTO> statsBuilder = ImmutableList
              .builder();
          for (int i = 0; i < repetitions; i++) {
            statsBuilder.add(singleRun(scenario, solution.configure(rng
                .nextLong()), objectiveFunction, showGui));
          }
          resultBuilder.put(scenario, solution, statsBuilder.build());
        }
      }
      return resultBuilder.build();
    }
  }

  // public static ImmutableList<StatisticsDTO> run(ScenarioParser sp,
  // ImmutableList<String> files, SimulatorConfigurator config) {
  // for (final String f : files) {
  //
  // }
  // }
  //
  // public static ImmutableList<StatisticsDTO>
  // run(ImmutableList<DynamicPDPTWScenario> scenarios, SimulatorConfigurator
  // config, ObjectiveFunction objFunc,boolean showGui){
  //
  //
  // for(final DynamicPDPTWScenario scen : scenarios ){
  // singleRun(scen, config.configure(seed), objFunc, showGui)
  // }
  // }

  public static StatisticsDTO singleRun(DynamicPDPTWScenario scenario,
      SimulatorConfiguration c, ObjectiveFunction objFunc, boolean showGui) {

    final StatisticsDTO stats = init(scenario, c).simulate();

    checkState(objFunc.isValidResult(stats), "The simulation did not result in a valid result: %s.", stats);

    return stats;
  }

  @VisibleForTesting
  static DynamicPDPTWProblem init(DynamicPDPTWScenario scenario,
      SimulatorConfiguration config) {
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
