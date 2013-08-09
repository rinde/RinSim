/**
 * 
 */
package rinde.sim.central;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import rinde.sim.central.GlobalStateObject.VehicleState;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.problem.common.DefaultParcel;
import rinde.sim.problem.common.DefaultVehicle;

import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public final class Converter {

    private Converter() {}

    public static GlobalStateObject convert(RoadModel rm, PDPModel pm,
            DefaultVehicle vehicle, Collection<DefaultParcel> availableParcels,
            long time, Unit<Duration> timeUnit, Unit<Velocity> speedUnit,
            Unit<Length> distUnit) {

        // this is ok since we actually check the type
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Collection<DefaultParcel> ps = Collections
                .checkedCollection((Collection) pm.getContents(vehicle), DefaultParcel.class);
        final long remainingServiceTime = pm.getVehicleState(vehicle) == PDPModel.VehicleState.IDLE ? 0
                : pm.getVehicleActionInfo(vehicle).timeNeeded();
        final ImmutableSet<VehicleState> vehicles = ImmutableSet
                .of(new VehicleState(vehicle, rm.getPosition(vehicle),
                        ImmutableSet.copyOf(ps), remainingServiceTime));

        return new GlobalStateObject(ImmutableSet.copyOf(availableParcels),
                vehicles, time, timeUnit, speedUnit, distUnit);
    }

    public static GlobalStateObject convert(RoadModel rm, PDPModel pm,
            long time, Unit<Duration> timeUnit, Unit<Velocity> speedUnit,
            Unit<Length> distUnit) {
        final ImmutableSet<DefaultParcel> parcels = ImmutableSet.copyOf(rm
                .getObjectsOfType(DefaultParcel.class));
        final Set<DefaultVehicle> vehicles = rm
                .getObjectsOfType(DefaultVehicle.class);
        final ImmutableSet.Builder<VehicleState> vbuilder = ImmutableSet
                .builder();
        for (final DefaultVehicle v : vehicles) {
            final long remainingServiceTime = pm.getVehicleState(v) == PDPModel.VehicleState.IDLE ? 0
                    : pm.getVehicleActionInfo(v).timeNeeded();

            // this is ok since we actually check the type
            @SuppressWarnings({ "unchecked", "rawtypes" })
            final Collection<DefaultParcel> ps = Collections
                    .checkedCollection((Collection) pm.getContents(v), DefaultParcel.class);
            vbuilder.add(new VehicleState(v, rm.getPosition(v), ImmutableSet
                    .copyOf(ps), remainingServiceTime));
        }
        return new GlobalStateObject(parcels, vbuilder.build(), time, timeUnit,
                speedUnit, distUnit);
    }

}
