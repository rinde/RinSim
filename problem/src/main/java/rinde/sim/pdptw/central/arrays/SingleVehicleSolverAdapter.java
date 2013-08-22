/**
 * 
 */
package rinde.sim.pdptw.central.arrays;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.RoundingMode;
import java.util.Collection;

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import rinde.sim.pdptw.central.GlobalStateObject;
import rinde.sim.pdptw.central.GlobalStateObject.VehicleStateObject;
import rinde.sim.pdptw.central.Solver;
import rinde.sim.pdptw.central.arrays.ArraysSolvers.ArraysObject;
import rinde.sim.pdptw.common.ParcelDTO;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Ints;

/**
 * Adapter for {@link SingleVehicleArraysSolver} to conform to the
 * {@link Solver} interface.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class SingleVehicleSolverAdapter implements Solver {

  private final SingleVehicleArraysSolver solver;
  private final Unit<Duration> outputTimeUnit;

  /**
   * @param solver The solver to use.
   * @param outputTimeUnit The time unit which is expected by the specified
   *          solver.
   */
  public SingleVehicleSolverAdapter(SingleVehicleArraysSolver solver,
      Unit<Duration> outputTimeUnit) {
    this.solver = solver;
    this.outputTimeUnit = outputTimeUnit;
  }

  @Override
  public ImmutableList<ImmutableList<ParcelDTO>> solve(GlobalStateObject state) {
    checkArgument(state.vehicles.size() == 1, "This solver can only deal with the single vehicle problem, found %s vehicles.", state.vehicles
        .size());

    final VehicleStateObject v = state.vehicles.iterator().next();
    checkArgument(v.remainingServiceTime == 0, "This solver can not deal with remaining service time, it should be 0, it was %s.", v.remainingServiceTime);
    final Collection<ParcelDTO> inCargo = v.contents;

    // there are always two locations: the current vehicle location and
    // the depot
    final int numLocations = 2 + (state.availableParcels.size() * 2)
        + inCargo.size();

    if (numLocations == 2) {
      // there are no orders
      final ImmutableList<ParcelDTO> empty = ImmutableList.of();
      return ImmutableList.of(empty);
    } else if (state.availableParcels.size() + inCargo.size() == 1) {
      // if there is only one order, the solution is trivial
      if (!state.availableParcels.isEmpty()) {
        // parcels on the map require two visits (one for pickup, one
        // for delivery)
        final ParcelDTO dto = state.availableParcels.iterator().next();
        return ImmutableList.of(ImmutableList.of(dto, dto));
      } // else
      return ImmutableList.of(ImmutableList.copyOf(inCargo));
    }
    // else, we are going to look for the optimal solution

    final ArraysObject ao = ArraysSolvers
        .toSingleVehicleArrays(state, outputTimeUnit);

    final SolutionObject sol = solver
        .solve(ao.travelTime, ao.releaseDates, ao.dueDates, ao.servicePairs, ao.serviceTimes);

    return ImmutableList.of(ArraysSolvers
        .convertSolutionObject(sol, ao.point2dto, ao.locations));
  }

  static int fixTWstart(long start, long time) {
    return Math
        .max((DoubleMath.roundToInt(Ints.checkedCast(start - time) / 1000d, RoundingMode.CEILING)), 0);
  }

  static int fixTWend(long end, long time) {
    return Math
        .max((DoubleMath.roundToInt(Ints.checkedCast(end - time) / 1000d, RoundingMode.FLOOR)), 0);
  }

}
