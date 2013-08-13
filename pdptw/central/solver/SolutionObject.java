package rinde.sim.pdptw.central.solver;

import java.util.Arrays;

/**
 * Solution object for single vehicle pickup-and-delivery problem with time
 * windows.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class SolutionObject {

    /**
     * Array of locations which have to be serviced in the specified sequence,
     * starts with the begin location and ends with depot. Should have the same
     * length as {@link #arrivalTimes}.
     */
    public final int[] route;

    /**
     * Array of times at which every location servicing starts. The number at
     * <code>arrivalTimes[i]</code> indicates the arrival time at location
     * <code>route[i]</code>. Always starts with the specified remaining service
     * time for this vehicle. Should have the same length as {@link #route}.
     */
    public final int[] arrivalTimes;

    /**
     * The objective value for this solution as computed by the solver.
     */
    public final int objectiveValue;

    /**
     * 
     * @param route {@link #route}
     * @param arrivalTimes {@link #arrivalTimes}
     * @param objectiveValue {@link #objectiveValue}
     */
    public SolutionObject(int[] route, int[] arrivalTimes, int objectiveValue) {
        this.route = route;
        this.arrivalTimes = arrivalTimes;
        this.objectiveValue = objectiveValue;
    }

    @Override
    public String toString() {
        return new StringBuilder("Route: ").append(Arrays.toString(route))
                .append("\n").append("Arrival times: ")
                .append(Arrays.toString(arrivalTimes)).append("\n")
                .append("Objective: ").append(objectiveValue).toString();
    }

}
