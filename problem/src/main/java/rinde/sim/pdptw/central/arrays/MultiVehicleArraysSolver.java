/**
 * 
 */
package rinde.sim.pdptw.central.arrays;

import javax.annotation.Nullable;

/**
 * Interface for solvers for the multi vehicle pickup-and-delivery problem with
 * time windows (PDPTW). This interface uses <code>int</code> arrays for
 * specification of the inputs and outputs.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface MultiVehicleArraysSolver {

  /**
   * Computes a solution for the multi vehicle PDPTW as specified by the
   * parameters. The returned solution does not necessarily need to be optimal
   * but it needs to be feasible. The {@link ArraysSolverValidator} can check
   * whether a {@link MultiVehicleArraysSolver} produces a valid solution and it
   * can check whether the parameters for the {@link MultiVehicleArraysSolver}
   * are valid.
   * <p>
   * All times are in units relative to the current time (0). All constraints
   * are soft, i.e. lateness at service locations and at depot are allowed. The
   * start location has index 0, the end location (depot) has index n-1.
   * 
   * @param travelTime Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
   *          .
   * @param releaseDates Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
   *          .
   * @param dueDates Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
   *          .
   * @param servicePairs Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
   *          .
   * @param serviceTimes Parameter as specified by
   *          {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
   *          .
   * @param vehicleTravelTimes <code>v x n</code> matrix, where <code>v</code>
   *          is the number of vehicles and <code>n</code> is the number of
   *          locations. Contains travel times from each vehicle to every
   *          location. Value <code>verhicleTravelTimes[i][j]</code> indicates
   *          travel time from location of vehicle <code>i</code> to location
   *          <code>j</code>.
   * @param inventories <code>m x 2</code> matrix of vehicle and location pairs.
   *          Value at <code>inventories[i][0]</code> indicates the vehicle,
   *          <code>inventories[i][1]</code> indicates the location. Vehicles
   *          may occur more than once (i.e. they may have more than one
   *          location in their inventory). Only delivery locations can be in
   *          the inventory.
   * @param remainingServiceTimes contains the remaining service time for every
   *          vehicle. The length of this array equals the number of vehicles.
   * @param currentDestinations contains a pointer to a location which indicates
   *          the current destination for every vehicle. If the value is
   *          <code>0</code> for a vehicle, it means that the vehicle has no
   *          destination. In case a vehicle has a destination, <b>the vehicle
   *          is obliged to continue travelling towards this destination and
   *          service it</b>. The length of this array equals the number of
   *          vehicles.
   * @return A solution object for every vehicle. Each location may occur in
   *         only one route at a time. Routes must respect the inventories, i.e.
   *         a vehicle with a location in its inventory <i>must</i> have that
   *         location in its route.
   */
  SolutionObject[] solve(int[][] travelTime, int[] releaseDates,
      int[] dueDates, int[][] servicePairs, int[] serviceTimes,
      int[][] vehicleTravelTimes, int[][] inventories,
      int[] remainingServiceTimes, int[] currentDestinations,
      @Nullable SolutionObject[] currentSolutions);

}
