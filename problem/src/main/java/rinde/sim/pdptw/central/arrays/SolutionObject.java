package rinde.sim.pdptw.central.arrays;

import java.util.Arrays;

/**
 * Solution object for single vehicle pickup-and-delivery problem with time
 * windows.
 * 
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
	 * Array of times at which every location servicing <i>starts</i>. The
	 * number at <code>arrivalTimes[i]</code> indicates the arrival time at
	 * location <code>route[i]</code>. Index 0 always contains the specified
	 * remaining service time for this vehicle. Should have the same length as
	 * {@link #route}.
	 */
	public final int[] arrivalTimes;

	/**
	 * The objective value for this solution as computed by the solver.
	 */
	public final int objectiveValue;

	/**
	 * 
	 * @param route
	 *            {@link #route}
	 * @param arrivalTimes
	 *            {@link #arrivalTimes}
	 * @param objectiveValue
	 *            {@link #objectiveValue}
	 */
	public SolutionObject(int[] route, int[] arrivalTimes, int objectiveValue) {
		this.route = Arrays.copyOf(route, route.length);
		this.arrivalTimes = Arrays.copyOf(arrivalTimes, arrivalTimes.length);
		this.objectiveValue = objectiveValue;
	}

	@Override
	public String toString() {
		return new StringBuilder("Route: ").append(Arrays.toString(route))
				.append("\n").append("Arrival times: ")
				.append(Arrays.toString(arrivalTimes))
				.append(System.getProperty("line.separator"))
				.append("Objective: ").append(objectiveValue).toString();
	}

}
