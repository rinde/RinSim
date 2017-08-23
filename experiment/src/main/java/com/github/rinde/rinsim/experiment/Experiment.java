/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.experiment.LocalComputer.ExperimentRunner;
import com.github.rinde.rinsim.experiment.PostProcessor.FailureStrategy;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.pdptw.common.StatisticsProvider;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioController;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
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
 * Experiment.builder()
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
  static final Logger LOGGER = LoggerFactory.getLogger(Experiment.class);
  static final String DASH = "-";

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
   * Start building a new experiment.
   * @return A new {@link Builder} instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Can be used to run a single simulation run.
   * @param scenario The scenario to run on.
   * @param configuration The configuration to use.
   * @param seed The seed of the run.
   * @param showGui If <code>true</code> enables the gui.
   * @param postProcessor The post processor to use for this run.
   * @param uic The UICreator to use.
   * @return The {@link SimulationResult} generated in the run.
   */
  public static SimulationResult singleRun(Scenario scenario,
      MASConfiguration configuration, long seed, boolean showGui,
      PostProcessor<?> postProcessor, @Nullable ModelBuilder<?, ?> uic) {

    final ExperimentRunner er = new ExperimentRunner(SimArgs.create(scenario,
      configuration, seed, 0, showGui, postProcessor, uic));
    final SimulationResult res = er.call();
    checkState(res != null);
    return res;
  }

  /**
   * Initialize a {@link Simulator} instance.
   * @param scenario The scenario to use.
   * @param config The configuration to use.
   * @param showGui Whether to show the gui.
   * @return The {@link Simulator} instance.
   */
  @VisibleForTesting
  static Simulator init(Scenario scenario, MASConfiguration config, long seed,
      boolean showGui, Optional<ModelBuilder<?, ?>> uiCreator) {

    final ScenarioController.Builder scenContrBuilder =
      ScenarioController.builder(
        scenario)
        .withIgnoreRedundantHandlers(true)
        .withEventHandlers(config.getEventHandlers());

    final Simulator.Builder simBuilder = Simulator.builder()
      .setRandomSeed(seed)
      .addModel(scenContrBuilder)
      .addModels(config.getModels());

    final boolean hasStatsTracker =
      containsStatisticsProvider(scenContrBuilder.getChildren())
        || containsStatisticsProvider(config.getModels());

    if (!hasStatsTracker) {
      simBuilder.addModel(StatsTracker.builder());
    }

    if (showGui) {
      checkState(uiCreator.isPresent(), "No UI was specified.");
      simBuilder.addModel(uiCreator.get());
    }

    return simBuilder.build();
  }

  static Object perform(SimArgs args) {
    final Simulator sim = Experiment.init(args.getScenario(),
      args.getMasConfig(), args.getRandomSeed(), args.isShowGui(),
      args.getUiCreator());

    try {
      sim.start();
      final Object resultObject =
        args.getPostProcessor().collectResults(sim, args);
      checkNotNull(resultObject, "PostProcessor may not return null.");
      return resultObject;

    } catch (final Exception e) {
      final FailureStrategy strategy =
        args.getPostProcessor().handleFailure(e, sim, args);
      checkNotNull(strategy,
        "An exception (%s) occured in the simulation but the PostProcessor %s "
          + "failed to handle the failure.",
        e.toString(), args.getPostProcessor());

      if (strategy == FailureStrategy.INCLUDE) {
        return args.getPostProcessor().collectResults(sim, args);
      } else if (strategy == FailureStrategy.ABORT_EXPERIMENT_RUN) {
        throw new AbortExperimentException("Failed: " + args, e);
      }
      return strategy;
    }
  }

  static boolean containsStatisticsProvider(
      Iterable<? extends ModelBuilder<?, ?>> mbs) {
    for (final ModelBuilder<?, ?> mb : mbs) {
      if (mb.getProvidingTypes().contains(StatisticsProvider.class)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Builder for configuring experiments.
   * @author Rinde van Lon
   */
  public static final class Builder {
    static final List<SimulationProperty> DEFAULT_EXPERIMENT_ORDERING =
      ImmutableList.of(
        SimulationProperty.CONFIG,
        SimulationProperty.SCENARIO,
        SimulationProperty.REPS,
        SimulationProperty.SEED_REPS);

    final Set<MASConfiguration> configurationsSet;
    final ImmutableSet.Builder<Scenario> scenariosBuilder;
    Optional<FileProvider.Builder> scenarioProviderBuilder;
    Function<Path, ? extends Scenario> fileReader;
    List<SimulationProperty> experimentOrdering;

    final List<ResultListener> resultListeners;
    @Nullable
    ModelBuilder<?, ?> uiCreator;
    PostProcessor<?> postProc;
    boolean showGui;
    int repetitions;
    int seedRepetitions;
    long masterSeed;
    int numThreads;
    int numBatches;
    int compositeTaskSize;
    long warmupPeriodMs;

    private Supplier<Computer> computerType;

    Builder() {
      configurationsSet = newLinkedHashSet();
      scenariosBuilder = ImmutableSet.builder();
      scenarioProviderBuilder = Optional.absent();
      fileReader = ScenarioIO.reader();
      resultListeners = newArrayList();
      showGui = false;
      repetitions = 1;
      seedRepetitions = 1;
      masterSeed = 0L;
      numThreads = Runtime.getRuntime().availableProcessors();
      numBatches = 1;
      compositeTaskSize = 1;
      computerType = Computers.LOCAL;
      postProc = PostProcessors.defaultPostProcessor();
      experimentOrdering = DEFAULT_EXPERIMENT_ORDERING;
    }

    /**
     * Set the number of repetitions for each simulation.
     * @param times The number of repetitions.
     * @return This, as per the builder pattern.
     */
    public Builder repeat(int times) {
      checkArgument(times > 0,
        "The number of repetitions must be strictly positive, was %s.",
        times);
      repetitions = times;
      return this;
    }

    /**
     * Sets the number of repetitions for each combination of
     * {@link MASConfiguration}, {@link Scenario} and <code>seed</code>. This
     * allows to run exactly the same simulation twice, generally there are two
     * use cases for doing this:
     * <ol>
     * <li>To test whether the simulation is deterministic, each simulation run
     * with the same input should give exactly the same output.</li>
     * <li>If the simulation is known to be non-deterministic (e.g. because of
     * hardware dependence), the influence of the non-determinism on the results
     * can be evaluated.</li>
     * </ol>
     * @param times The number of times to repeat simulations with the same
     *          {@link MASConfiguration}, {@link Scenario} and <code>seed</code>
     *          .
     * @return This, as per the builder pattern.
     */
    public Builder repeatSeed(int times) {
      checkArgument(times > 0,
        "The number of seed repetitions must be strictly positive, was %s.",
        times);
      seedRepetitions = times;
      return this;
    }

    /**
     * Enable or disable the GUI for each simulation. When a large number of
     * simulations is performed this may slow down the experiment significantly.
     * The GUI can not be shown when more than one thread is used.
     * @param show Show the GUI for each simulation if <code>true</code>, or
     *          hide it if <code>false</code>.
     * @return This, as per the builder pattern.
     */
    public Builder showGui(boolean show) {
      showGui = show;
      return this;
    }

    /**
     * Enable the GUI using the specified creator for each simulation. When a
     * large number of simulations is performed this may slow down the
     * experiment significantly. The GUI can not be shown when more than one
     * thread is used.
     * @param uic The {@link ModelBuilder} to use for creating the GUI.
     * @return This, as per the builder pattern.
     */
    public Builder showGui(ModelBuilder<?, ?> uic) {
      uiCreator = uic;
      return showGui(true);
    }

    /**
     * Specifies the ordering of simulations. The resulting order of simulations
     * is the order in which the simulations are offered to the executor. It
     * depends on the executor's behavior whether this ordering is respected. In
     * case the experiment is executed locally, the execution order <i>will</i>
     * be respected, in a distributed setting this may not always be the case.
     * <p>
     * For example, consider the following settings:
     * <ul>
     * <li>{@link #repeat(int)} is set to 3.</li>
     * <li>{@link #repeatSeed(int)} is set to 2.</li>
     * <li>The ordering starts with <code>[{@link SimulationProperty#SEED_REPS},
     *  {@link SimulationProperty#REPS}, ..]</code></li>
     * </ul>
     * The result will be that simulations first ordered by their seed creation
     * order, and second by their repetition number:
     * <ol>
     * <li>seed0, rep0, ..</li>
     * <li>seed0, rep1, ..</li>
     * <li>seed0, rep2, ..</li>
     * <li>seed1, rep0, ..</li>
     * <li>seed1, rep1, ..</li>
     * <li>seed1, rep2, ..</li>
     * </ol>
     * The default order is {@link SimulationProperty#CONFIG},
     * {@link SimulationProperty#SCENARIO}, {@link SimulationProperty#REPS},
     * {@link SimulationProperty#SEED_REPS}.
     *
     * @param experimentOrder The order of simulation creation, all
     *          {@link SimulationProperty}s must be specified.
     * @return This, as per the builder pattern.
     */
    public Builder withOrdering(SimulationProperty... experimentOrder) {
      return withOrdering(ImmutableList.copyOf(checkNotNull(experimentOrder)));
    }

    /**
     * Specifies the ordering of simulations. The resulting order of simulations
     * is the order in which the simulations are offered to the executor. It
     * depends on the executor's behavior whether this ordering is respected. In
     * case the experiment is executed locally, the execution order <i>will</i>
     * be respected, in a distributed setting this may not always be the case.
     * <p>
     * For example, consider the following settings:
     * <ul>
     * <li>{@link #repeat(int)} is set to 3.</li>
     * <li>{@link #repeatSeed(int)} is set to 2.</li>
     * <li>The ordering starts with <code>[{@link SimulationProperty#SEED_REPS},
     *  {@link SimulationProperty#REPS}, ..]</code></li>
     * </ul>
     * The result will be that simulations first ordered by their seed creation
     * order, and second by their repetition number:
     * <ol>
     * <li>seed0, rep0, ..</li>
     * <li>seed0, rep1, ..</li>
     * <li>seed0, rep2, ..</li>
     * <li>seed1, rep0, ..</li>
     * <li>seed1, rep1, ..</li>
     * <li>seed1, rep2, ..</li>
     * </ol>
     * The default order is {@link SimulationProperty#CONFIG},
     * {@link SimulationProperty#SCENARIO}, {@link SimulationProperty#REPS},
     * {@link SimulationProperty#SEED_REPS}.
     *
     * @param experimentOrder The order of simulation creation, all
     *          {@link SimulationProperty}s must be specified.
     * @return This, as per the builder pattern.
     */
    public Builder withOrdering(Iterable<SimulationProperty> experimentOrder) {
      checkArgument(
        Iterables.size(experimentOrder) == SimulationProperty.values().length,
        "Each experiment ordering should be specified exactly once, is: %s.",
        experimentOrder);
      experimentOrdering = ImmutableList.copyOf(experimentOrder);
      return this;
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
    public Builder addScenarios(Iterable<? extends Scenario> scenarios) {
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
     * default equals {@link Runtime#availableProcessors()}.
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
     * Set the warmup period for the experiments. This is the number of
     * milliseconds that the experiment will be run until it will be
     * interrupted. After the warmup period the experiment will start again
     * without being interrupted.
     * @param warmupPeriodMillis The length of the warmup period. Must be
     *          positive, if <code>0</code> no warmup will be performed.
     * @return This, as per the builder pattern.
     */
    public Builder withWarmup(long warmupPeriodMillis) {
      checkArgument(warmupPeriodMillis >= 0,
        "Warmup period must be positive, was %s.", warmupPeriodMillis);
      warmupPeriodMs = warmupPeriodMillis;
      return this;
    }

    /**
     * Specify a {@link PostProcessor} which is used to create a results object
     * and perform error handling. The data gathered by the post-processor ends
     * up in {@link SimulationResult#getResultObject()}.
     * @param postProcessor The post-processor to use, by default
     *          {@link PostProcessors#defaultPostProcessor()} is used.
     * @return This, as per the builder pattern.
     */
    public Builder usePostProcessor(PostProcessor<?> postProcessor) {
      postProc = postProcessor;
      return this;
    }

    /**
     * Sets the number of batches that should be used when using the
     * {@link #computeDistributed()} setting. This method only has an effect in
     * case the computations are distributed, see {@link #computeDistributed()}.
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
     * Sets the number of simulations that should be bundles into one task. All
     * simulations that are bundled are executed sequentially. Default is
     * <code>1</code>. This method only has an effect in case the computations
     * are distributed, see {@link #computeDistributed()}.
     * @param size The composite task size to use.
     * @return This, as per the builder pattern.
     */
    public Builder setCompositeTaskSize(int size) {
      checkArgument(size > 0,
        "The composite task size must be strictly positive, was %s.", size);
      compositeTaskSize = size;
      return this;
    }

    /**
     * When this method is called the experiment will be performed in a
     * distributed fashion using the <a href="http://www.jppf.org/">JPPF</a>
     * framework. By default JPPF will attempt to connect to a driver on
     * <code>localhost</code>. For changing the JPPF settings, please consult
     * the <a href="http://www.jppf.org/doc/">JPPF documentation</a>.
     * <p>
     * <b>Requirements:</b> {@link MASConfiguration} and {@link PostProcessor}
     * (if used) must implement {@link java.io.Serializable}.
     * <p>
     * <b>Incompatible settings</b><br>
     * The following settings will be ignored when computing is done in a
     * distributed fashion:
     * <ul>
     * <li>{@link #withThreads(int)}</li>
     * <li>{@link #showGui(boolean)}</li>
     * <li>{@link #showGui(ModelBuilder)}</li>
     * </ul>
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
     * listener will be called each time a simulation is done.
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

      final ImmutableSet<Scenario> scenarios = getAllScenarios();
      final ImmutableSet<SimArgs> runners =
        createFactorialSetup(seeds, scenarios);

      if (warmupPeriodMs > 0) {
        checkArgument(computerType == Computers.LOCAL,
          "Warmup can only be used when experiment is performed locally.");
        checkArgument(!showGui,
          "Gui can not be shown in combination with a warmup period.");
        LOGGER.info("Start warmup.");

        new WarmupComputer(computerType.get()).compute(this, runners);
        LOGGER.info("Warmup finished. {}");
      }

      for (final ResultListener rl : resultListeners) {
        rl.startComputing(runners.size(),
          ImmutableSet.copyOf(configurationsSet),
          scenarios,
          repetitions,
          seedRepetitions);

      }
      // run Forrest run!
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
    public Optional<ExperimentResults> perform(PrintStream out,
        String... args) {
      LOGGER.trace("perform {}", Arrays.toString(args));
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

    private ImmutableSet<SimArgs> createFactorialSetup(List<Long> seeds,
        ImmutableSet<Scenario> scenarios) {
      final ImmutableSet<MASConfiguration> conf =
        ImmutableSet.copyOf(configurationsSet);

      final ContiguousSet<Integer> seedReps = ContiguousSet.create(
        Range.closedOpen(0, seedRepetitions), DiscreteDomain.integers());
      final ImmutableSet<Long> seedSet = ImmutableSet.copyOf(seeds);

      checkArgument(!scenarios.isEmpty(), "At least one scenario is required.");
      checkArgument(!conf.isEmpty(), "At least one configuration is required.");
      final ImmutableSet.Builder<SimArgs> runnerBuilder =
        ImmutableSet.builder();

      final List<Set<? extends Object>> input = new ArrayList<>();
      for (final SimulationProperty eo : experimentOrdering) {
        input.add(eo.select(configurationsSet, scenarios, seedSet, seedReps));
      }

      final Set<List<Object>> product = Sets.cartesianProduct(input);

      for (final List<Object> args : product) {
        Scenario s = null;
        MASConfiguration c = null;
        Optional<Long> seed = Optional.absent();
        Optional<Integer> rep = Optional.absent();
        for (final Object arg : args) {
          if (arg instanceof Scenario) {
            s = (Scenario) arg;
          } else if (arg instanceof MASConfiguration) {
            c = (MASConfiguration) arg;
          } else if (arg instanceof Long) {
            seed = Optional.of((Long) arg);
          } else if (arg instanceof Integer) {
            rep = Optional.of((Integer) arg);
          }
        }

        runnerBuilder.add(SimArgs.create(verifyNotNull(s), verifyNotNull(c),
          seed.get(), rep.get(), showGui, postProc,
          uiCreator));
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

  /**
   * Simulation arguments of a single simulation.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class SimArgs {

    SimArgs() {}

    /**
     * @return the scenario
     */
    public abstract Scenario getScenario();

    /**
     * @return the masConfig
     */
    public abstract MASConfiguration getMasConfig();

    /**
     * @return the randomSeed
     */
    public abstract long getRandomSeed();

    /**
     * @return The seed repetition number.
     */
    public abstract int getRepetition();

    /**
     * @return the showGui
     */
    public abstract boolean isShowGui();

    /**
     * @return the postProcessor
     */
    public abstract PostProcessor<?> getPostProcessor();

    /**
     * @return the uiCreator
     */
    public abstract Optional<ModelBuilder<?, ?>> getUiCreator();

    @Override
    public String toString() {
      return new StringBuilder()
        .append("SimArgs{problemClass=")
        .append(getScenario().getProblemClass().toString())
        .append(",instancedId=")
        .append(getScenario().getProblemInstanceId())
        .append(",masConfig=")
        .append(getMasConfig().getName())
        .append(",randomSeed=")
        .append(getRandomSeed())
        .append(",repetition=")
        .append(getRepetition())
        .append(",postProcessor=")
        .append(getPostProcessor())
        .append("}")
        .toString();
    }

    /**
     * @return A very compact string representation.
     */
    public String toShortString() {
      return new StringBuilder(getScenario().getProblemClass().getId())
        .append(DASH)
        .append(getScenario().getProblemInstanceId())
        .append(DASH)
        .append(getMasConfig().getName())
        .append("-s")
        .append(getRandomSeed())
        .append(DASH)
        .append("r")
        .append(getRepetition())
        .toString();
    }

    static SimArgs create(Scenario s, MASConfiguration m, long seed,
        int repetition, boolean gui, PostProcessor<?> pp,
        @Nullable ModelBuilder<?, ?> uic) {
      return new AutoValue_Experiment_SimArgs(s, m, seed, repetition, gui, pp,
        Optional.<ModelBuilder<?, ?>>fromNullable(uic));
    }

    static Function<SimArgs, MASConfiguration> toConfig() {
      return ToConfig.INSTANCE;
    }

    static Function<SimArgs, Scenario> toScenario() {
      return ToScenario.INSTANCE;
    }

    static Function<SimArgs, Long> toSeed() {
      return ToSeed.INSTANCE;
    }

    static Function<SimArgs, Integer> toRepeat() {
      return ToRepeat.INSTANCE;
    }

    private enum ToConfig implements Function<SimArgs, MASConfiguration> {
      INSTANCE {
        @Nullable
        @Override
        public MASConfiguration apply(@Nullable SimArgs input) {
          return verifyNotNull(input).getMasConfig();
        }
      }
    }

    private enum ToScenario implements Function<SimArgs, Scenario> {
      INSTANCE {
        @Nullable
        @Override
        public Scenario apply(@Nullable SimArgs input) {
          return verifyNotNull(input).getScenario();
        }
      }
    }

    private enum ToSeed implements Function<SimArgs, Long> {
      INSTANCE {
        @Nullable
        @Override
        public Long apply(@Nullable SimArgs input) {
          return verifyNotNull(input).getRandomSeed();
        }
      }
    }

    private enum ToRepeat implements Function<SimArgs, Integer> {
      INSTANCE {
        @Nullable
        @Override
        public Integer apply(@Nullable SimArgs input) {
          return verifyNotNull(input).getRepetition();
        }
      }
    }
  }

  /**
   * The result of a single simulation. It contains both the resulting
   * statistics as well as the inputs used to obtain this result.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class SimulationResult
      implements Comparable<SimulationResult> {

    SimulationResult() {}

    /**
     * @return The arguments of the simulation.
     */
    public abstract SimArgs getSimArgs();

    /**
     * @return A result object is created by a {@link PostProcessor}.
     */
    public abstract Object getResultObject();

    @Override
    public int compareTo(@Nullable SimulationResult o) {
      assert o != null;
      return ComparisonChain.start()
        .compare(getSimArgs().getScenario().getProblemClass().getId(),
          o.getSimArgs().getScenario().getProblemClass().getId())
        .compare(getSimArgs().getScenario().getProblemInstanceId(),
          o.getSimArgs().getScenario().getProblemInstanceId())
        .result();
    }

    static SimulationResult create(SimArgs simArgs, Object simResult) {
      return new AutoValue_Experiment_SimulationResult(simArgs, simResult);
    }
  }
}
