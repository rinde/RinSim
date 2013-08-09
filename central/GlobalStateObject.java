/**
 * 
 */
package rinde.sim.central;

import rinde.sim.core.graph.Point;
import rinde.sim.problem.common.DefaultParcel;
import rinde.sim.problem.common.DefaultVehicle;

import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class GlobalStateObject {

    // parcels on map
    public final ImmutableSet<DefaultParcel> availableParcels;
    public final ImmutableSet<VehicleState> vehicles;
    public final long time;

    public GlobalStateObject(ImmutableSet<DefaultParcel> availableParcels,
            ImmutableSet<VehicleState> vehicles, long time) {
        this.availableParcels = availableParcels;
        this.vehicles = vehicles;
        this.time = time;
    }

    public static class VehicleState {
        public final DefaultVehicle vehicle;
        public final Point location;
        public final ImmutableSet<DefaultParcel> contents;
        public final long remainingServiceTime;

        public VehicleState(DefaultVehicle vehicle, Point location,
                ImmutableSet<DefaultParcel> contents, long remainingServiceTime) {
            this.vehicle = vehicle;
            this.location = location;
            this.contents = ImmutableSet.copyOf(contents);
            this.remainingServiceTime = remainingServiceTime;
        }
    }

}
