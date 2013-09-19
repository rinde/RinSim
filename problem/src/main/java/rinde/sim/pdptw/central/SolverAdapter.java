/**
 * 
 */
package rinde.sim.pdptw.central;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Queue;

import rinde.sim.core.Simulator;
import rinde.sim.pdptw.central.Solvers.MVSolverHandle;
import rinde.sim.pdptw.common.DefaultParcel;

/**
 * Adapts any {@link Solver} to its simplest form such that it is automatically
 * configured based on the simulator.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class SolverAdapter {

  private final MVSolverHandle handle;

  /**
   * Create a new instance based on a solver and a simulator.
   * @param solver The {@link Solver} to use.
   * @param simulator The {@link Simulator} to use.
   */
  public SolverAdapter(Solver solver, Simulator simulator) {
    checkArgument(simulator.isConfigured());
    handle = Solvers.solver(solver, simulator);
  }

  /**
   * 
   * @return A list of routes, one for every vehicle.
   */
  public List<Queue<DefaultParcel>> solve() {
    return handle.solve();
  }
}
