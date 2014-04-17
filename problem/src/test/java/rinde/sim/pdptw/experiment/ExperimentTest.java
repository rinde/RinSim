/**
 * 
 */
package rinde.sim.pdptw.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.pdptw.central.Central;
import rinde.sim.pdptw.central.RandomSolver;
import rinde.sim.pdptw.common.DynamicPDPTWProblem;
import rinde.sim.pdptw.common.ObjectiveFunction;
import rinde.sim.pdptw.common.StatisticsDTO;
import rinde.sim.pdptw.experiment.Experiment.ExperimentResults;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;
import rinde.sim.pdptw.gendreau06.Gendreau06Scenario;
import rinde.sim.pdptw.scenario.PDPScenario;

import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ExperimentTest {

  public static StatisticsDTO singleRun(PDPScenario scenario,
      MASConfiguration c, long seed, ObjectiveFunction objFunc, boolean showGui) {
    return Experiment.singleRun(scenario, c, seed, objFunc, showGui, null,
        null).stats;
  }

  public static DynamicPDPTWProblem init(PDPScenario scenario,
      MASConfiguration config, long seed, boolean showGui) {
    return Experiment.init(scenario, config, seed, showGui,
        null);
  }

  @Test
  public void test() {
    final Gendreau06Scenario scenario = Gendreau06Parser.parse(
        new File("files/test/gendreau06/req_rapide_1_240_24"));

    final Experiment.Builder builder = Experiment
        .build(new Gendreau06ObjectiveFunction())
        .addScenario(scenario)
        .addConfiguration(Central.solverConfiguration(RandomSolver.supplier()))
        .usePostProcessor(new TestPostProcessor())
        .withRandomSeed(123);

    final ExperimentResults er = builder.perform();

    assertEquals(123, er.masterSeed);
    assertEquals(123, er.results.get(0).seed);

    @SuppressWarnings("unchecked")
    final List<Point> positions = (List<Point>) er.results.get(0).simulationData;
    assertEquals(11, positions.size());
    for (final Point p : positions) {
      assertEquals(new Point(2, 2.5), p);
    }
  }

  /**
   * Checks whether the ordering of results is as expected.
   */
  @Test
  public void multiThreadedOrder() {
    final Gendreau06Scenario scenario = Gendreau06Parser.parse(
        new File("files/test/gendreau06/req_rapide_1_240_24"));

    final Experiment.Builder builder = Experiment
        .build(new Gendreau06ObjectiveFunction())
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
    assertTrue(er.results.get(0).masConfiguration.toString().endsWith("A"));
    assertTrue(er.results.get(1).masConfiguration.toString().endsWith("B"));
    assertTrue(er.results.get(2).masConfiguration.toString().endsWith("C"));
    assertTrue(er.results.get(3).masConfiguration.toString().endsWith("D"));
  }

  static class TestPostProcessor implements
      PostProcessor<ImmutableList<Point>> {

    @Override
    public ImmutableList<Point> collectResults(Simulator sim) {
      final RoadModel rm = sim.getModelProvider().getModel(RoadModel.class);
      return ImmutableList.copyOf(rm.getObjectPositions());
    }
  }

}
