/**
 * 
 */
package com.github.rinde.rinsim.pdptw.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.TestObjectiveFunction;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioTestUtil;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon 
 * 
 */
public class ExperimentTest {

  public static StatisticsDTO singleRun(Scenario scenario,
      MASConfiguration c, long seed, ObjectiveFunction objFunc, boolean showGui) {
    return Experiment.singleRun(scenario, c, seed, objFunc, showGui, null,
        null).stats;
  }

  public static DynamicPDPTWProblem init(Scenario scenario,
      MASConfiguration config, long seed, boolean showGui) {
    return Experiment.init(scenario, config, seed, showGui,
        null);
  }

  @Test
  public void testPostProcessor() {
    final Scenario scenario = ScenarioTestUtil.create(123L);
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(scenario)
        .addConfiguration(TestMASConfiguration.create("test"))
        .usePostProcessor(new TestPostProcessor())
        .withRandomSeed(123);

    final ExperimentResults er = builder.perform();
    assertEquals(123, er.masterSeed);
    assertEquals(123, er.results.asList().get(0).seed);

    @SuppressWarnings("unchecked")
    final List<Point> positions = (List<Point>) er.results.asList().get(0).simulationData
        .get();
    assertEquals(10, positions.size());
  }

  /**
   * Checks whether the ordering of results is as expected.
   */
  // FIXME is this test still applicable now that we use sets for the results?
  // i.e. ordering is no longer important
  @Test
  public void multiThreadedOrder() {
    final Experiment.Builder builder = Experiment
        .build(TestObjectiveFunction.INSTANCE)
        .addScenario(ScenarioTestUtil.create(456L))
        .addConfiguration(
            TestMASConfiguration.create("A"))
        .addConfiguration(
            TestMASConfiguration.create("B"))
        .addConfiguration(
            TestMASConfiguration.create("C"))
        .addConfiguration(
            TestMASConfiguration.create("D"))
        .withThreads(4)
        .withRandomSeed(456);

    final ExperimentResults er = builder.perform();
    assertTrue(er.results.asList().get(0).masConfiguration.toString().endsWith(
        "A"));
    assertTrue(er.results.asList().get(1).masConfiguration.toString().endsWith(
        "B"));
    assertTrue(er.results.asList().get(2).masConfiguration.toString().endsWith(
        "C"));
    assertTrue(er.results.asList().get(3).masConfiguration.toString().endsWith(
        "D"));
  }

  static class TestPostProcessor implements
      PostProcessor<ImmutableList<Point>>, Serializable {
    private static final long serialVersionUID = -2166760289557525263L;

    @Override
    public ImmutableList<Point> collectResults(Simulator sim) {
      final RoadModel rm = sim.getModelProvider().getModel(RoadModel.class);
      return ImmutableList.copyOf(rm.getObjectPositions());
    }
  }

}
