/**
 * 
 */
package rinde.sim.pdptw.central;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.NonSI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

import rinde.sim.pdptw.central.arrays.ArraysSolverDebugger;
import rinde.sim.pdptw.central.arrays.ArraysSolverDebugger.MVASDebugger;
import rinde.sim.pdptw.central.arrays.ArraysSolverValidator;
import rinde.sim.pdptw.central.arrays.MultiVehicleSolverAdapter;
import rinde.sim.pdptw.central.arrays.RandomMVArraysSolver;
import rinde.sim.pdptw.central.arrays.SolutionObject;
import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.pdptw.common.StatsTracker.StatisticsDTO;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class SolverSimTest {

  @Test
  public void test() throws IOException {
    final MVASDebugger solver = ArraysSolverDebugger.wrap(ArraysSolverValidator
        .wrap(new RandomMVArraysSolver(new MersenneTwister(123))), false);

    final DynamicPDPTWScenario scenario = DynamicPDPTWScenario
        .convertToOffline(Gendreau06Parser
            .parse("files/test/gendreau06/req_rapide_1_240_24", 10));

    final Gendreau06ObjectiveFunction obj = new Gendreau06ObjectiveFunction();
    final StatisticsDTO stats = Central
        .solve(scenario, new MultiVehicleSolverAdapter(solver, scenario
            .getTimeUnit()), obj, false);

    assertEquals(1, solver.getInputMemory().size());
    assertEquals(1, solver.getOutputMemory().size());

    final SolutionObject[] sols = solver.getOutputMemory().get(0);

    int objVal = 0;
    for (final SolutionObject sol : sols) {
      objVal += sol.objectiveValue;
    }

    // convert the objective values computed by the solver to the unit of the
    // gendreau benchmark (minutes).
    final UnitConverter converter = scenario.getTimeUnit()
        .getConverterTo(NonSI.MINUTE);
    final double objValInMinutes = converter.convert(objVal);

    assertEquals(obj.computeCost(stats), objValInMinutes, 0.1);

  }
}
