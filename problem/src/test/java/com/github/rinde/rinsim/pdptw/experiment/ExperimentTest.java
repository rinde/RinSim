/**
 * 
 */
package com.github.rinde.rinsim.pdptw.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.central.Central;
import com.github.rinde.rinsim.pdptw.central.RandomSolver;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.experiment.Experiment;
import com.github.rinde.rinsim.pdptw.experiment.ExperimentResults;
import com.github.rinde.rinsim.pdptw.experiment.MASConfiguration;
import com.github.rinde.rinsim.pdptw.experiment.PostProcessor;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06Parser;
import com.github.rinde.rinsim.pdptw.gendreau06.Gendreau06Scenario;
import com.github.rinde.rinsim.scenario.Scenario;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
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
  public void test() {
    final Gendreau06Scenario scenario = Gendreau06Parser.parse(
        new File("files/test/gendreau06/req_rapide_1_240_24"));

    final Experiment.Builder builder = Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .addScenario(scenario)
        .addConfiguration(Central.solverConfiguration(RandomSolver.supplier()))
        .usePostProcessor(new TestPostProcessor())
        .withRandomSeed(123);

    final ExperimentResults er = builder.perform();

    assertEquals(123, er.masterSeed);
    assertEquals(123, er.results.asList().get(0).seed);

    @SuppressWarnings("unchecked")
    final List<Point> positions = (List<Point>) er.results.asList().get(0).simulationData
        .get();
    assertEquals(11, positions.size());
    for (final Point p : positions) {
      assertEquals(new Point(2, 2.5), p);
    }
  }

  /**
   * Checks whether the ordering of results is as expected.
   */
  // FIXME is this test still applicable now that we use sets for the results?
  // i.e. ordering is no longer important
  @Test
  public void multiThreadedOrder() {
    final Gendreau06Scenario scenario = Gendreau06Parser.parse(
        new File("files/test/gendreau06/req_rapide_1_240_24"));

    final Experiment.Builder builder = Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .addScenario(scenario)
        .addConfiguration(
            Central.solverConfiguration(RandomSolver.supplier(), "A"))
        .addConfiguration(
            Central.solverConfiguration(RandomSolver.supplier(), "B"))
        .addConfiguration(
            Central.solverConfiguration(RandomSolver.supplier(), "C"))
        .addConfiguration(
            Central.solverConfiguration(RandomSolver.supplier(), "D"))
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
