/**
 * 
 */
package rinde.sim.pdptw.central.arrays;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.System.out;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.deepToString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import rinde.sim.pdptw.central.arrays.ArraysSolvers.ArraysObject;
import rinde.sim.pdptw.central.arrays.ArraysSolvers.MVArraysObject;

/**
 * A {@link SingleVehicleArraysSolver} wrapper that adds debugging facilities. A
 * history is kept of all inputs and outputs and all inputs can optionally be
 * printed to sys.out.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class ArraysSolverDebugger {

  private ArraysSolverDebugger() {}

  /**
   * Wraps the specified {@link SingleVehicleArraysSolver} to allow easy
   * debugging. Every invocation of
   * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[], SolutionObject)}
   * all inputs and outputs are printed to <code>System.out</code>, also all
   * inputs and outputs are stored (accessible via
   * {@link SVASDebugger#getInputMemory()} and
   * {@link SVASDebugger#getOutputMemory()}.
   * @param s The {@link SingleVehicleArraysSolver} to wrap.
   * @return The wrapped solver.
   */
  public static SVASDebugger wrap(SingleVehicleArraysSolver s) {
    return new SVASDebugger(s, true);
  }

  /**
   * Wraps the specified {@link SingleVehicleArraysSolver} to allow easy
   * debugging. Stores all invocation arguments and outputs and optionally
   * prints them to <code>System.out</code>.
   * @param s The {@link SingleVehicleArraysSolver} to wrap.
   * @param print If <code>true</code> all information will be printed as well.
   * @return The wrapped solver.
   */
  public static SVASDebugger wrap(SingleVehicleArraysSolver s, boolean print) {
    return new SVASDebugger(s, print);
  }

  /**
   * Wraps the specified {@link MultiVehicleArraysSolver} to allow easy
   * debugging. Stores all invocation arguments and outputs and optionally
   * prints them to <code>System.out</code>.
   * @param s The {@link MultiVehicleArraysSolver} to wrap.
   * @param print If <code>true</code> all information will be printed as well.
   * @return The wrapped solver.
   */
  public static MVASDebugger wrap(MultiVehicleArraysSolver s, boolean print) {
    return new MVASDebugger(s, print);
  }

  static String fix(String s) {
    return s.replace('[', '{').replace(']', '}') + ";";
  }

  private static class Debugger<I extends ArraysObject, O> {
    protected final List<I> inputMemory;
    protected final List<O> outputMemory;
    protected final boolean print;

    Debugger(boolean print) {
      this.print = print;
      inputMemory = newArrayList();
      outputMemory = newArrayList();
    }

    /**
     * Clears the memory.
     */
    public void flush() {
      inputMemory.clear();
      outputMemory.clear();
    }

    /**
     * @return An unmodifiable list with an {@link ArraysObject} in invocation
     *         order for every invocation of <code>solve(..)</code>.
     */
    public List<I> getInputMemory() {
      return Collections.unmodifiableList(inputMemory);
    }

    /**
     * @return An unmodifiable list with an {@link SolutionObject} in invocation
     *         order for every invocation of <code>solve(..)</code>.
     */
    public List<O> getOutputMemory() {
      return Collections.unmodifiableList(outputMemory);
    }
  }

  /**
   * Debugger for {@link SingleVehicleArraysSolver}s.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static final class SVASDebugger extends
      Debugger<ArraysObject, SolutionObject> implements
      SingleVehicleArraysSolver {
    private final SingleVehicleArraysSolver solver;

    SVASDebugger(SingleVehicleArraysSolver solver, boolean print) {
      super(print);
      this.solver = solver;
    }

    @Override
    public SolutionObject solve(int[][] travelTime, int[] releaseDates,
        int[] dueDates, int[][] servicePairs, int[] serviceTimes,
        @Nullable SolutionObject currentSolution) {

      inputMemory.add(new ArraysObject(travelTime, releaseDates, dueDates,
          servicePairs, serviceTimes, currentSolution == null ? null
              : new SolutionObject[] { currentSolution }));
      if (print) {
        out.println("int[][] travelTime = " + fix(deepToString(travelTime)));
        out.println("int[] releaseDates = "
            + fix(Arrays.toString(releaseDates)));
        out.println("int[] dueDates = " + fix(Arrays.toString(dueDates)));
        out.println("int[][] servicePairs = " + fix(deepToString(servicePairs)));
        out.println("int[] serviceTime = " + fix(Arrays.toString(serviceTimes)));
      }

      final long start = System.currentTimeMillis();
      final SolutionObject sol = solver.solve(travelTime, releaseDates,
          dueDates, servicePairs, serviceTimes, currentSolution);
      if (print) {
        out.println(System.currentTimeMillis() - start + "ms");
        out.println("route: " + Arrays.toString(sol.route));
        out.println("arrivalTimes: " + Arrays.toString(sol.arrivalTimes));
        out.println("objectiveValue: " + sol.objectiveValue);
      }

      outputMemory
          .add(new SolutionObject(copyOf(sol.route, sol.route.length), copyOf(
              sol.arrivalTimes, sol.arrivalTimes.length), sol.objectiveValue));

      int totalTravelTime = 0;
      for (int i = 1; i < travelTime.length; i++) {
        totalTravelTime += travelTime[sol.route[i - 1]][sol.route[i]];
      }
      if (print) {
        out.println("travel time :  " + totalTravelTime);
      }
      return sol;
    }
  }

  /**
   * Debugger for {@link MultiVehicleArraysSolver}s.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static final class MVASDebugger extends
      Debugger<MVArraysObject, SolutionObject[]> implements
      MultiVehicleArraysSolver {

    private final MultiVehicleArraysSolver solver;

    private MVASDebugger(MultiVehicleArraysSolver solver, boolean print) {
      super(print);
      this.solver = solver;
    }

    @Override
    public SolutionObject[] solve(int[][] travelTime, int[] releaseDates,
        int[] dueDates, int[][] servicePairs, int[] serviceTimes,
        int[][] vehicleTravelTimes, int[][] inventories,
        int[] remainingServiceTimes, int[] currentDestinations,
        @Nullable SolutionObject[] currentSolutions) {

      inputMemory.add(new MVArraysObject(travelTime, releaseDates, dueDates,
          servicePairs, serviceTimes, vehicleTravelTimes, inventories,
          remainingServiceTimes, currentDestinations, currentSolutions));

      final SolutionObject[] output = solver.solve(travelTime, releaseDates,
          dueDates, servicePairs, serviceTimes, vehicleTravelTimes,
          inventories, remainingServiceTimes, currentDestinations,
          currentSolutions);
      outputMemory.add(output);
      return output;
    }
  }
}
