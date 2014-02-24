package rinde.sim.pdptw.central;

import java.io.File;

import org.junit.Test;

import rinde.sim.pdptw.central.arrays.RandomMVArraysSolver;
import rinde.sim.pdptw.experiment.Experiment;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;

/**
 * Integration tests for the centralized facade.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class CentralIntegrationTest {

  /**
   * Test of {@link RandomMVArraysSolver} using the
   * {@link rinde.sim.pdptw.central.arrays.MultiVehicleArraysSolver} interface.
   */
  @Test
  public void test() {
    Experiment
        .build(new Gendreau06ObjectiveFunction())
        .addScenario(
            Gendreau06Parser.parse(new File(
                "files/test/gendreau06/req_rapide_1_240_24")))
        .addConfiguration(
            Central.solverConfiguration(RandomMVArraysSolver.solverSupplier()))
        .repeat(3)
        .perform();
  }

  /**
   * Test of {@link RandomSolver} on a scenario using the {@link Solver}
   * interface..
   */
  @Test
  public void testRandomSolver() {
    Experiment
        .build(new Gendreau06ObjectiveFunction())
        .addScenario(
            Gendreau06Parser.parse(new File(
                "files/test/gendreau06/req_rapide_1_240_24")))
        .addConfiguration(
            Central.solverConfiguration(SolverValidator.wrap(RandomSolver
                .supplier())))
        .repeat(6)
        .perform();
  }
}
