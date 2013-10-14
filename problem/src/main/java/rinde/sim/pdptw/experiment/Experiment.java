/**
 * 
 */

package rinde.sim.pdptw.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.model.Model;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DynamicPDPTWProblem;
import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.pdptw.common.ObjectiveFunction;
import rinde.sim.pdptw.common.RouteRenderer;
import rinde.sim.pdptw.common.ScenarioParser;
import rinde.sim.pdptw.common.StatisticsDTO;
import rinde.sim.util.SupplierRng;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

/**
 * Utility for defining and performing experiments. An experiment is composed of
 * a set of {@link DynamicPDPTWScenario}s and a set of {@link SupplierRng}s that
 * supply {@link MASConfiguration}s. For <b>each</b> combination of these a user
 * configurable number of simulations is performed.
 * <p>
 * <b>Example</b> Consider an experiment with three scenarios and two
 * configurators, and each simulation needs to be repeated twice. The code
 * required for this setup:
 * 
 * <pre>
 * {@code
 * Experiment.experiment(objFunc)
 *    .addConfigurator(config1)
 *    .addConfigurator(config2)
 *    .addScenario(scen1)
 *    .addScenarios(asList(scen2,scen3))
 *    .repeat(2)
 *    .perform();
 *    }
 * </pre>
 * 
 * The following simulations will be run:
 * <ol>
 * <li>config1, scen1, seed1</li>
 * <li>config1, scen1, seed2</li>
 * <li>config1, scen2, seed1</li>
 * <li>config1, scen2, seed2</li>
 * <li>config1, scen3, seed1</li>
 * <li>config1, scen3, seed2</li>
 * <li>config2, scen1, seed1</li>
 * <li>config2, scen1, seed2</li>
 * <li>config2, scen2, seed1</li>
 * <li>config2, scen2, seed2</li>
 * <li>config2, scen3, seed1</li>
 * <li>config2, scen3, seed2</li>
 * </ol>
 * For each simulation a {@link SimulationResult} returned.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class Experiment {
  // TODO add strict mode which checks whether there are not too many
  // vehicles/parcels/depots?

  private Experiment() {}

  /**
   * Create an experiment with the specified {@link ObjectiveFunction}.
   * @param objectiveFunction The objective function which is used to evaluate
   *          all simulation runs.
   * @return An {@link Builder} instance as per the builder pattern.
   */
  public static Builder build(ObjectiveFunction objectiveFunction) {
    return new Builder(objectiveFunction);
  }

  /**
   * Builder for configuring experiments.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static final class Builder {
    final ObjectiveFunction objectiveFunction;
    final ImmutableList.Builder<SupplierRng<MASConfiguration>> configuratorsBuilder;
    final ImmutableList.Builder<DynamicPDPTWScenario> scenariosBuilder;
    boolean showGui;
    int repetitions;
    long masterSeed;
    private int numThreads;

    Builder(ObjectiveFunction objectiveFunction) {
      this.objectiveFunction = objectiveFunction;
      configuratorsBuilder = ImmutableList.builder();
      scenariosBuilder = ImmutableList.builder();
      showGui = false;
      repetitions = 1;
      masterSeed = 0L;
      numThreads = 1;
    }

    /**
     * Set the number of repetitions for each simulation.
     * @param times The number of repetitions.
     * @return This, as per the builder pattern.
     */
    public Builder repeat(int times) {
      checkArgument(times > 0);
      repetitions = times;
      return this;
    }

    /**
     * Enable the GUI for each simulation. When a large number of simulations is
     * performed this may slow down the experiment significantly. The GUI can
     * not be shown when more than one thread is used.
     * @return This, as per the builder pattern.
     */
    public Builder showGui() {
      showGui = true;
      return this;
    }

    /**
     * Add a configurator to the experiment. For each simulation
     * {@link SupplierRng#get(long)} is called and the resulting
     * {@link MASConfiguration} is used for a <i>single</i> simulation.
     * @param config The configurator to add.
     * @return This, as per the builder pattern.
     */
    public Builder addConfigurator(SupplierRng<MASConfiguration> config) {
      configuratorsBuilder.add(config);
      return this;
    }

    /**
     * Adds all configurators to the experiment. For each simulation
     * {@link SupplierRng#get(long)} is called and the resulting
     * {@link MASConfiguration} is used for a <i>single</i> simulation.
     * @param configs The configurators to add.
     * @return This, as per the builder pattern.
     */
    public Builder addConfigurators(List<SupplierRng<MASConfiguration>> configs) {
      configuratorsBuilder.addAll(configs);
      return this;
    }

    /**
     * Add a scenario to the set of scenarios.
     * @param scenario The scenario to add.
     * @return This, as per the builder pattern.
     */
    public Builder addScenario(DynamicPDPTWScenario scenario) {
      scenariosBuilder.add(scenario);
      return this;
    }

    /**
     * Add all scenarios to the set of scenarios.
     * @param scenarios The scenarios to add.
     * @return This, as per the builder pattern.
     */
    public Builder addScenarios(List<DynamicPDPTWScenario> scenarios) {
      scenariosBuilder.addAll(scenarios);
      return this;
    }

    /**
     * Parse all scenarios with the given file names and parse them using the
     * given parser.
     * @param parser The parser to use for parsing.
     * @param files The files to parse.
     * @return This, as per the builder pattern.
     */
    public Builder addScenarios(
        ScenarioParser<? extends DynamicPDPTWScenario> parser,
        List<String> files) {
      for (final String file : files) {
        scenariosBuilder.add(parser.parse(file));
      }
      return this;
    }

    /**
     * Add all scenarios which are generated by the specified
     * {@link ScenarioProvider}.
     * @param sp The scenario provider to use.
     * @return This, as per the builder pattern.
     */
    public Builder addScenarioProvider(ScenarioProvider sp) {
      scenariosBuilder.addAll(sp.provide());
      return this;
    }

    /**
     * Specify the number of threads to use for computing the experiments, the
     * default is <code>1</code>.
     * @param threads The number of threads to use.
     * @return This, as per the builder pattern.
     */
    public Builder withThreads(int threads) {
      checkArgument(threads > 0,
          "Only a positive number of threads is allowed, was %s.", threads);
      numThreads = threads;
      return this;
    }

    /**
     * Set the master random seed for the experiments.
     * @param seed The seed to use.
     * @return This, as per the builder pattern.
     */
    public Builder withRandomSeed(long seed) {
      masterSeed = seed;
      return this;
    }

    /**
     * Perform the experiment. For every scenario every configurator is used
     * <code>n</code> times. Where <code>n</code> is the number of repetitions
     * as specified.
     * @return An {@link ExperimentResults} instance which contains all
     *         experiment parameters and the corresponding results.
     */
    public ExperimentResults perform() {
      checkArgument(numThreads == 1 || !showGui,
          "The GUI can not be shown when using more than one thread.");
      final ImmutableList<DynamicPDPTWScenario> scen = scenariosBuilder.build();
      final ImmutableList<SupplierRng<MASConfiguration>> conf = configuratorsBuilder
          .build();

      checkArgument(!scen.isEmpty(), "At least one scenario is required.");
      checkArgument(!conf.isEmpty(), "At least one configurator is required.");

      final RandomGenerator rng = new MersenneTwister(masterSeed);
      final List<Long> seeds = ExperimentUtil
          .generateDistinct(rng, repetitions);

      // gather all runners
      final ImmutableList.Builder<ExperimentRunner> runnerBuilder = ImmutableList
          .builder();
      for (final SupplierRng<MASConfiguration> configurator : conf) {
        for (final DynamicPDPTWScenario scenario : scen) {
          for (int i = 0; i < repetitions; i++) {
            final long seed = seeds.get(i);
            runnerBuilder.add(new ExperimentRunner(scenario, configurator,
                seed, objectiveFunction, showGui));
          }
        }
      }
      final List<ExperimentRunner> runners = runnerBuilder.build();

      if (numThreads > 1) {
        final ExecutorService executor = Executors
            .newFixedThreadPool(numThreads);
        for (final ExperimentRunner er : runners) {
          executor.execute(er);
        }
        executor.shutdown();
        try {
          executor.awaitTermination(10, TimeUnit.DAYS);
        } catch (final InterruptedException e) {
          throw new IllegalStateException(e);
        }
      } else {
        for (final ExperimentRunner er : runners) {
          er.run();
        }
      }

      final ImmutableList.Builder<SimulationResult> resultBuilder = ImmutableList
          .builder();
      for (final ExperimentRunner er : runners) {
        final SimulationResult sr = er.getResult();
        if (sr != null) {
          resultBuilder.add(sr);
        } else {
          // FIXME need some way to gracefully handle this error. All data
          // should be saved to reproduce this simulation.
          System.err.println("Found a null result");
        }
      }
      return new ExperimentResults(this, resultBuilder.build());
    }
  }

  private static class ExperimentRunner implements Runnable {

    private final DynamicPDPTWScenario scenario;
    private final SupplierRng<MASConfiguration> configurator;
    private final long seed;
    private final ObjectiveFunction objectiveFunction;
    private final boolean showGui;

    @Nullable
    private SimulationResult result;

    ExperimentRunner(DynamicPDPTWScenario scenario,
        SupplierRng<MASConfiguration> configurator, long seed,
        ObjectiveFunction objectiveFunction, boolean showGui) {
      this.scenario = scenario;
      this.configurator = configurator;
      this.seed = seed;
      this.objectiveFunction = objectiveFunction;
      this.showGui = showGui;
    }

    @Override
    public void run() {
      MASConfiguration masConfig = null;
      try {
        masConfig = configurator.get(seed);
        final StatisticsDTO stats = performSingleRun(scenario, masConfig,
            objectiveFunction, showGui);
        result = new SimulationResult(stats, scenario, configurator, seed);
      } catch (final RuntimeException e) {
        final StringBuilder sb = new StringBuilder().append("[Scenario= ")
            .append(scenario).append(",").append(scenario.getProblemClass())
            .append(",").append(scenario.getProblemInstanceId()).append("]")
            .append(",seed=").append(seed).append(",[Configurator=")
            .append(configurator).append(",config=").append(masConfig)
            .append("]");
        throw new RuntimeException(sb.toString(), e);
      }
      // FIXME this should be changed into a more decent progress indicator
      System.out.print(".");
    }

    @Nullable
    SimulationResult getResult() {
      return result;
    }
  }

  /**
   * The result of a single simulation. It contains both the resulting
   * statistics as well as the inputs used to obtain this result.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static final class SimulationResult {
    /**
     * The simulation statistics.
     */
    public final StatisticsDTO stats;

    /**
     * The scenario on which the simulation was run.
     */
    public final DynamicPDPTWScenario scenario;

    /**
     * The configurator which was used to configure the MAS.
     */
    public final SupplierRng<MASConfiguration> masConfigurator;

    /**
     * The seed that was supplied to {@link SupplierRng#get(long)}.
     */
    public final long seed;

    SimulationResult(StatisticsDTO stats, DynamicPDPTWScenario scenario,
        SupplierRng<MASConfiguration> masConfigurator, long seed) {
      this.stats = stats;
      this.scenario = scenario;
      this.masConfigurator = masConfigurator;
      this.seed = seed;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      if (obj.getClass() != getClass()) {
        return false;
      }
      final SimulationResult other = (SimulationResult) obj;
      return new EqualsBuilder().append(stats, other.stats)
          .append(scenario, other.scenario)
          .append(masConfigurator, other.masConfigurator)
          .append(seed, other.seed).isEquals();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(stats, scenario, masConfigurator, seed);
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

  /**
   * Value object containing all the result of a single experiment as performed
   * by {@link Builder#perform()}.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static final class ExperimentResults {
    /**
     * The {@link ObjectiveFunction} that was used for this experiment.
     */
    public final ObjectiveFunction objectiveFunction;

    /**
     * The configurators that were used in this experiment.
     */
    public final ImmutableList<SupplierRng<MASConfiguration>> configurators;

    /**
     * The scenarios that were used in this experiment.
     */
    public final ImmutableList<DynamicPDPTWScenario> scenarios;

    /**
     * Indicates whether the experiment was executed with or without the
     * graphical user interface.
     */
    public final boolean showGui;

    /**
     * The number of repetitions for each run (with a different seed).
     */
    public final int repetitions;

    /**
     * The seed of the master random generator.
     */
    public final long masterSeed;

    /**
     * The list of individual simulation results.
     */
    public final ImmutableList<SimulationResult> results;

    ExperimentResults(Builder exp, ImmutableList<SimulationResult> res) {
      objectiveFunction = exp.objectiveFunction;
      configurators = exp.configuratorsBuilder.build();
      scenarios = exp.scenariosBuilder.build();
      showGui = exp.showGui;
      repetitions = exp.repetitions;
      masterSeed = exp.masterSeed;
      results = res;
    }
  }

  /**
   * Can be used to run a single simulation run.
   * @param scenario The scenario to run on.
   * @param configuration The configuration to use.
   * @param objFunc The {@link ObjectiveFunction} to use.
   * @param showGui If <code>true</code> enables the gui.
   * @return The statistics generated in the run.
   */
  public static StatisticsDTO performSingleRun(DynamicPDPTWScenario scenario,
      MASConfiguration configuration, ObjectiveFunction objFunc, boolean showGui) {
    final StatisticsDTO stats = init(scenario, configuration, showGui)
        .simulate();
    checkState(objFunc.isValidResult(stats),
        "The simulation did not result in a valid result: %s.", stats);
    return stats;
  }

  /**
   * Initialize a {@link DynamicPDPTWProblem} instance.
   * @param scenario The scenario to use.
   * @param config The configuration to use.
   * @param showGui Whether to show the gui.
   * @return The {@link DynamicPDPTWProblem} instance.
   */
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
      problem.addRendererToUI(new RouteRenderer());
      problem.enableUI();
    }
    return problem;
  }
}
