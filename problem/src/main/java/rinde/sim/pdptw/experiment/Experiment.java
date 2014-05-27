/**
 * 
 */

package rinde.sim.pdptw.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.model.Model;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DynamicPDPTWProblem;
import rinde.sim.pdptw.common.ObjectiveFunction;
import rinde.sim.pdptw.common.RouteRenderer;
import rinde.sim.pdptw.common.StatisticsDTO;
import rinde.sim.pdptw.scenario.PDPScenario;
import rinde.sim.scenario.ScenarioController.UICreator;
import rinde.sim.util.StochasticSupplier;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Utility for defining and performing experiments. An experiment is composed of
 * a set of {@link PDPScenario}s and a set of {@link MASConfiguration} s. For
 * <b>each</b> combination of these a user configurable number of simulations is
 * performed. The number of used threads in the experiment can be set via
 * {@link Builder#withThreads(int)}.
 * <p>
 * <b>Example</b> Consider an experiment with three scenarios and two
 * configurations, and each simulation needs to be repeated twice. The code
 * required for this setup:
 * 
 * <pre>
 * {@code
 * Experiment.experiment(objFunc)
 *    .addConfiguration(config1)
 *    .addConfiguration(config2)
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
   * Can be used to run a single simulation run.
   * @param scenario The scenario to run on.
   * @param configuration The configuration to use.
   * @param seed The seed of the run.
   * @param objFunc The {@link ObjectiveFunction} to use.
   * @param showGui If <code>true</code> enables the gui.
   * @param postProcessor The post processor to use for this run.
   * @param uic The UICreator to use.
   * @return The {@link SimulationResult} generated in the run.
   */
  public static SimulationResult singleRun(PDPScenario scenario,
      MASConfiguration configuration, long seed, ObjectiveFunction objFunc,
      boolean showGui, @Nullable PostProcessor<?> postProcessor,
      @Nullable UICreator uic) {

    final ExperimentRunner er = new ExperimentRunner(scenario, configuration,
        seed, objFunc, showGui, postProcessor, uic);
    final SimulationResult res = er.call();
    checkState(res != null);
    return res;
  }

  /**
   * Initialize a {@link DynamicPDPTWProblem} instance.
   * @param scenario The scenario to use.
   * @param config The configuration to use.
   * @param showGui Whether to show the gui.
   * @return The {@link DynamicPDPTWProblem} instance.
   */
  @VisibleForTesting
  static DynamicPDPTWProblem init(PDPScenario scenario,
      MASConfiguration config, long seed, boolean showGui,
      @Nullable UICreator uic) {

    final RandomGenerator rng = new MersenneTwister(seed);
    final long simSeed = rng.nextLong();

    final ImmutableList<? extends StochasticSupplier<? extends Model<?>>> modelSuppliers = config
        .getModels();
    final Model<?>[] models = new Model<?>[modelSuppliers.size()];
    for (int i = 0; i < modelSuppliers.size(); i++) {
      models[i] = modelSuppliers.get(i).get(rng.nextLong());
    }

    final DynamicPDPTWProblem problem = new DynamicPDPTWProblem(scenario,
        simSeed, models);
    problem.addCreator(AddVehicleEvent.class, config.getVehicleCreator());
    if (config.getDepotCreator().isPresent()) {
      problem.addCreator(AddDepotEvent.class, config.getDepotCreator().get());
    }
    if (config.getParcelCreator().isPresent()) {
      problem.addCreator(AddParcelEvent.class, config.getParcelCreator().get());
    }
    if (showGui) {
      if (uic == null) {
        problem.addRendererToUI(new RouteRenderer());
        problem.enableUI();
      }
      else {
        problem.enableUI(uic);
      }
    }
    return problem;
  }

  /**
   * Builder for configuring experiments.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static final class Builder {
    final ObjectiveFunction objectiveFunction;
    final ImmutableList.Builder<MASConfiguration> configurationsBuilder;
    final ImmutableList.Builder<PDPScenario> scenariosBuilder;
    @Nullable
    UICreator uiCreator;
    @Nullable
    PostProcessor<?> postProc;
    boolean showGui;
    int repetitions;
    long masterSeed;
    private int numThreads;

    Builder(ObjectiveFunction objectiveFunction) {
      this.objectiveFunction = objectiveFunction;
      configurationsBuilder = ImmutableList.builder();
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
     * Enable the GUI using the specified creator for each simulation. When a
     * large number of simulations is performed this may slow down the
     * experiment significantly. The GUI can not be shown when more than one
     * thread is used.
     * @param uic The {@link UICreator} to use for creating the GUI.
     * @return This, as per the builder pattern.
     */
    public Builder showGui(UICreator uic) {
      uiCreator = uic;
      return showGui();
    }

    /**
     * Add a configuration to the experiment. For each simulation
     * {@link StochasticSupplier#get(long)} is called and the resulting
     * {@link MASConfiguration} is used for a <i>single</i> simulation.
     * @param config The configuration to add.
     * @return This, as per the builder pattern.
     */
    public Builder addConfiguration(MASConfiguration config) {
      configurationsBuilder.add(config);
      return this;
    }

    /**
     * Adds all configurations to the experiment. For each simulation
     * {@link StochasticSupplier#get(long)} is called and the resulting
     * {@link MASConfiguration} is used for a <i>single</i> simulation.
     * @param configs The configurations to add.
     * @return This, as per the builder pattern.
     */
    public Builder addConfigurations(List<MASConfiguration> configs) {
      configurationsBuilder.addAll(configs);
      return this;
    }

    /**
     * Add a scenario to the set of scenarios.
     * @param scenario The scenario to add.
     * @return This, as per the builder pattern.
     */
    public Builder addScenario(PDPScenario scenario) {
      scenariosBuilder.add(scenario);
      return this;
    }

    /**
     * Add all scenarios to the set of scenarios.
     * @param scenarios The scenarios to add.
     * @return This, as per the builder pattern.
     */
    public Builder addScenarios(List<? extends PDPScenario> scenarios) {
      scenariosBuilder.addAll(scenarios);
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
     * Specify a {@link PostProcessor} which is used to gather additional
     * results from a simulation. The data gathered by the post-processor ends
     * up in {@link SimulationResult#simulationData}.
     * @param postProcessor The post-processor to use, by default there is no
     *          post-processor.
     * @return This, as per the builder pattern.
     */
    public Builder usePostProcessor(PostProcessor<?> postProcessor) {
      postProc = postProcessor;
      return this;
    }

    /**
     * Perform the experiment. For every scenario every configuration is used
     * <code>n</code> times. Where <code>n</code> is the number of repetitions
     * as specified.
     * @return An {@link ExperimentResults} instance which contains all
     *         experiment parameters and the corresponding results.
     */
    public ExperimentResults perform() {
      checkArgument(numThreads == 1 || !showGui,
          "The GUI can not be shown when using more than one thread.");
      final List<Long> seeds = generateSeeds();

      // run Forrest run!
      final ImmutableList<ExperimentRunner> runners = gatherAllRunners(seeds);
      return runAllRunners(runners);
    }

    private ImmutableList<Long> generateSeeds() {
      if (repetitions > 1) {
        final RandomGenerator rng = new MersenneTwister(masterSeed);
        return generateDistinct(rng, repetitions);
      } else {
        return ImmutableList.of(masterSeed);
      }
    }

    private ImmutableList<ExperimentRunner> gatherAllRunners(List<Long> seeds) {
      final ImmutableList<PDPScenario> scen = scenariosBuilder.build();
      final ImmutableList<MASConfiguration> conf = configurationsBuilder
          .build();

      checkArgument(!scen.isEmpty(), "At least one scenario is required.");
      checkArgument(!conf.isEmpty(), "At least one configuration is required.");
      final ImmutableList.Builder<ExperimentRunner> runnerBuilder = ImmutableList
          .builder();
      for (final MASConfiguration configuration : conf) {
        for (final PDPScenario scenario : scen) {
          for (int i = 0; i < repetitions; i++) {
            final long seed = seeds.get(i);
            runnerBuilder.add(new ExperimentRunner(scenario, configuration,
                seed, objectiveFunction, showGui, postProc, uiCreator));
          }
        }
      }
      return runnerBuilder.build();
    }

    private ExperimentResults runAllRunners(
        ImmutableList<ExperimentRunner> runners) {
      final int threads = Math.min(numThreads, runners.size());
      final ListeningExecutorService executor;
      if (threads > 1) {
        executor = MoreExecutors
            .listeningDecorator(Executors.newFixedThreadPool(threads));
      } else {
        executor = MoreExecutors.sameThreadExecutor();
      }
      final List<SimulationResult> results;
      try {
        // safe cast according to javadoc
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final List<ListenableFuture<SimulationResult>> futures = (List) executor
            .invokeAll(runners);
        results = Futures.allAsList(futures).get();
      } catch (final InterruptedException e) {
        throw new IllegalStateException(e);
      } catch (final ExecutionException e) {
        // FIXME need some way to gracefully handle this error. All data
        // should be saved to reproduce this simulation.
        throw new IllegalStateException(e);
      }
      executor.shutdown();

      return new ExperimentResults(this, ImmutableList.copyOf(results));
    }
  }

  static ImmutableList<Long> generateDistinct(RandomGenerator rng, int size) {
    final Set<Long> numbers = newLinkedHashSet();
    while (numbers.size() < size) {
      numbers.add(rng.nextLong());
    }
    return ImmutableList.copyOf(numbers);
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
    public final PDPScenario scenario;

    /**
     * The configuration which was used to configure the MAS.
     */
    public final MASConfiguration masConfiguration;

    /**
     * The seed that was supplied to {@link StochasticSupplier#get(long)}.
     */
    public final long seed;

    /**
     * Additional simulation data as gathered by a {@link PostProcessor}, or if
     * no post-processor was used this object defaults to <code>null</code>.
     */
    @Nullable
    public Object simulationData;

    SimulationResult(StatisticsDTO stats, PDPScenario scenario,
        MASConfiguration masConfiguration, long seed, @Nullable Object simData) {
      this.stats = stats;
      this.scenario = scenario;
      this.masConfiguration = masConfiguration;
      this.seed = seed;
      simulationData = simData;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj.getClass() != getClass()) {
        return false;
      }
      final SimulationResult other = (SimulationResult) obj;
      return Objects.equal(stats, other.stats)
          && Objects.equal(scenario, other.scenario)
          && Objects.equal(masConfiguration, other.masConfiguration)
          && Objects.equal(seed, other.seed)
          && Objects.equal(simulationData, other.simulationData);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(stats, scenario, masConfiguration, seed,
          simulationData);
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("stats", stats)
          .add("scenario", scenario)
          .add("masConfiguration", masConfiguration)
          .add("seed", seed)
          .add("simulationData", simulationData)
          .toString();
    }
  }

  /**
   * Value object containing all the results of a single experiment as performed
   * by {@link Builder#perform()}.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static final class ExperimentResults {
    /**
     * The {@link ObjectiveFunction} that was used for this experiment.
     */
    public final ObjectiveFunction objectiveFunction;

    /**
     * The configurations that were used in this experiment.
     */
    public final ImmutableList<MASConfiguration> configurations;

    /**
     * The scenarios that were used in this experiment.
     */
    public final ImmutableList<PDPScenario> scenarios;

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
      configurations = exp.configurationsBuilder.build();
      scenarios = exp.scenariosBuilder.build();
      showGui = exp.showGui;
      repetitions = exp.repetitions;
      masterSeed = exp.masterSeed;
      results = res;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(objectiveFunction, configurations, scenarios,
          showGui, repetitions, masterSeed, results);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (other == null) {
        return false;
      }
      if (other.getClass() != getClass()) {
        return false;
      }
      final ExperimentResults er = (ExperimentResults) other;
      return Objects.equal(objectiveFunction, er.objectiveFunction)
          && Objects.equal(configurations, er.configurations)
          && Objects.equal(scenarios, er.scenarios)
          && Objects.equal(showGui, er.showGui)
          && Objects.equal(repetitions, er.repetitions)
          && Objects.equal(masterSeed, er.masterSeed)
          && Objects.equal(results, er.results);
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("objectiveFunction", objectiveFunction)
          .add("configurations", configurations)
          .add("scenarios", scenarios)
          .add("showGui", showGui)
          .add("repetitions", repetitions)
          .add("masterSeed", masterSeed)
          .add("results", results)
          .toString();
    }
  }

  private static class ExperimentRunner implements Callable<SimulationResult> {
    private final PDPScenario scenario;
    private final MASConfiguration configuration;
    private final long seed;
    private final ObjectiveFunction objectiveFunction;
    private final boolean showGui;
    @Nullable
    private final UICreator uiCreator;
    @Nullable
    private final PostProcessor<?> postProcessor;

    ExperimentRunner(PDPScenario scenario,
        MASConfiguration configuration, long seed,
        ObjectiveFunction objectiveFunction, boolean showGui,
        @Nullable PostProcessor<?> postProc,
        @Nullable UICreator uic) {
      this.scenario = scenario;
      this.configuration = configuration;
      this.seed = seed;
      this.objectiveFunction = objectiveFunction;
      this.showGui = showGui;
      postProcessor = postProc;
      uiCreator = uic;
    }

    @Override
    public SimulationResult call() {
      try {
        final DynamicPDPTWProblem prob = init(scenario, configuration, seed,
            showGui, uiCreator);
        final StatisticsDTO stats = prob.simulate();

        @Nullable
        Object data = null;
        if (postProcessor != null) {
          data = postProcessor.collectResults(prob.getSimulator());
        }
        checkState(objectiveFunction.isValidResult(stats),
            "The simulation did not result in a valid result: %s.", stats);
        final SimulationResult result = new SimulationResult(stats, scenario,
            configuration, seed, data);

        // FIXME this should be changed into a more decent progress indicator
        System.out.print(".");
        return result;
      } catch (final RuntimeException e) {
        final StringBuilder sb = new StringBuilder().append("[Scenario= ")
            .append(scenario).append(",").append(scenario.getProblemClass())
            .append(",").append(scenario.getProblemInstanceId()).append("]")
            .append(",seed=").append(seed).append(",config=")
            .append(configuration);
        throw new RuntimeException(sb.toString(), e);
      }

    }
  }
}
