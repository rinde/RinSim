/**
 * 
 */
package rinde.sim.pdptw.central;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.central.GlobalStateObject;
import rinde.sim.pdptw.central.GlobalStateObject.VehicleState;
import rinde.sim.problem.common.ParcelDTO;
import rinde.sim.problem.common.VehicleDTO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ConverterTest {

    public static GlobalStateObject createGlobalStateObject(
            ImmutableSet<ParcelDTO> availableParcels,
            ImmutableList<VehicleState> vehicles, long time,
            Unit<Duration> timeUnit, Unit<Velocity> speedUnit,
            Unit<Length> distUnit) {
        return new GlobalStateObject(availableParcels, vehicles, time,
                timeUnit, speedUnit, distUnit);
    }

    public static VehicleState createVehicleState(VehicleDTO dto,
            Point location, ImmutableSet<ParcelDTO> contents,
            long remainingServiceTime) {
        return new VehicleState(dto, location, contents, remainingServiceTime);
    }
}
