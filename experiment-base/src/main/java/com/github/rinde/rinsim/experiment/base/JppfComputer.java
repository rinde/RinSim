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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.client.event.TaskResultListener;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.node.protocol.Task;
import org.jppf.task.storage.DataProvider;
import org.jppf.task.storage.MemoryMapDataProvider;

import com.google.common.base.Converter;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;

final class JppfComputer implements Computer {
  private static Optional<JPPFClient> client = Optional.absent();
  private static final String JOB_NAME = "RinSim - Experiment";
  private static final String COMPUTE_FUNC_ID = "ComputeFunc";
  private static final long THREAD_SLEEP_MS = 1000L;

  static JPPFClient getJPPFClient() {
    if (!client.isPresent()) {
      client = Optional.of(new JPPFClient());
    }
    return client.get();
  }

  @Override
  public ExperimentResults compute(ExperimentBuilder<?> builder,
      Set<SimArgs> inputs) {

    checkArgument(builder.computeFunction instanceof Serializable);

    final IdMap<Configuration> configMap = new IdMap<>("c",
        builder.configurationConverter);
    final IdMap<Scenario> scenarioMap = new IdMap<>("s",
        builder.scenarioConverter);

    final List<ResultListener> listeners = newArrayList(builder.resultListeners);
    final Map<String, Scenario> scenariosMap = newLinkedHashMap();
    Map<SimulationTask, SimArgs> taskArgumentsMap = newLinkedHashMap();

    // create tasks
    final List<SimulationTask> tasks = newArrayList();
    for (final SimArgs args : inputs) {
      final String configId = configMap.storeAndGenerateId(
          args.configuration);

      final String scenId = scenarioMap.storeAndGenerateId(args.scenario);
      scenariosMap.put(scenId, args.scenario);
      SimulationTask task = new SimulationTask(args.randomSeed, scenId,
          configId);
      taskArgumentsMap.put(task, args);
      tasks.add(task);
    }

    // this sorts tasks using this chain: scenario, configuration, objective
    // function, postprocessor, seed
    Collections.sort(tasks);

    // determine size of batches
    final int numBatches = Math.min(tasks.size(), builder.numBatches);
    final int batchSize = DoubleMath.roundToInt(tasks.size()
        / (double) numBatches, RoundingMode.CEILING);

    final Map<Task<?>, JPPFJob> taskJobMap = newLinkedHashMap();
    final ResultsCollector res = new ResultsCollector(tasks.size(), listeners,
        taskArgumentsMap);
    final List<JPPFJob> jobs = newArrayList();
    for (int i = 0; i < numBatches; i++) {
      final JPPFJob job = new JPPFJob(new MemoryMapDataProvider(), res);
      job.setName(Joiner.on("").join(JOB_NAME, " ", i + 1, "/", numBatches));
      job.getDataProvider().setParameter(COMPUTE_FUNC_ID,
          builder.computeFunction);
      jobs.add(job);

      for (final SimulationTask t : tasks.subList(i * batchSize, (i + 1)
          * batchSize)) {
        try {
          final Supplier<Configuration> config = Suppliers.memoize(configMap
              .getSerializableValue(t.getConfigurationId()));
          final Supplier<Scenario> scenario = Suppliers.memoize(scenarioMap
              .getSerializableValue(t.getScenarioId()));

          job.getDataProvider().setParameter(t.getConfigurationId(), config);
          job.getDataProvider().setParameter(t.getScenarioId(), scenario);
          job.add(t);
        } catch (final JPPFException e) {
          throw new IllegalStateException(e);
        }
        taskJobMap.put(t, job);
      }
    }
    for (final ResultListener l : listeners) {
      l.startComputing(tasks.size());
    }

    checkState(!getJPPFClient().isClosed());
    try {
      for (final JPPFJob job : jobs) {
        getJPPFClient().submitJob(job);
      }
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
    res.awaitResults();
    for (final ResultListener l : listeners) {
      l.doneComputing();
    }
    return new ExperimentResults(builder, res.buildResults());
  }

  private static class ResultsCollector implements TaskResultListener {
    private final ImmutableSet.Builder<SimResultContainer> results;
    private final List<ResultListener> listeners;
    private final int expectedNumResults;
    private int receivedNumResults;
    private Optional<IllegalArgumentException> exception;
    private final Map<SimulationTask, SimArgs> taskArgumentsMap;

    ResultsCollector(int expectedNumberOfResults, List<ResultListener> list,
        Map<SimulationTask, SimArgs> taskArgumentsMap) {
      results = ImmutableSet.builder();
      listeners = list;
      expectedNumResults = expectedNumberOfResults;
      receivedNumResults = 0;
      exception = Optional.absent();
      this.taskArgumentsMap = taskArgumentsMap;
    }

    @Override
    public void resultsReceived(@Nullable TaskResultEvent event) {
      requireNonNull(event);
      for (final Task<?> t : event.getTasks()) {
        final SimulationTask simTask = (SimulationTask) t;
        try {
          final SimResultContainer res = processResult(simTask);
          results.add(res);
          for (final ResultListener l : listeners) {
            l.receive(res);
          }
        } catch (final IllegalArgumentException iae) {
          exception = Optional.of(iae);
        }
      }
      receivedNumResults += event.getTasks().size();
    }

    SimResultContainer processResult(SimulationTask simTask) {
      requireNonNull(simTask);
      if (simTask.getThrowable() != null) {
        throw new IllegalArgumentException(simTask.getThrowable());
      }
      return SimResultContainer.create(taskArgumentsMap.get(simTask),
          simTask.getResult());
    }

    void awaitResults() {
      while (!isComplete() && !exception.isPresent()) {
        try {
          Thread.sleep(THREAD_SLEEP_MS);
        } catch (final InterruptedException e) {
          throw new IllegalStateException(e);
        }
      }
      if (exception.isPresent()) {
        throw exception.get();
      }
    }

    boolean isComplete() {
      return receivedNumResults == expectedNumResults;
    }

    ImmutableSet<SimResultContainer> buildResults() {
      return results.build();
    }
  }

  private static class IdMap<T> {
    private final BiMap<Supplier<T>, String> configMap;
    private int idNum;
    private final String prefix;
    private final Converter<T, Serializable> converter;

    IdMap(String idPrefix, Converter<T, Serializable> conv) {
      configMap = HashBiMap.create();
      idNum = 0;
      prefix = idPrefix;
      converter = conv;
    }

    String storeAndGenerateId(T value) {
      Serializable serialForm = converter.convert(value);
      Supplier<T> serialSup = Suppliers.compose(converter.reverse(),
          Suppliers.ofInstance(serialForm));
      final String id;
      if (configMap.containsKey(serialSup)) {
        id = configMap.get(serialSup);
      } else {
        id = prefix + idNum++;
        configMap.put(serialSup, id);
      }
      return id;
    }

    Supplier<T> getSerializableValue(String id) {
      return configMap.inverse().get(id);
    }
  }

  private static final class SimulationTask extends AbstractTask<SimResult>
      implements Comparable<SimulationTask> {
    private static final long serialVersionUID = 5298683984670600238L;

    private final long seed;
    private final String scenarioId;
    private final String configurationId;

    private final String id;
    private final int hashCode;

    SimulationTask(long randomSeed, String scenId, String configId) {
      seed = randomSeed;
      scenarioId = scenId;
      configurationId = configId;
      id = Joiner.on("-").join(seed, scenarioId, configurationId);
      hashCode = Objects.hash(seed, scenarioId, configurationId);
    }

    @Override
    public void run() {
      // gather data from provider
      final DataProvider dataProvider = getDataProvider();
      System.out.println(dataProvider);
      checkNotNull(
          dataProvider,
          "Probable problem: your MASConfiguration/ObjectiveFunction/PostProcessor is not fully serializable.");

      final Supplier<Scenario> scenario = getDataProvider().getParameter(
          scenarioId);
      final Supplier<Configuration> configuration = getDataProvider()
          .getParameter(configurationId);

      Function<SimArgs, SimResult> computeFunc = getDataProvider()
          .getParameter(COMPUTE_FUNC_ID);

      SimArgs args = new SimArgs(scenario.get(), configuration.get(), seed,
          false, null);
      setResult(computeFunc.apply(args));
    }

    String getScenarioId() {
      return scenarioId;
    }

    String getConfigurationId() {
      return configurationId;
    }

    @Override
    public String getId() {
      return id;
    }

    @Deprecated
    @Override
    public void setId(@Nullable String id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == null || o.getClass() != this.getClass()) {
        return false;
      }
      final SimulationTask t = (SimulationTask) o;
      return Objects.equals(t.seed, seed) &&
          Objects.equals(t.scenarioId, scenarioId) &&
          Objects.equals(t.configurationId, configurationId);
    }

    @Override
    public int compareTo(@Nullable SimulationTask o) {
      checkNotNull(o);
      return ComparisonChain.start()
          .compare(scenarioId, o.scenarioId)
          .compare(configurationId, o.configurationId)
          .compare(seed, o.seed)
          .result();
    }
  }
}
