/**
 * 
 */
package rinde.sim.pdptw.central.arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.primitives.Ints;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class RandomMVArraysSolver implements MultiVehicleArraysSolver {

  private final RandomGenerator rng;

  public RandomMVArraysSolver(RandomGenerator rng) {
    this.rng = rng;
  }

  @Override
  public SolutionObject[] solve(int[][] travelTime, int[] releaseDates,
      int[] dueDates, int[][] servicePairs, int[] serviceTimes,
      int[][] vehicleTravelTimes, int[][] inventories,
      int[] remainingServiceTimes, int[] currentDestinations) {

    final int n = travelTime.length;
    final int v = vehicleTravelTimes.length;

    final List<List<Integer>> routes = newArrayList();
    for (int i = 0; i < v; i++) {
      routes.add(new ArrayList<Integer>());
    }
    for (int i = 0; i < inventories.length; i++) {
      routes.get(inventories[i][0]).add(inventories[i][1]);
    }

    for (int i = 0; i < servicePairs.length; i++) {
      final List<Integer> route = routes.get(rng.nextInt(v));
      route.add(servicePairs[i][0]);
      route.add(servicePairs[i][1]);
    }

    final Map<Integer, Integer> servicePairMap = newHashMap();
    for (int i = 0; i < servicePairs.length; i++) {
      servicePairMap.put(servicePairs[i][0], servicePairs[i][1]);
    }

    for (int i = 0; i < v; i++) {
      final boolean hasDest = vehicleTravelTimes[i][1] == Integer.MAX_VALUE
          || vehicleTravelTimes[i][2] == Integer.MAX_VALUE;
      if (hasDest) {
        int destIndex = -1;
        for (int j = 1; j < n; j++) {
          if (vehicleTravelTimes[i][j] != Integer.MAX_VALUE) {
            destIndex = j;
            break;
          }
        }
        checkArgument(destIndex >= 0);

        remove(routes, destIndex);
        routes.get(i).add(0, destIndex);
        if (servicePairMap.containsKey(destIndex)) {
          final int deliveryIndex = servicePairMap.get(destIndex);
          // if it has an associated delivery, move that as well
          remove(routes, deliveryIndex);
          routes.get(i).add(1, deliveryIndex);
        }

      }
    }

    final SolutionObject[] sols = new SolutionObject[v];
    for (int i = 0; i < v; i++) {
      routes.get(i).add(0, 0);
      routes.get(i).add(routes.get(i).size(), travelTime.length - 1);

      final int[] route = Ints.toArray(routes.get(i));
      final int[] arrivalTimes = new int[route.length];
      if (route.length > 0) {
        arrivalTimes[0] = remainingServiceTimes[i];
      }

      for (int j = 1; j < route.length; j++) {
        final int prev = route[j - 1];
        final int cur = route[j];

        // we compute the travel time. If it is the first step in the
        // route, we use the time from vehicle location to the next
        // location in the route.
        final int tt = j == 1 ? vehicleTravelTimes[i][cur]
            : travelTime[prev][cur];

        // we compute the first possible arrival time for the vehicle to
        // arrive at location i, given that it first visited location
        // i-1
        final int earliestArrivalTime = arrivalTimes[j - 1]
            + serviceTimes[prev] + tt;

        // we also have to take into account the time window
        final int minArrivalTime = Math
            .max(earliestArrivalTime, releaseDates[cur]);
        arrivalTimes[j] = minArrivalTime;
      }

      final int totalTravelTime = ArraysSolvers
          .computeTotalTravelTime(route, travelTime, vehicleTravelTimes[i]);
      final int tardiness = ArraysSolvers
          .computeSumTardiness(route, arrivalTimes, serviceTimes, dueDates);
      sols[i] = new SolutionObject(route, arrivalTimes, totalTravelTime
          + tardiness);
      // System.out.println(sols[i]);
    }

    return sols;
  }

  static void remove(List<List<Integer>> lists, int i) {
    for (final List<Integer> l : lists) {
      l.remove(new Integer(i));
    }
  }
}
