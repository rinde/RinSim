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
package com.github.rinde.rinsim.experiment.base;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Objects.requireNonNull;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.io.FileProvider;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public abstract class ExperimentBuilder<T extends ExperimentBuilder<T>> {
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
        throw new UnsupportedOperationException();
        // return new JppfComputer();
      }
    };
  }

  final Set<Configuration> configurationsSet;
  final ImmutableSet.Builder<Scenario> scenariosBuilder;
  Optional<FileProvider.Builder> scenarioProviderBuilder;
  Optional<? extends Function<Path, ? extends Scenario>> fileReader;

  final List<ResultListener> resultListeners;
  // @Nullable
  // UICreator uiCreator;
  @Nullable
  PostProcessor<?, ?> postProc;
  boolean showGui;
  int repetitions;
  long masterSeed;
  int numThreads;
  int numBatches;

  private Supplier<Computer> computerType;

  Function<SimArgs, SimResult> computeFunction;

  protected ExperimentBuilder(Function<SimArgs, SimResult> computeFunc) {
    computeFunction = computeFunc;
    configurationsSet = newLinkedHashSet();
    scenariosBuilder = ImmutableSet.builder();
    scenarioProviderBuilder = Optional.absent();
    fileReader = Optional.absent();
    resultListeners = newArrayList();
    showGui = false;
    repetitions = 1;
    masterSeed = 0L;
    numThreads = Runtime.getRuntime().availableProcessors();
    numBatches = 1;
    computerType = Computers.LOCAL;
  }

  /**
   * Creates a default builder for defining experiments.
   * 
   * @param computeFunc A {@link Function} that converts {@link SimArgs} to
   *          {@link SimResult}s.
   * @return A new {@link ExperimentBuilder} instance.
   */
  public static DefaultExperimentBuilder defaultInstance(
      Function<SimArgs, SimResult> computeFunc) {
    return new DefaultExperimentBuilder(computeFunc);
  }

  /**
   * Should return 'this', the builder.
   * @return 'this'.
   */
  protected abstract T self();

  // abstract SimTask createTask(SimArgs args);

  /**
   * Set the number of repetitions for each simulation.
   * @param times The number of repetitions.
   * @return This, as per the builder pattern.
   */
  public T repeat(int times) {
    checkArgument(times > 0,
        "The number of repetitions must be strictly positive, was %s.", times);
    repetitions = times;
    return self();
  }

  /**
   * Enable the GUI for each simulation. When a large number of simulations is
   * performed this may slow down the experiment significantly. The GUI can not
   * be shown when more than one thread is used.
   * @return This, as per the builder pattern.
   */
  public T showGui() {
    showGui = true;
    return self();
  }

  /*
   * Enable the GUI using the specified creator for each simulation. When a
   * large number of simulations is performed this may slow down the experiment
   * significantly. The GUI can not be shown when more than one thread is used.
   * @param uic The {@link UICreator} to use for creating the GUI.
   * @return This, as per the builder pattern.
   */
  // public T showGui(Object uic) {
  // uiCreator = uic;
  // return showGui();
  // }

  /**
   * Add a configuration to the experiment.
   * @param config The configuration to add.
   * @return This, as per the builder pattern.
   */
  public T addConfiguration(Configuration config) {
    checkArgument(!configurationsSet.contains(config));
    configurationsSet.add(config);
    return self();
  }

  /**
   * Adds all configurations to the experiment.
   * @param configs The configurations to add.
   * @return This, as per the builder pattern.
   */
  public T addConfigurations(Iterable<Configuration> configs) {
    final Set<Configuration> newConfigs = ImmutableSet.copyOf(configs);
    checkArgument(Sets.intersection(configurationsSet, newConfigs)
        .isEmpty());
    configurationsSet.addAll(newConfigs);
    return self();
  }

  /**
   * Add a scenario to the set of scenarios.
   * @param scenario The scenario to add.
   * @return This, as per the builder pattern.
   */
  public T addScenario(Scenario scenario) {
    scenariosBuilder.add(scenario);
    return self();
  }

  /**
   * Add all scenarios to the set of scenarios.
   * @param scenarios The scenarios to add.
   * @return This, as per the builder pattern.
   */
  public T addScenarios(List<? extends Scenario> scenarios) {
    scenariosBuilder.addAll(scenarios);
    return self();
  }

  /**
   * Adds a {@link com.github.rinde.rinsim.io.FileProvider.Builder} to the
   * experiment.
   * @param providerBuilder This builder will be used create a
   *          {@link FileProvider} instance to load scenarios.
   * @return This, as per the builder pattern.
   * @see #setScenarioReader(Function)
   */
  public T addScenarios(FileProvider.Builder providerBuilder) {
    scenarioProviderBuilder = Optional.of(providerBuilder);
    return self();
  }

  /**
   * Change the scenario reader which defines how {@link Path} instances are
   * converted to {@link Scenario} instances. By default ScenarioIO#reader() is
   * used as a scenario reader.
   * @param reader The reader to use.
   * @return This, as per the builder pattern.
   */
  public T setScenarioReader(Function<Path, ? extends Scenario> reader) {
    fileReader = Optional.of(reader);
    return self();
  }

  /**
   * Specify the number of threads to use for computing the experiments, the
   * default is <code>1</code>.
   * @param threads The number of threads to use.
   * @return This, as per the builder pattern.
   */
  public T withThreads(int threads) {
    checkArgument(threads > 0,
        "Only a positive number of threads is allowed, was %s.", threads);
    numThreads = threads;
    return self();
  }

  /**
   * Set the master random seed for the experiments.
   * @param seed The seed to use.
   * @return This, as per the builder pattern.
   */
  public T withRandomSeed(long seed) {
    masterSeed = seed;
    return self();
  }

  /*
   * Specify a {@link PostProcessor} which is used to gather additional results
   * from a simulation. The data gathered by the post-processor ends up in
   * {@link SimulationResult#simulationData}.
   * @param postProcessor The post-processor to use, by default there is no
   * post-processor.
   * @return This, as per the builder pattern.
   */
  public T usePostProcessor(PostProcessor<?, ?> postProcessor) {
    postProc = postProcessor;
    return self();
  }

  /**
   * Sets the number of batches that should be used when using the
   * {@link #computeDistributed()} setting.
   * @param num The number of batches to use.
   * @return This, as per the builder pattern.
   */
  public T numBatches(int num) {
    checkArgument(num > 0,
        "The number of batches must be strictly positive, was %s.", num);
    numBatches = num;
    return self();
  }

  /*
   * When this method is called the experiment will be performed in a
   * distributed fashion using the <a href="http://www.jppf.org/">JPPF</a>
   * framework. By default JPPF will attempt to connect to a driver on
   * <code>localhost</code>. For changing the JPPF settings, please consult the
   * <a href="http://www.jppf.org/doc/">JPPF documentation</a>. <p>
   * <b>Requirements:</b> {@link ..}, {@link ..} and {@link PostProcessor} (if
   * used) must implement {@link java.io.Serializable}. <p> <b>Incompatible
   * settings</b><br> The following settings will be ignored when computing is
   * done in a distributed fashion: <ul> <li>{@link #withThreads(int)}</li>
   * <li>{@link #showGui()}</li> <li>{@link #showGui(UICreator)}</li> </ul>
   * @return This, as per the builder pattern.
   */
  public T computeDistributed() {
    computerType = Computers.DISTRIBUTED;
    return self();
  }

  /**
   * This setting will perform the experiment locally (this is the default).
   * @return This, as per the builder pattern.
   */
  public T computeLocal() {
    computerType = Computers.LOCAL;
    return self();
  }

  /**
   * This setting will perform a 'dry-run' experiment. No computations will be
   * done. Note that this must be called <i>after</i> any calls to
   * {@link #computeDistributed()} or {@link #computeLocal()}, otherwise it has
   * no effect.
   * @param verbose If <code>true</code> additional information is printed.
   * @param stream The stream to write to.
   * @param error The error stream to write to.
   * @return This, as per the builder pattern.
   */
  public T dryRun(final boolean verbose, final PrintStream stream,
      final PrintStream error) {
    final Supplier<Computer> originalComputerType = computerType;
    computerType = new Supplier<Computer>() {
      @Override
      public Computer get() {
        return new DryRunComputer(originalComputerType, verbose, stream,
            error);
      }
    };
    return self();
  }

  /**
   * Adds the specified {@link ResultListener} to the experiment. This listener
   * will be called each time a simulation is done.
   * @param listener The listener to add.
   * @return This, as per the builder pattern.
   */
  public T addResultListener(ResultListener listener) {
    resultListeners.add(listener);
    return self();
  }

  /**
   * Perform the experiment. For every scenario every configuration is used
   * <code>n</code> times. Where <code>n</code> is the number of repetitions as
   * specified.
   * @return An {@link ExperimentResults} instance which contains all experiment
   *         parameters and the corresponding results.
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
  // public Optional<ExperimentResults> perform(PrintStream out, String... args)
  // {
  // final Optional<String> error = ExperimentCli.safeExecute(this, args);
  // if (error.isPresent()) {
  // out.println(error.get());
  // return Optional.absent();
  // }
  // return Optional.of(perform());
  // }

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
      checkArgument(fileReader.isPresent(),
          "A scenario reader must be specified.");
      scenarios.addAll(scenarioProviderBuilder.get().build(fileReader.get())
          .get());
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

    final ImmutableSet<Configuration> conf = ImmutableSet
        .copyOf(configurationsSet);

    checkArgument(!scenarios.isEmpty(), "At least one scenario is required.");
    checkArgument(!conf.isEmpty(), "At least one configuration is required.");
    final ImmutableSet.Builder<SimArgs> runnerBuilder = ImmutableSet
        .builder();
    for (final Configuration configuration : conf) {
      for (final Scenario scenario : scenarios) {
        for (int i = 0; i < repetitions; i++) {
          final long seed = seeds.get(i);
          runnerBuilder
              .add(new SimArgs(scenario, configuration, seed, showGui, postProc));
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

  public static class DefaultExperimentBuilder extends
      ExperimentBuilder<DefaultExperimentBuilder> {

    DefaultExperimentBuilder(Function<SimArgs, SimResult> computeFunc) {
      super(computeFunc);
    }

    @Override
    protected DefaultExperimentBuilder self() {
      return this;
    }

    // @Override
    // SimTask createTask(SimArgs args) {
    // return new FunctionRunner(args, function);
    // }
  }

  static class FunctionRunner implements SimTask {
    private final SimArgs args;
    private final Function<SimArgs, SimResult> executor;

    FunctionRunner(SimArgs args, Function<SimArgs, SimResult> exec) {
      this.args = args;
      executor = exec;
    }

    @Override
    public SimResult call() throws Exception {
      return requireNonNull(executor.apply(args));
    }
  }
}
