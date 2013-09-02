/**
 * 
 */
package rinde.sim.pdptw.central;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Queue;

import javax.measure.Measure;

import rinde.sim.core.Simulator;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.pdptw.common.DefaultParcel;
import rinde.sim.pdptw.common.PDPRoadModel;

/**
 * Adapts any {@link Solver} to its simplest form such that it is automatically
 * configured based on the simulator.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class SolverAdapter {

  private final Solver solver;
  private final Simulator simulator;
  private final PDPRoadModel roadModel;
  private final PDPModel pdpModel;

  /**
   * Create a new instance based on a solver and a simulator.
   * @param solver The {@link Solver} to use.
   * @param simulator The {@link Simulator} to use.
   */
  public SolverAdapter(Solver solver, Simulator simulator) {
    checkArgument(simulator.isConfigured());
    this.solver = solver;
    this.simulator = simulator;
    roadModel = simulator.getModelProvider().getModel(PDPRoadModel.class);
    pdpModel = simulator.getModelProvider().getModel(PDPModel.class);
  }

  /**
   * 
   * @return A list of routes, one for every vehicle.
   */
  public List<Queue<DefaultParcel>> solve() {
    return Solvers.solve(solver, roadModel, pdpModel, Measure.valueOf(simulator
        .getCurrentTime(), simulator.getTimeUnit()));
  }
}
