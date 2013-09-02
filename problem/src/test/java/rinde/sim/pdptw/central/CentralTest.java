/**
 * 
 */
package rinde.sim.pdptw.central;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

import rinde.sim.pdptw.central.Central.SolverCreator;
import rinde.sim.pdptw.central.arrays.ArraysSolverValidator;
import rinde.sim.pdptw.central.arrays.MultiVehicleSolverAdapter;
import rinde.sim.pdptw.central.arrays.RandomMVArraysSolver;
import rinde.sim.pdptw.experiments.Experiment;
import rinde.sim.pdptw.experiments.Experiment.ExperimentResults;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;
import rinde.sim.pdptw.gendreau06.Gendreau06Scenario;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class CentralTest {
  /**
   * Tests whether the SolverConfigurator works.
   * @throws IOException When something goes wrong with loading the file.
   */
  @Test
  public void test() throws IOException {
    final Gendreau06Scenario scenario = Gendreau06Parser
        .parse("files/test/gendreau06/req_rapide_1_240_24", 10);

    final SolverCreator s = new SolverCreator() {
      @Override
      public Solver create(long seed) {
        return SolverValidator.wrap(new MultiVehicleSolverAdapter(
            ArraysSolverValidator.wrap(new RandomMVArraysSolver(
                new MersenneTwister(seed))), scenario.getTimeUnit()));
      }
    };
    final Experiment.Builder builder = Experiment
        .build(new Gendreau06ObjectiveFunction()) //
        .addScenario(scenario) //
        .addConfigurator(Central.solverConfigurator(s)) //
        .withRandomSeed(123);

    final ExperimentResults res1 = builder.perform();
    final ExperimentResults res2 = builder.perform();

    assertEquals(res1.results, res2.results);
  }
}
