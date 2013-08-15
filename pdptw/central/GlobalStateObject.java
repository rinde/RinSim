/**
 * 
 */
package rinde.sim.pdptw.central;

import javax.annotation.Nullable;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Point;
import rinde.sim.problem.common.DynamicPDPTWProblem;
import rinde.sim.problem.common.ParcelDTO;
import rinde.sim.problem.common.VehicleDTO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * An immutable state object representing the state of an entire
 * {@link Simulator} configured using {@link DynamicPDPTWProblem}. Instances can
 * be obtained via
 * {@link Solvers#convert(rinde.sim.core.model.road.RoadModel, rinde.sim.core.model.pdp.PDPModel, long, Unit, Unit, Unit)}
 * .
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class GlobalStateObject {

    // TODO add generic way for storing travel distances based on shortest path
    // in road model

    /**
     * All known parcels which require both a pickup and a delivery. They are
     * not in the inventory of a vehicle.
     */
    public final ImmutableSet<ParcelDTO> availableParcels;

    /**
     * All vehicles.
     */
    public final ImmutableList<VehicleState> vehicles;

    /**
     * The current time.
     */
    public final long time;

    /**
     * The unit of time.
     */
    public final Unit<Duration> timeUnit;

    /**
     * The unit of (vehicle) speed.
     */
    public final Unit<Velocity> speedUnit;

    /**
     * The unit of distances.
     */
    public final Unit<Length> distUnit;

    GlobalStateObject(ImmutableSet<ParcelDTO> availableParcels,
            ImmutableList<VehicleState> vehicles, long time,
            Unit<Duration> timeUnit, Unit<Velocity> speedUnit,
            Unit<Length> distUnit) {
        this.availableParcels = availableParcels;
        this.vehicles = vehicles;
        this.time = time;
        this.timeUnit = timeUnit;
        this.speedUnit = speedUnit;
        this.distUnit = distUnit;
    }

    /**
     * Immutable state object of a vehicle.
     * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
     */
    public static class VehicleState extends VehicleDTO {
        private static final long serialVersionUID = -9021013328998791086L;

        /**
         * Location of the vehicle.
         */
        public final Point location;

        /**
         * The contents of the vehicle.
         */
        public final ImmutableSet<ParcelDTO> contents;

        /**
         * The remaining time the vehicle needs for completion of its current
         * task.
         */
        public final long remainingServiceTime;

        /**
         * This field is only not <code>null</code> in case all of the following
         * holds:
         * <ul>
         * <li>Vehicles are not allowed to divert from their previously started
         * routes.</li>
         * <li>The vehicle is moving to a parcel (either pickup or delivery
         * location).</li>
         * <li>The vehicle has not yet reached its destination.</li>
         * </ul>
         * When it is not <code>null</code> it indicates the current destination
         * of a vehicle. When a vehicle has a destination it <b>must</b> first
         * move to and service this destination.
         */
        @Nullable
        public final ParcelDTO destination;

        VehicleState(VehicleDTO dto, Point location,
                ImmutableSet<ParcelDTO> contents, long remainingServiceTime,
                @Nullable ParcelDTO destination) {
            super(dto.startPosition, dto.speed, dto.capacity,
                    dto.availabilityTimeWindow);
            this.location = location;
            this.contents = contents;
            this.remainingServiceTime = remainingServiceTime;
            this.destination = destination;
        }
    }
}
