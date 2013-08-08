/**
 * 
 */
package rinde.sim.central;

import rinde.sim.core.graph.Point;
import rinde.sim.problem.common.DefaultParcel;

import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class GlobalStateObject {

    // parcels on map
    public final ImmutableSet<DefaultParcel> availableParcels;
    public final ImmutableSet<VehicleState> vehicles;

    public GlobalStateObject(ImmutableSet<DefaultParcel> availableParcels,
            ImmutableSet<VehicleState> vehicles) {
        this.availableParcels = availableParcels;
        this.vehicles = vehicles;
    }

    public static class VehicleState {
        public final Point location;
        public final ImmutableSet<DefaultParcel> contents;

        public VehicleState(Point location, ImmutableSet<DefaultParcel> contents) {
            this.location = location;
            this.contents = ImmutableSet.copyOf(contents);
        }
    }

}
