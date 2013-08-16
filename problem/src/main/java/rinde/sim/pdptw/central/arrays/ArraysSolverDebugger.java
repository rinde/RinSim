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

/**
 * A {@link SingleVehicleArraysSolver} wrapper that adds debugging.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ArraysSolverDebugger implements SingleVehicleArraysSolver {

    private final List<InputObject> inputMemory;
    private final List<SolutionObject> outputMemory;

    private final SingleVehicleArraysSolver solver;
    private final boolean print;

    private ArraysSolverDebugger(SingleVehicleArraysSolver solver, boolean print) {
        this.solver = solver;
        this.print = print;
        inputMemory = newArrayList();
        outputMemory = newArrayList();
    }

    @Override
    public SolutionObject solve(int[][] travelTime, int[] releaseDates,
            int[] dueDates, int[][] servicePairs, int[] serviceTimes) {

        inputMemory.add(new InputObject(travelTime, releaseDates, dueDates,
                servicePairs, serviceTimes));
        if (print) {
            out.println("int[][] travelTime = " + fix(deepToString(travelTime)));
            out.println("int[] releaseDates = "
                    + fix(Arrays.toString(releaseDates)));
            out.println("int[] dueDates = " + fix(Arrays.toString(dueDates)));
            out.println("int[][] servicePairs = "
                    + fix(deepToString(servicePairs)));
            out.println("int[] serviceTime = "
                    + fix(Arrays.toString(serviceTimes)));
        }

        final long start = System.currentTimeMillis();
        final SolutionObject sol =
                solver.solve(travelTime, releaseDates, dueDates, servicePairs,
                    serviceTimes);
        if (print) {
            out.println(System.currentTimeMillis() - start + "ms");
            out.println("route: " + Arrays.toString(sol.route));
            out.println("arrivalTimes: " + Arrays.toString(sol.arrivalTimes));
            out.println("objectiveValue: " + sol.objectiveValue);
        }

        outputMemory.add(new SolutionObject(
                copyOf(sol.route, sol.route.length), copyOf(sol.arrivalTimes,
                    sol.arrivalTimes.length), sol.objectiveValue));

        int totalTravelTime = 0;
        for (int i = 1; i < travelTime.length; i++) {
            totalTravelTime += travelTime[sol.route[i - 1]][sol.route[i]];
        }
        if (print) {
            out.println("travel time :  " + totalTravelTime);
        }

        // code for debugging arrival times
        // for (int i = 1; i < travelTime.length; i++) {
        // System.out.println(sol.route[i - 1] + " -> " + sol.route[i] + " = " +
        // sol.arrivalTimes[sol.route[i - 1]]
        // + " + " + travelTime[sol.route[i - 1]][sol.route[i]] + " + " + (i > 1
        // ? serviceTime : 0) + " = "
        // + " (" + sol.arrivalTimes[sol.route[i]] + ")");
        // }

        return sol;
    }

    /**
     * Clears the memory.
     */
    public void flush() {
        inputMemory.clear();
        outputMemory.clear();
    }

    /**
     * @return An unmodifiable list with an {@link InputObject} in invocation
     *         order for every invocation of
     *         {@link #solve(int[][], int[], int[], int[][], int[])}.
     */
    public List<InputObject> getInputMemory() {
        return Collections.unmodifiableList(inputMemory);
    }

    /**
     * @return An unmodifiable list with an {@link SolutionObject} in invocation
     *         order for every invocation of
     *         {@link #solve(int[][], int[], int[], int[][], int[])}.
     */
    public List<SolutionObject> getOutputMemory() {
        return Collections.unmodifiableList(outputMemory);
    }

    /**
     * Wraps the specified {@link SingleVehicleArraysSolver} to allow easy
     * debugging. Every invocation of
     * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
     * all inputs and outputs are printed to <code>System.out</code>, also all
     * inputs and outputs are stored (accessible via {@link #getInputMemory()}
     * and {@link #getOutputMemory()}.
     * @param s The {@link SingleVehicleArraysSolver} to wrap.
     * @return The wrapped solver.
     */
    public static ArraysSolverDebugger wrap(SingleVehicleArraysSolver s) {
        return new ArraysSolverDebugger(s, true);
    }

    /**
     * Wraps the specified {@link SingleVehicleArraysSolver} to allow easy
     * debugging. Stores all invocation arguments and outputs and optionally
     * prints them to <code>System.out</code>.
     * @param s The {@link SingleVehicleArraysSolver} to wrap.
     * @param print If <code>true</code> all information will be printed as
     *            well.
     * @return The wrapped solver.
     */
    public static ArraysSolverDebugger wrap(SingleVehicleArraysSolver s,
            boolean print) {
        return new ArraysSolverDebugger(s, print);
    }

    static String fix(String s) {
        return s.replace('[', '{').replace(']', '}') + ";";
    }

    /**
     * Object containing a copy of the call arguments of
     * {@link SingleVehicleArraysSolver#solve(int[][], int[], int[], int[][], int[])}
     * .
     * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
     */
    public static class InputObject {
        final int[][] travelTime;
        final int[] releaseDates;
        final int[] dueDates;
        final int[][] servicePairs;
        final int[] serviceTimes;

        InputObject(int[][] tt, int[] rd, int[] dd, int[][] sp, int[] st) {
            travelTime = copyOf(tt, tt.length);
            releaseDates = copyOf(rd, rd.length);
            dueDates = copyOf(dd, dd.length);
            servicePairs = copyOf(sp, sp.length);
            serviceTimes = copyOf(st, st.length);
        }
    }

}
