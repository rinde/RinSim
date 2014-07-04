package rinde.sim.pdptw.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.JPPFConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rinde.sim.core.Simulator;
import rinde.sim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import rinde.sim.pdptw.central.Central;
import rinde.sim.pdptw.central.RandomSolver;
import rinde.sim.pdptw.central.SolverValidator;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.StopConditions;
import rinde.sim.pdptw.common.ObjectiveFunction;
import rinde.sim.pdptw.common.StatisticsDTO;
import rinde.sim.pdptw.experiment.ExperimentTest.TestPostProcessor;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;
import rinde.sim.pdptw.gendreau06.Gendreau06Scenario;
import rinde.sim.pdptw.scenario.Models;
import rinde.sim.pdptw.scenario.PDPScenario;
import rinde.sim.pdptw.scenario.ScenarioGenerator;

/**
 * Test for JPPF computation.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class JppfTest {
  @SuppressWarnings("null")
  static JPPFDriver driver;
  @SuppressWarnings("null")
  static Gendreau06Scenario gendreauScenario;

  /**
   * Starts the JPPF driver.
   */
  @BeforeClass
  public static void setUp() {
    JPPFConfiguration.getProperties().setBoolean("jppf.local.node.enabled",
        true);
    JPPFDriver.main(new String[] { "noLauncher" });
    driver = JPPFDriver.getInstance();

    gendreauScenario = Gendreau06Parser.parse(new File(
        "files/test/gendreau06/req_rapide_1_240_24"));
  }

  /**
   * Stops the JPPF driver.
   */
  @AfterClass
  public static void tearDown() {
    driver.shutdown();
  }

  /**
   * Checks determinism of two subsequent identical JPPF experiments.
   */
  @Test
  public void determinismJppfVsJppf() {
    final Experiment.Builder experimentBuilder = Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .computeDistributed()
        .addScenario(gendreauScenario)
        .withRandomSeed(123)
        .repeat(5)
        .addConfiguration(
            Central.solverConfiguration(
                SolverValidator.wrap(RandomSolver.supplier()), "A"));

    final ExperimentResults results1 = experimentBuilder.perform();
    final ExperimentResults results2 = experimentBuilder.perform();
    assertEquals(results1, results2);
  }

  /**
   * Checks determinism of a local experiment and a JPPF experiment, both with
   * identical settings. Using a gendreau scenario.
   */
  @Test
  public void determinismLocalVsJppf() {
    final Experiment.Builder experimentBuilder = Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .computeDistributed()
        .addScenario(gendreauScenario)
        .withRandomSeed(123)
        .repeat(1)
        .usePostProcessor(new TestPostProcessor())
        .addConfiguration(
            Central.solverConfiguration(
                SolverValidator.wrap(RandomSolver.supplier()), "A"));

    final ExperimentResults results3 = experimentBuilder.perform();
    experimentBuilder.computeLocal();
    final ExperimentResults results4 = experimentBuilder.perform();
    assertEquals(results3, results4);
    assertTrue(results3.results.get(0).simulationData.isPresent());
  }

  /**
   * Checks determinism of a local experiment and a JPPF experiment, both with
   * identical settings. Using a generated scenario.
   */
  @Test
  public void determinismGeneratedScenarioLocalVsJppf() {
    final RandomGenerator rng = new MersenneTwister(123L);
    final PDPScenario generatedScenario = ScenarioGenerator.builder()
        .addModel(Models.roadModel(20, true))
        .addModel(Models.pdpModel(TimeWindowPolicies.LIBERAL))
        .stopCondition(StopConditions.VEHICLES_DONE_AND_BACK_AT_DEPOT)
        .build().generate(rng, "hoi");

    final Experiment.Builder experimentBuilder = Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .computeDistributed()
        .addScenario(generatedScenario)
        .withRandomSeed(123)
        .repeat(1)
        .usePostProcessor(new TestPostProcessor())
        .addConfiguration(
            Central.solverConfiguration(
                SolverValidator.wrap(RandomSolver.supplier()), "A"));

    final ExperimentResults resultsDistributed = experimentBuilder.perform();
    final ExperimentResults resultsLocal = experimentBuilder.computeLocal()
        .perform();
    assertEquals(resultsLocal, resultsDistributed);
  }

  /**
   * Tests a post processor that returns objects which are not serializable.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testFaultyPostProcessor() {
    Experiment.build(Gendreau06ObjectiveFunction.instance())
        .computeDistributed()
        .addScenario(gendreauScenario)
        .withRandomSeed(123)
        .repeat(1)
        .usePostProcessor(new TestFaultyPostProcessor())
        .addConfiguration(
            Central.solverConfiguration(
                SolverValidator.wrap(RandomSolver.supplier()), "A"))
        .perform();

  }

  /**
   * Tests whether a not serializable objective function generates an exception.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testNotSerializableObjFunc() {
    Experiment
        .build(new NotSerializableObjFunc())
        .computeDistributed()
        .addScenario(gendreauScenario)
        .withRandomSeed(123)
        .repeat(1)
        .addConfiguration(
            Central.solverConfiguration(
                SolverValidator.wrap(RandomSolver.supplier()), "A"))
        .perform();
  }

  static class TestFaultyPostProcessor implements
      PostProcessor<NotSerializable>, Serializable {
    private static final long serialVersionUID = -2166760289557525263L;

    @Override
    public NotSerializable collectResults(Simulator sim) {
      return new NotSerializable();
    }
  }

  static class NotSerializableObjFunc implements ObjectiveFunction {

    @Override
    public boolean isValidResult(StatisticsDTO stats) {
      return true;
    }

    @Override
    public double computeCost(StatisticsDTO stats) {
      return 0;
    }

    @Override
    public String printHumanReadableFormat(StatisticsDTO stats) {
      return "NotSerializableObjFunc";
    }

  }

  static class NotSerializable {}
}
