/**
 * 
 */
package rinde.sim.pdptw.central.arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.primitives.Ints;

/**
 * This is a random implementation of {@link MultiVehicleArraysSolver} used for
 * testing. Note that it is not perfectly random, it is a 'lazy' implementation
 * that creates feasible solutions but not necessary with an equal or non-zero
 * probability.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
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
      int[] remainingServiceTimes, int[] currentDestinations,
      @Nullable SolutionObject[] currentSolutions) {

    final int n = travelTime.length;
    final int v = vehicleTravelTimes.length;

    final List<List<Integer>> routes = newArrayList();
    for (int i = 0; i < v; i++) {
      routes.add(new ArrayList<Integer>());
      if (currentDestinations[i] > 0) {
        routes.get(i).add(currentDestinations[i]);
      }
    }
    final Set<Integer> curDestSet = newLinkedHashSet(Ints
        .asList(currentDestinations));

    for (int i = 0; i < inventories.length; i++) {
      if (!curDestSet.contains(inventories[i][1])) {
        routes.get(inventories[i][0]).add(inventories[i][1]);
      }
    }

    for (int i = 0; i < servicePairs.length; i++) {
      if (!curDestSet.contains(servicePairs[i][0])) {
        final List<Integer> route = routes.get(rng.nextInt(v));
        route.add(servicePairs[i][0]);
        route.add(servicePairs[i][1]);
      }
    }

    final Map<Integer, Integer> servicePairMap = newHashMap();
    for (int i = 0; i < servicePairs.length; i++) {
      servicePairMap.put(servicePairs[i][0], servicePairs[i][1]);
    }

    for (int i = 0; i < v; i++) {
      final boolean hasDest = currentDestinations[i] > 0;
      if (hasDest) {
        final int destIndex = currentDestinations[i];
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
      routes.get(i).add(routes.get(i).size(), n - 1);
      final int[] route = Ints.toArray(routes.get(i));
      final int[] arrivalTimes = ArraysSolvers.computeArrivalTimes(route,
          travelTime, remainingServiceTimes[i], vehicleTravelTimes[i],
          serviceTimes, releaseDates);
      final int totalTravelTime = ArraysSolvers.computeTotalTravelTime(route,
          travelTime, vehicleTravelTimes[i]);
      final int tardiness = ArraysSolvers.computeSumTardiness(route,
          arrivalTimes, serviceTimes, dueDates);
      sols[i] = new SolutionObject(route, arrivalTimes, totalTravelTime
          + tardiness);
    }
    return sols;
  }

  static void remove(List<List<Integer>> lists, int i) {
    for (final List<Integer> l : lists) {
      l.remove(new Integer(i));
    }
  }
}
