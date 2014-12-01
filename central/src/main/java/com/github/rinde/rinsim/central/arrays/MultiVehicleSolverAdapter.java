package com.github.rinde.rinsim.central.arrays;

import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.central.GlobalStateObject;
import com.github.rinde.rinsim.central.Solver;
import com.github.rinde.rinsim.central.arrays.ArraysSolvers.MVArraysObject;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.google.common.collect.ImmutableList;

/**
 * Adapter for {@link MultiVehicleArraysSolver} to conform to the {@link Solver}
 * interface.
 * @author Rinde van Lon 
 */
public class MultiVehicleSolverAdapter implements Solver {

  private final MultiVehicleArraysSolver solver;
  private final Unit<Duration> outputTimeUnit;

  /**
   * @param solver The solver to use.
   * @param outputTimeUnit The time unit which is expected by the specified
   *          solver.
   */
  public MultiVehicleSolverAdapter(MultiVehicleArraysSolver solver,
      Unit<Duration> outputTimeUnit) {
    this.solver = solver;
    this.outputTimeUnit = outputTimeUnit;
  }

  @Override
  public ImmutableList<ImmutableList<ParcelDTO>> solve(GlobalStateObject state) {
    final MVArraysObject o = ArraysSolvers.toMultiVehicleArrays(state,
        outputTimeUnit);

    final SolutionObject[] sols = solver.solve(o.travelTime, o.releaseDates,
        o.dueDates, o.servicePairs, o.serviceTimes, o.vehicleTravelTimes,
        o.inventories, o.remainingServiceTimes, o.currentDestinations,
        o.currentSolutions);
    final ImmutableList.Builder<ImmutableList<ParcelDTO>> b = ImmutableList
        .builder();
    for (final SolutionObject sol : sols) {
      b.add(ArraysSolvers.convertSolutionObject(sol, o.index2parcel));
    }
    return b.build();
  }
}
