package rinde.sim.pdptw.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
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

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

final class JppfComputer implements Computer {
  private static Optional<JPPFClient> CLIENT = Optional.absent();

  static JPPFClient getJPPFClient() {
    if (!CLIENT.isPresent()) {
      CLIENT = Optional.of(new JPPFClient());
    }
    return CLIENT.get();
  }

  @Override
  public ExperimentResults compute(Builder builder, Iterable<SimArgs> inputs) {
    final MemoryMapDataProvider dataProvider = new MemoryMapDataProvider();
    final JPPFJob job = new JPPFJob(dataProvider);

    final IdMap<MASConfiguration> configMap = new IdMap<MASConfiguration>("c",
        MASConfiguration.class);
    final IdMap<ScenarioProvider> scenarioMap = new IdMap<>("s",
        ScenarioProvider.class);
    final IdMap<ObjectiveFunction> objFuncMap = new IdMap<>("o",
        ObjectiveFunction.class);
    @SuppressWarnings("rawtypes")
    final IdMap<PostProcessor> ppMap = new IdMap<>("p", PostProcessor.class);
    final Map<String, PDPScenario> scenariosMap = newLinkedHashMap();
    for (final SimArgs args : inputs) {
      final String configId = configMap.storeAndGenerateId(dataProvider,
          args.masConfig);
      final String scenId = scenarioMap.storeAndGenerateId(dataProvider,
          new ScenarioProvider(ScenarioIO.write(args.scenario),
              args.scenario.getClass()));
      scenariosMap.put(scenId, args.scenario);

      final String objFuncId = objFuncMap.storeAndGenerateId(dataProvider,
          args.objectiveFunction);

      final Optional<String> postProcId;
      if (args.postProcessor.isPresent()) {
        postProcId = Optional.of(ppMap.storeAndGenerateId(
            dataProvider, args.postProcessor.get()));
      } else {
        postProcId = Optional.absent();
      }

      try {
        job.add(new SimulationTask(args.randomSeed, scenId, configId,
            objFuncId, postProcId));
      } catch (final JPPFException e) {
        throw new IllegalArgumentException(e);
      }
    }
    final List<Task<?>> finishedTasks;
    try {
      finishedTasks = getJPPFClient().submitJob(job);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }

    final ImmutableList.Builder<SimulationResult> results = ImmutableList
        .builder();
    for (final Task<?> t : finishedTasks) {
      final SimulationTask simTask = (SimulationTask) t;
      if (simTask.getThrowable() != null) {
        throw new IllegalArgumentException(simTask.getThrowable());
      }
      final SimTaskResult res = simTask.getResult();
      final PDPScenario scen = scenariosMap.get(simTask.getScenarioId());
      final MASConfiguration conf = dataProvider
          .getParameter(simTask.getConfigurationId());
      results.add(new SimulationResult(res.getStats(), scen, conf, simTask
          .getSeed(), res.getData()));
    }

    return new ExperimentResults(builder, results.build());
  }

  static class IdMap<T> {
    private final Class<T> clazz;
    final Map<T, String> configMap;
    int idNum;
    String prefix;

    IdMap(String idPrefix, Class<T> cls) {
      clazz = cls;
      configMap = newLinkedHashMap();
      idNum = 0;
      prefix = idPrefix;
    }

    public String storeAndGenerateId(DataProvider dp, T value) {
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
        dp.setParameter(id, value);
      }
      return id;
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

  static final class SimulationTask extends AbstractTask<SimTaskResult> {
    private static final long serialVersionUID = 5298683984670600238L;

    private final long seed;
    private final String scenarioId;
    private final String configurationId;
    private final String objectiveFunctionId;
    private final Optional<String> postProcessorId;

    SimulationTask(long randomSeed, String scenId, String configId,
        String objFuncId, Optional<String> postProcId) {
      seed = randomSeed;
      scenarioId = scenId;
      configurationId = configId;
      objectiveFunctionId = objFuncId;
      postProcessorId = postProcId;
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
