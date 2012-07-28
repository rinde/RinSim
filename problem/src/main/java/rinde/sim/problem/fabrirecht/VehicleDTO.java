/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import rinde.sim.core.graph.Point;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class VehicleDTO {

	public final Point startPosition;
	public final double speed;
	public final int capacity;
	public final TimeWindow availabilityTimeWindow;

	public VehicleDTO(Point pStartPosition, double pSpeed, int pCapacity, TimeWindow pAvailabilityTimeWindow) {
		startPosition = pStartPosition;
		speed = pSpeed;
		capacity = pCapacity;
		availabilityTimeWindow = pAvailabilityTimeWindow;
	}

}
