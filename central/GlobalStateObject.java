/**
 * 
 */
package rinde.sim.central;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

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

    public final Unit<Duration> timeUnit;
    public final Unit<Velocity> speedUnit;
    public final Unit<Length> distUnit;

    GlobalStateObject(ImmutableSet<DefaultParcel> availableParcels,
            ImmutableSet<VehicleState> vehicles, long time,
            Unit<Duration> timeUnit, Unit<Velocity> speedUnit,
            Unit<Length> distUnit) {
        this.availableParcels = availableParcels;
        this.vehicles = vehicles;
        this.time = time;
        this.timeUnit = timeUnit;
        this.speedUnit = speedUnit;
        this.distUnit = distUnit;
    }

    public static class VehicleState {
        public final DefaultVehicle vehicle;
        public final Point location;
        public final ImmutableSet<DefaultParcel> contents;
        public final long remainingServiceTime;

        VehicleState(DefaultVehicle vehicle, Point location,
                ImmutableSet<DefaultParcel> contents, long remainingServiceTime) {
            this.vehicle = vehicle;
            this.location = location;
            this.contents = contents;
            this.remainingServiceTime = remainingServiceTime;
        }
    }

}
