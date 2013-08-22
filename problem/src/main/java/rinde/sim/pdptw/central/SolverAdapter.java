/**
 * 
 */
package rinde.sim.pdptw.central;

import java.util.List;
import java.util.Queue;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

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
  private final Unit<Duration> timeUnit;
  private final Unit<Velocity> speedUnit;
  private final Unit<Length> distUnit;

  public SolverAdapter(Solver solver, Simulator simulator,
      Unit<Duration> timeUnit, Unit<Velocity> speedUnit, Unit<Length> distUnit) {
    this.solver = solver;
    this.simulator = simulator;
    roadModel = simulator.getModelProvider().getModel(PDPRoadModel.class);
    pdpModel = simulator.getModelProvider().getModel(PDPModel.class);
    this.timeUnit = timeUnit;
    this.speedUnit = speedUnit;
    this.distUnit = distUnit;
  }

  /**
   * 
   * @return A list of routes, one for every vehicle.
   */
  public List<Queue<DefaultParcel>> solve() {
    return Solvers.solve(solver, roadModel, pdpModel, simulator
        .getCurrentTime(), timeUnit, speedUnit, distUnit);
  }
}
