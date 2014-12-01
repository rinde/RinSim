/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rinde.rinsim.pdptw.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.RouteRenderer;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.experiment.LocalComputer.ExperimentRunner;
import com.github.rinde.rinsim.scenario.AddDepotEvent;
import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioController.UICreator;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Utility for defining and performing experiments. An experiment is composed of
 * a set of {@link Scenario}s and a set of {@link MASConfiguration} s. For
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
 * @author Rinde van Lon
 */
public final class Experiment {
  // TODO add strict mode which checks whether there are not too many
  // vehicles/parcels/depots?

  enum Computers implements Supplier<Computer> {
    LOCAL {
      @Override
      public Computer get() {
        return new LocalComputer();
      }
    },
    DISTRIBUTED {
      @Override
      public Computer get() {
        return new JppfComputer();
      }
    };
  }

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
  public static SimulationResult singleRun(Scenario scenario,
      MASConfiguration configuration, long seed, ObjectiveFunction objFunc,
      boolean showGui, @Nullable PostProcessor<?> postProcessor,
      @Nullable UICreator uic) {

    final ExperimentRunner er = new ExperimentRunner(new SimArgs(scenario,
        configuration, seed, objFunc, showGui, postProcessor, uic));
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
  static DynamicPDPTWProblem init(Scenario scenario,
      MASConfiguration config, long seed, boolean showGui,
      Optional<UICreator> uiCreator) {

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
      if (uiCreator.isPresent()) {
        problem.enableUI(uiCreator.get());
      }
      else {
        problem.addRendererToUI(new RouteRenderer());
        problem.enableUI();
      }
    }
    return problem;
  }

  /**
   * Builder for configuring experiments.
   * @author Rinde van Lon
   */
  public static final class Builder {
    final ObjectiveFunction objectiveFunction;
    final Set<MASConfiguration> configurationsSet;
    final ImmutableSet.Builder<Scenario> scenariosBuilder;
    Optional<FileProvider.Builder> scenarioProviderBuilder;
    Function<Path, ? extends Scenario> fileReader;

    final List<ResultListener> resultListeners;
    @Nullable
    UICreator uiCreator;
    @Nullable
    PostProcessor<?> postProc;
    boolean showGui;
    int repetitions;
    long masterSeed;
    int numThreads;
    int numBatches;

    private Supplier<Computer> computerType;

    Builder(ObjectiveFunction objectiveFunction) {
      this.objectiveFunction = objectiveFunction;
      configurationsSet = newLinkedHashSet();
      scenariosBuilder = ImmutableSet.builder();
      scenarioProviderBuilder = Optional.absent();
      fileReader = ScenarioIO.reader();
      resultListeners = newArrayList();
      showGui = false;
      repetitions = 1;
      masterSeed = 0L;
      numThreads = Runtime.getRuntime().availableProcessors();
      numBatches = 1;
      computerType = Computers.LOCAL;
    }

    /**
     * Set the number of repetitions for each simulation.
     * @param times The number of repetitions.
     * @return This, as per the builder pattern.
     */
    public Builder repeat(int times) {
      checkArgument(times > 0,
          "The number of repetitions must be strictly positive, was %s.", times);
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
      checkArgument(!configurationsSet.contains(config));
      configurationsSet.add(config);
      return this;
    }

    /**
     * Adds all configurations to the experiment. For each simulation
     * {@link StochasticSupplier#get(long)} is called and the resulting
     * {@link MASConfiguration} is used for a <i>single</i> simulation.
     * @param configs The configurations to add.
     * @return This, as per the builder pattern.
     */
    public Builder addConfigurations(Iterable<MASConfiguration> configs) {
      final Set<MASConfiguration> newConfigs = ImmutableSet.copyOf(configs);
      checkArgument(Sets.intersection(configurationsSet, newConfigs)
          .isEmpty());
      configurationsSet.addAll(newConfigs);
      return this;
    }

    /**
     * Add a scenario to the set of scenarios.
     * @param scenario The scenario to add.
     * @return This, as per the builder pattern.
     */
    public Builder addScenario(Scenario scenario) {
      scenariosBuilder.add(scenario);
      return this;
    }

    /**
     * Add all scenarios to the set of scenarios.
     * @param scenarios The scenarios to add.
     * @return This, as per the builder pattern.
     */
    public Builder addScenarios(List<? extends Scenario> scenarios) {
      scenariosBuilder.addAll(scenarios);
      return this;
    }

    /**
     * Adds a {@link com.github.rinde.rinsim.io.FileProvider.Builder} to the
     * experiment.
     * @param providerBuilder This builder will be used create a
     *          {@link FileProvider} instance to load scenarios.
     * @return This, as per the builder pattern.
     * @see #setScenarioReader(Function)
     */
    public Builder addScenarios(FileProvider.Builder providerBuilder) {
      scenarioProviderBuilder = Optional.of(providerBuilder);
      return this;
    }

    /**
     * Change the scenario reader which defines how {@link Path} instances are
     * converted to {@link Scenario} instances. By default
     * {@link ScenarioIO#reader()} is used as a scenario reader.
     * @param reader The reader to use.
     * @return This, as per the builder pattern.
     */
    public Builder setScenarioReader(
        Function<Path, ? extends Scenario> reader) {
      fileReader = reader;
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
     * Sets the number of batches that should be used when using the
     * {@link #computeDistributed()} setting.
     * @param num The number of batches to use.
     * @return This, as per the builder pattern.
     */
    public Builder numBatches(int num) {
      checkArgument(num > 0,
          "The number of batches must be strictly positive, was %s.", num);
      numBatches = num;
      return this;
    }

    /**
     * When this method is called the experiment will be performed in a
     * distributed fashion using the <a href="http://www.jppf.org/">JPPF</a>
     * framework. By default JPPF will attempt to connect to a driver on
     * <code>localhost</code>. For changing the JPPF settings, please consult
     * the <a href="http://www.jppf.org/doc/">JPPF documentation</a>.
     * <p>
     * <b>Requirements:</b> {@link ObjectiveFunction}, {@link MASConfiguration}
     * and {@link PostProcessor} (if used) must implement
     * {@link java.io.Serializable}.
     * <p>
     * <b>Incompatible settings</b><br>
     * The following settings will be ignored when computing is done in a
     * distributed fashion:
     * <ul>
     * <li>{@link #withThreads(int)}</li>
     * <li>{@link #showGui()}</li>
     * <li>{@link #showGui(UICreator)}</li>
     * </ul>
     * 
     * @return This, as per the builder pattern.
     */
    public Builder computeDistributed() {
      computerType = Computers.DISTRIBUTED;
      return this;
    }

    /**
     * This setting will perform the experiment locally (this is the default).
     * @return This, as per the builder pattern.
     */
    public Builder computeLocal() {
      computerType = Computers.LOCAL;
      return this;
    }

    /**
     * This setting will perform a 'dry-run' experiment. No computations will be
     * done. Note that this must be called <i>after</i> any calls to
     * {@link #computeDistributed()} or {@link #computeLocal()}, otherwise it
     * has no effect.
     * @param verbose If <code>true</code> additional information is printed.
     * @param stream The stream to write to.
     * @param error The error stream to write to.
     * @return This, as per the builder pattern.
     */
    public Builder dryRun(final boolean verbose, final PrintStream stream,
        final PrintStream error) {
      final Supplier<Computer> originalComputerType = computerType;
      computerType = new Supplier<Computer>() {
        @Override
        public Computer get() {
          return new DryRunComputer(originalComputerType, verbose, stream,
              error);
        }
      };
      return this;
    }

    /**
     * Adds the specified {@link ResultListener} to the experiment. This
     * listener will be called each time a simulation is done. <b>Currently only
     * works for distributed computation</b>.
     * @param listener The listener to add.
     * @return This, as per the builder pattern.
     */
    public Builder addResultListener(ResultListener listener) {
      resultListeners.add(listener);
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
      final ImmutableSet<SimArgs> runners = createFactorialSetup(seeds);
      return computerType.get().compute(this, runners);
    }

    /**
     * Parses the command line arguments. Performs the experiment using
     * {@link #perform()} if the arguments allow it.
     * @param out The print stream to write the feedback from the cli system to.
     * @param args The arguments to parse.
     * @return {@link Optional} containing {@link ExperimentResults} if the
     *         experiment was performed, {@link Optional#absent()} otherwise.
     */
    public Optional<ExperimentResults> perform(PrintStream out, String... args) {
      final Optional<String> error = ExperimentCli.safeExecute(this, args);
      if (error.isPresent()) {
        out.println(error.get());
        return Optional.absent();
      }
      return Optional.of(perform());
    }

    private ImmutableList<Long> generateSeeds() {
      if (repetitions > 1) {
        final RandomGenerator rng = new MersenneTwister(masterSeed);
        return generateDistinct(rng, repetitions);
      }
      return ImmutableList.of(masterSeed);
    }

    Supplier<Computer> getComputer() {
      return computerType;
    }

    ImmutableSet<Scenario> getAllScenarios() {
      final Set<Scenario> scenarios = newLinkedHashSet(scenariosBuilder
          .build());
      if (scenarioProviderBuilder.isPresent()) {
        scenarios.addAll(scenarioProviderBuilder.get().build(fileReader).get());
      }
      return ImmutableSet.copyOf(scenarios);
    }

    int getNumScenarios() {
      final Set<Scenario> scenarios = scenariosBuilder.build();
      if (scenarioProviderBuilder.isPresent()) {
        return scenarios.size()
            + scenarioProviderBuilder.get().build().get().size();
      }
      return scenarios.size();
    }

    private ImmutableSet<SimArgs> createFactorialSetup(List<Long> seeds) {
      final Set<Scenario> scenarios = getAllScenarios();

      final ImmutableSet<MASConfiguration> conf = ImmutableSet
          .copyOf(configurationsSet);

      checkArgument(!scenarios.isEmpty(), "At least one scenario is required.");
      checkArgument(!conf.isEmpty(), "At least one configuration is required.");
      final ImmutableSet.Builder<SimArgs> runnerBuilder = ImmutableSet
          .builder();
      for (final MASConfiguration configuration : conf) {
        for (final Scenario scenario : scenarios) {
          for (int i = 0; i < repetitions; i++) {
            final long seed = seeds.get(i);
            runnerBuilder.add(new SimArgs(scenario, configuration,
                seed, objectiveFunction, showGui, postProc, uiCreator));
          }
        }
      }
      return runnerBuilder.build();
    }

    static ImmutableList<Long> generateDistinct(RandomGenerator rng, int size) {
      final Set<Long> numbers = newLinkedHashSet();
      while (numbers.size() < size) {
        numbers.add(rng.nextLong());
      }
      return ImmutableList.copyOf(numbers);
    }
  }

  static class SimArgs {
    final Scenario scenario;
    final MASConfiguration masConfig;
    final long randomSeed;
    final ObjectiveFunction objectiveFunction;
    final boolean showGui;
    final Optional<? extends PostProcessor<?>> postProcessor;
    final Optional<UICreator> uiCreator;

    SimArgs(Scenario s, MASConfiguration m, long seed,
        ObjectiveFunction obj, boolean gui, @Nullable PostProcessor<?> pp,
        @Nullable UICreator uic) {
      scenario = s;
      masConfig = m;
      randomSeed = seed;
      objectiveFunction = obj;
      showGui = gui;
      postProcessor = Optional.fromNullable(pp);
      uiCreator = Optional.fromNullable(uic);
    }

    @Override
    public String toString() {
      return Joiner.on(",").join(scenario.getClass().getName(),
          scenario.getProblemClass(),
          scenario.getProblemInstanceId(), masConfig, randomSeed,
          objectiveFunction, showGui, postProcessor, uiCreator);
    }
  }

  /**
   * The result of a single simulation. It contains both the resulting
   * statistics as well as the inputs used to obtain this result.
   * @author Rinde van Lon
   */
  public static final class SimulationResult implements
      Comparable<SimulationResult> {
    /**
     * The simulation statistics.
     */
    public final StatisticsDTO stats;

    /**
     * The scenario on which the simulation was run.
     */
    public final Scenario scenario;

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
     * no post-processor was used this object defaults to
     * <code>Optional#absent()</code>.
     */
    public Optional<?> simulationData;

    SimulationResult(StatisticsDTO stats, Scenario scenario,
        MASConfiguration masConfiguration, long seed, Optional<?> simData) {
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
      return MoreObjects.toStringHelper(this)
          .add("stats", stats)
          .add("scenario", scenario)
          .add("masConfiguration", masConfiguration)
          .add("seed", seed)
          .add("simulationData", simulationData)
          .toString();
    }

    @Override
    public int compareTo(SimulationResult o) {
      return ComparisonChain
          .start()
          .compare(scenario.getProblemClass().getId(),
              o.scenario.getProblemClass().getId())
          .compare(scenario.getProblemInstanceId(),
              o.scenario.getProblemInstanceId())
          .result();
    }
  }
}
