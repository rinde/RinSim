package rinde.sim.pdptw.central;

import java.io.IOException;

import org.junit.Test;

import rinde.sim.pdptw.central.arrays.RandomMVArraysSolver;
import rinde.sim.pdptw.experiment.Experiment;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;

public class CentralIntegrationTest {

  @Test
  public void test() throws IOException {
    // try test in RinLog?
    Experiment
        .build(new Gendreau06ObjectiveFunction())
        .addScenario(
            Gendreau06Parser.parse("files/test/gendreau06/req_rapide_1_240_24",
                3))
        .addConfiguration(
            Central.solverConfiguration(RandomMVArraysSolver.solverSupplier()))
        .repeat(10).perform();

  }

  /**
   * Test of {@link RandomSolver} on a scenario.
   */
  @Test
  public void testRandomSolver() {
    Experiment
        .build(new Gendreau06ObjectiveFunction())
        .addScenario(
            Gendreau06Parser.parse("files/test/gendreau06/req_rapide_1_240_24",
                10))
        .addConfiguration(

            Central.solverConfiguration(SolverValidator.wrap(RandomSolver
                .supplier())))
        .repeat(20)
        .perform();
  }
}
