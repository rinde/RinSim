package rinde.sim.pdptw.central;

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.MersenneTwister;

import rinde.sim.pdptw.central.Central.SolverCreator;
import rinde.sim.pdptw.central.arrays.ArraysSolverDebugger;
import rinde.sim.pdptw.central.arrays.ArraysSolverDebugger.MVASDebugger;
import rinde.sim.pdptw.central.arrays.ArraysSolverValidator;
import rinde.sim.pdptw.central.arrays.MultiVehicleArraysSolver;
import rinde.sim.pdptw.central.arrays.MultiVehicleSolverAdapter;
import rinde.sim.pdptw.central.arrays.RandomMVArraysSolver;

/**
 * A solver creator useful for debugging.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class DebugSolverCreator implements SolverCreator {
  /**
   * The arrays solver that is used to compute solutions.
   */
  public final MVASDebugger arraysSolver;

  /**
   * The solver that a wrapper for {@link #arraysSolver}.
   */
  public final SolverDebugger solver;

  /**
   * Create a new instance using the specified time unit and seed.
   * @param timeUnit The time unit to use for the underlying
   *          {@link RandomMVArraysSolver}.
   * @param seed The seed to use for the {@link RandomMVArraysSolver}.
   */
  public DebugSolverCreator(long seed, Unit<Duration> timeUnit) {
    this(new RandomMVArraysSolver(new MersenneTwister(seed)), timeUnit);
  }

  /**
   * Create a new instance using the specified time unit and seed.
   * @param arrSolver The {@link MultiVehicleArraysSolver} that is used for
   *          solving.
   * @param timeUnit The time unit to use for the underlying
   *          {@link RandomMVArraysSolver}.
   */
  public DebugSolverCreator(MultiVehicleArraysSolver arrSolver,
      Unit<Duration> timeUnit) {
    arraysSolver = ArraysSolverDebugger.wrap(
        ArraysSolverValidator.wrap(arrSolver), false);
    solver = SolverDebugger.wrap(SolverValidator
        .wrap(new MultiVehicleSolverAdapter(arraysSolver, timeUnit)), false);
  }

  @Override
  public Solver create(long seed) {
    return solver;
  }
}
