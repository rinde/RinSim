package rinde.sim.pdptw.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.io.Serializable;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

import rinde.sim.pdptw.common.DynamicPDPTWProblem;
import rinde.sim.pdptw.common.ObjectiveFunction;
import rinde.sim.pdptw.common.StatisticsDTO;
import rinde.sim.pdptw.experiment.Experiment.Builder;
import rinde.sim.pdptw.experiment.Experiment.Builder.SimArgs;
import rinde.sim.pdptw.experiment.Experiment.SimulationResult;
import rinde.sim.pdptw.scenario.PDPScenario;
import rinde.sim.pdptw.scenario.ScenarioIO;
import rinde.sim.scenario.ScenarioController.UICreator;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Ints;

final class JppfComputer implements Computer {
  private static Optional<JPPFClient> CLIENT = Optional.absent();
  private static final String JOB_NAME = "RinSim - Experiment";

  static JPPFClient getJPPFClient() {
    if (!CLIENT.isPresent()) {
      CLIENT = Optional.of(new JPPFClient());
    }
    return CLIENT.get();
  }

  @Override
  public ExperimentResults compute(Builder builder, Set<SimArgs> inputs) {
    final IdMap<MASConfiguration> configMap = new IdMap<MASConfiguration>("c",
        MASConfiguration.class);
    final IdMap<ScenarioProvider> scenarioMap = new IdMap<>("s",
        ScenarioProvider.class);
    final IdMap<ObjectiveFunction> objFuncMap = new IdMap<>("o",
        ObjectiveFunction.class);

    final List<ResultListener> listeners = newArrayList(builder.resultListeners);

    @SuppressWarnings("rawtypes")
    final IdMap<PostProcessor> ppMap = new IdMap<>("p", PostProcessor.class);
    final Map<String, PDPScenario> scenariosMap = newLinkedHashMap();

    // create tasks
    final List<SimulationTask> tasks = newArrayList();
    for (final SimArgs args : inputs) {
      final String configId = configMap.storeAndGenerateId(
          args.masConfig);
      final String scenId = scenarioMap.storeAndGenerateId(
          new ScenarioProvider(ScenarioIO.write(args.scenario),
              args.scenario.getClass()));
      scenariosMap.put(scenId, args.scenario);
      final String objFuncId = objFuncMap.storeAndGenerateId(
          args.objectiveFunction);

      final Optional<String> postProcId;
      if (args.postProcessor.isPresent()) {
        postProcId = Optional.of(ppMap.storeAndGenerateId(args.postProcessor
            .get()));
      } else {
        postProcId = Optional.absent();
      }
      tasks.add(new SimulationTask(tasks.size(), args.randomSeed, scenId,
          configId, objFuncId, postProcId));
    }

    // this sorts tasks using this chain: scenario, configuration, objective
    // function, postprocessor, seed
    Collections.sort(tasks);

    // determine size of batches
    final int numBatches = Math.min(tasks.size(), builder.numBatches);
    final int batchSize = DoubleMath.roundToInt(tasks.size()
        / (double) numBatches, RoundingMode.CEILING);

    final Map<Task<?>, JPPFJob> taskJobMap = newLinkedHashMap();
    final ResultsCollector res = new ResultsCollector(tasks.size(),
        scenariosMap, taskJobMap, listeners);
    final List<JPPFJob> jobs = newArrayList();
    for (int i = 0; i < numBatches; i++) {
      final JPPFJob job = new JPPFJob(new MemoryMapDataProvider(), res);
      job.setName(Joiner.on("").join(JOB_NAME, " ", i + 1, "/", numBatches));
      jobs.add(job);
      for (final SimulationTask t : tasks.subList(i * batchSize, (i + 1)
          * batchSize)) {
        try {

          final MASConfiguration config = configMap.getValue(t
              .getConfigurationId());
          final ScenarioProvider scenario = scenarioMap.getValue(t
              .getScenarioId());
          final ObjectiveFunction objFunc = objFuncMap.getValue(t
              .getObjectiveFunctionId());
          if (t.getPostProcessorId().isPresent()) {
            job.getDataProvider().setParameter(t.getPostProcessorId().get(),
                ppMap.getValue(t.getPostProcessorId().get()));
          }
          job.getDataProvider().setParameter(t.getConfigurationId(), config);
          job.getDataProvider().setParameter(t.getScenarioId(), scenario);
          job.getDataProvider().setParameter(t.getObjectiveFunctionId(),
              objFunc);

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
    // getJPPFClient().close();
    return new ExperimentResults(builder, res.buildResults());
  }

  static SimulationResult processResult(SimulationTask simTask,
      final Map<String, PDPScenario> scenariosMap,
      final Map<Task<?>, JPPFJob> jobMap) {
    checkNotNull(simTask);
    if (simTask.getThrowable() != null) {
      throw new IllegalArgumentException(simTask.getThrowable());
    }
    final SimTaskResult result = simTask.getResult();
    final PDPScenario scen = scenariosMap.get(simTask.getScenarioId());
    final MASConfiguration conf = jobMap.get(simTask).getDataProvider()
        .getParameter(simTask.getConfigurationId());
    return new SimulationResult(result.getStats(), scen, conf, simTask
        .getSeed(), result.getData());
  }

  static class ResultsCollector implements TaskResultListener {
    private final ImmutableSet.Builder<SimulationResult> results;
    private final Map<String, PDPScenario> scenariosMap;
    private final Map<Task<?>, JPPFJob> taskJobMap;
    private final List<ResultListener> listeners;
    private final int expectedNumResults;
    private int receivedNumResults;
    private Optional<IllegalArgumentException> exception;

    ResultsCollector(int expectedNumberOfResults,
        final Map<String, PDPScenario> scenMap,
        final Map<Task<?>, JPPFJob> tjMap, List<ResultListener> list) {
      results = ImmutableSet.builder();
      scenariosMap = scenMap;
      taskJobMap = tjMap;
      listeners = list;
      expectedNumResults = expectedNumberOfResults;
      receivedNumResults = 0;
      exception = Optional.absent();
    }

    @Override
    public void resultsReceived(@Nullable TaskResultEvent event) {
      checkNotNull(event);
      for (final Task<?> t : event.getTasks()) {
        final SimulationTask simTask = (SimulationTask) t;
        try {
          final SimulationResult res = processResult(simTask, scenariosMap,
              taskJobMap);
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

    public void awaitResults() {
      while (!isComplete() && !exception.isPresent()) {
        try {
          Thread.sleep(1000);
        } catch (final InterruptedException e) {
          throw new IllegalStateException(e);
        }
      }
      if (exception.isPresent()) {
        throw exception.get();
      }
    }

    public boolean isComplete() {
      return receivedNumResults == expectedNumResults;
    }

    public ImmutableSet<SimulationResult> buildResults() {
      return results.build();
    }
  }

  static class SimResultWrapper implements Comparable<SimResultWrapper> {
    public final SimulationResult result;
    public final int index;

    public SimResultWrapper(SimulationResult r, int i) {
      result = r;
      index = i;
    }

    @Override
    public int compareTo(SimResultWrapper o) {
      return Ints.compare(index, o.index);
    }
  }

  static class IdMap<T> {
    private final BiMap<T, String> configMap;
    private int idNum;
    private final String prefix;
    private final Class<T> clazz;

    IdMap(String idPrefix, Class<T> cls) {
      clazz = cls;
      configMap = HashBiMap.create();
      idNum = 0;
      prefix = idPrefix;
    }

    public String storeAndGenerateId(T value) {
      checkArgument(
          value instanceof Serializable,
          "When using JPPF, instances of %s must implement Serializable, found: %s.",
          clazz,
          value);
      final String id;
      if (configMap.containsKey(value)) {
        id = configMap.get(value);
      } else {
        id = prefix + idNum++;
        configMap.put(value, id);
      }
      return id;
    }

    public T getValue(String id) {
      return configMap.inverse().get(id);
    }

    public String getKey(T value) {
      return configMap.get(value);
    }
  }

  /**
   * This class provides instances of {@link PDPScenario}. This class equals
   * another if the provided scenarios are equal.
   * 
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  static final class ScenarioProvider implements Supplier<PDPScenario>,
      Serializable {
    private static final long serialVersionUID = 1738175155810322872L;

    private final String serializedScenario;
    private final Class<?> scenarioClass;
    @Nullable
    private transient PDPScenario localCache;

    ScenarioProvider(String serialScen, Class<?> clz) {
      serializedScenario = serialScen;
      scenarioClass = clz;
      localCache = null;
    }

    @SuppressWarnings("null")
    @Override
    public PDPScenario get() {
      if (localCache == null) {
        localCache = (PDPScenario) ScenarioIO.read(
            serializedScenario, scenarioClass);
      }
      return localCache;
    }

    @Override
    public int hashCode() {
      return serializedScenario.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (other == null || other.getClass() != getClass()) {
        return false;
      }
      final ScenarioProvider sp = (ScenarioProvider) other;
      return Objects.equal(serializedScenario, sp.serializedScenario);
    }
  }

  static final class SimulationTask extends AbstractTask<SimTaskResult>
      implements Comparable<SimulationTask> {
    private static final long serialVersionUID = 5298683984670600238L;

    private final long seed;
    private final String scenarioId;
    private final String configurationId;
    private final String objectiveFunctionId;
    private final Optional<String> postProcessorId;
    private final String id;
    private final int hashCode;
    private final int index;

    SimulationTask(int ind, long randomSeed, String scenId, String configId,
        String objFuncId, Optional<String> postProcId) {
      index = ind;
      seed = randomSeed;
      scenarioId = scenId;
      configurationId = configId;
      objectiveFunctionId = objFuncId;
      postProcessorId = postProcId;
      id = Joiner.on("-").join(seed, scenarioId, configurationId,
          objectiveFunctionId, postProcessorId);
      hashCode = Objects.hashCode(seed, scenarioId, configurationId,
          objectiveFunctionId, postProcessorId);
    }

    public int getIndex() {
      return index;
    }

    @Override
    public void run() {
      // gather data from provider
      final DataProvider dataProvider = getDataProvider();
      checkNotNull(
          dataProvider,
          "Probable problem: your MASConfiguration/ObjectiveFunction/PostProcessor is not fully serializable.");

      final Supplier<PDPScenario> scenario = getDataProvider().getParameter(
          scenarioId);
      final MASConfiguration configuration = getDataProvider().getParameter(
          configurationId);
      final ObjectiveFunction objectiveFunction = getDataProvider()
          .getParameter(objectiveFunctionId);

      // perform simulation
      final DynamicPDPTWProblem prob = Experiment.init(scenario.get(),
          configuration, seed, false, Optional.<UICreator> absent());
      final StatisticsDTO stats = prob.simulate();

      final Optional<Object> data;
      if (postProcessorId.isPresent()) {
        final PostProcessor<?> postProcessor = getDataProvider().getParameter(
            postProcessorId.get());
        data = Optional.of((Object) postProcessor.collectResults(prob
            .getSimulator()));
        checkArgument(data.get() instanceof Serializable,
            "Your PostProcessor must generate Serializable objects, found %s.",
            data.get());
      } else {
        data = Optional.absent();
      }
      checkState(objectiveFunction.isValidResult(stats),
          "The simulation did not result in a valid result: %s.", stats);

      setResult(new SimTaskResult(stats, data));
    }

    public long getSeed() {
      return seed;
    }

    public String getScenarioId() {
      return scenarioId;
    }

    public String getConfigurationId() {
      return configurationId;
    }

    public String getObjectiveFunctionId() {
      return objectiveFunctionId;
    }

    public Optional<String> getPostProcessorId() {
      return postProcessorId;
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
      return Objects.equal(t.seed, seed) &&
          Objects.equal(t.scenarioId, scenarioId) &&
          Objects.equal(t.configurationId, configurationId) &&
          Objects.equal(t.objectiveFunctionId, objectiveFunctionId) &&
          Objects.equal(t.postProcessorId, postProcessorId);
    }

    @Override
    public int compareTo(SimulationTask o) {
      return ComparisonChain.start()
          .compare(scenarioId, o.scenarioId)
          .compare(configurationId, o.configurationId)
          .compare(objectiveFunctionId, o.objectiveFunctionId)
          .compare(postProcessorId.orNull(), o.postProcessorId.orNull(),
              Ordering.natural().nullsLast())
          .compare(seed, o.seed)
          .result();
    }
  }

  static class SimTaskResult implements Serializable {
    private static final long serialVersionUID = -631947579134555016L;
    private final StatisticsDTO stats;
    private final Optional<?> data;

    public SimTaskResult(StatisticsDTO stat, Optional<?> d) {
      stats = stat;
      data = d;
    }

    public StatisticsDTO getStats() {
      return stats;
    }

    public Optional<?> getData() {
      return data;
    }
  }
}
