/**
 * 
 */
package rinde.sim.pdptw.central;

import static com.google.common.collect.Lists.newLinkedList;
import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.pdptw.central.GlobalStateObject.VehicleState;
import rinde.sim.problem.common.DefaultParcel;
import rinde.sim.problem.common.DefaultVehicle;
import rinde.sim.problem.common.ParcelDTO;
import rinde.sim.problem.common.PDPRoadModel;
import rinde.sim.problem.common.VehicleDTO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public final class Solvers {

    private Solvers() {}

    public static class StateContext {
        public final GlobalStateObject state;
        public final ImmutableMap<VehicleDTO, DefaultVehicle> vehicleMap;
        public final ImmutableMap<ParcelDTO, DefaultParcel> parcelMap;

        StateContext(GlobalStateObject state,
                ImmutableMap<VehicleDTO, DefaultVehicle> vehicleMap,
                ImmutableMap<ParcelDTO, DefaultParcel> parcelMap) {
            this.state = state;
            this.vehicleMap = vehicleMap;
            this.parcelMap = parcelMap;
        }
    }

    // solver for multi vehicle
    public static List<Queue<DefaultParcel>> solve(Solver solver,
            PDPRoadModel rm, PDPModel pm, long time,
            Unit<Duration> timeUnit, Unit<Velocity> speedUnit,
            Unit<Length> distUnit) {
        final StateContext state =
                convert(rm, pm, time, timeUnit, speedUnit, distUnit);
        return convertRoutes(state, solver.solve(state.state));
    }

    // solver for single vehicle
    public static Queue<DefaultParcel> solve(Solver solver, PDPRoadModel rm,
            PDPModel pm, DefaultVehicle vehicle,
            Collection<DefaultParcel> availableParcels, long time,
            Unit<Duration> timeUnit, Unit<Velocity> speedUnit,
            Unit<Length> distUnit) {
        final StateContext state =
                convert(rm, pm, vehicle, availableParcels, time, timeUnit,
                    speedUnit, distUnit);
        return convertRoutes(state, solver.solve(state.state)).get(0);
    }

    // converts the routes received from Solver.solve(..) into a format which is
    // expected by the simulator
    public static List<Queue<DefaultParcel>> convertRoutes(StateContext cont,
            List<? extends List<ParcelDTO>> routes) {
        final ImmutableList.Builder<Queue<DefaultParcel>> routesBuilder =
                ImmutableList.builder();
        for (final List<ParcelDTO> route : routes) {
            final Queue<DefaultParcel> newRoute = newLinkedList();
            for (final ParcelDTO dto : route) {
                newRoute.add(cont.parcelMap.get(dto));
            }
            routesBuilder.add(newRoute);
        }
        return routesBuilder.build();
    }

    public static StateContext convert(PDPRoadModel rm, PDPModel pm,
            DefaultVehicle vehicle, Collection<DefaultParcel> availableParcels,
            long time, Unit<Duration> timeUnit, Unit<Velocity> speedUnit,
            Unit<Length> distUnit) {
        return convert(rm, pm, asList(vehicle), availableParcels, time,
            timeUnit, speedUnit, distUnit);
    }

    public static StateContext convert(PDPRoadModel rm, PDPModel pm,
            long time, Unit<Duration> timeUnit, Unit<Velocity> speedUnit,
            Unit<Length> distUnit) {
        return convert(rm, pm, rm.getObjectsOfType(DefaultVehicle.class),
            rm.getObjectsOfType(DefaultParcel.class), time, timeUnit,
            speedUnit, distUnit);
    }

    static StateContext convert(PDPRoadModel rm, PDPModel pm,
            Collection<DefaultVehicle> vehicles,
            Collection<DefaultParcel> availableParcels, long time,
            Unit<Duration> timeUnit, Unit<Velocity> speedUnit,
            Unit<Length> distUnit) {

        final ImmutableMap<ParcelDTO, DefaultParcel> parcelMap =
                toMap(availableParcels);
        final ImmutableMap<VehicleDTO, DefaultVehicle> vehicleMap =
                toVehicleMap(vehicles);
        final ImmutableList.Builder<VehicleState> vbuilder =
                ImmutableList.builder();
        final ImmutableMap.Builder<ParcelDTO, DefaultParcel> allParcels =
                ImmutableMap.builder();
        allParcels.putAll(parcelMap);
        for (final DefaultVehicle v : vehicles) {
            final ImmutableMap<ParcelDTO, DefaultParcel> contentsMap =
                    contentsToMap(pm, v);
            vbuilder.add(convertToVehicleState(rm, pm, v, contentsMap));
            allParcels.putAll(contentsMap);
        }
        return new StateContext(new GlobalStateObject(parcelMap.keySet(),
                vbuilder.build(), time, timeUnit, speedUnit, distUnit),//
                vehicleMap, allParcels.build());
    }

    static ImmutableMap<ParcelDTO, DefaultParcel> contentsToMap(PDPModel pm,
            DefaultVehicle vehicle) {
        // this is ok since we actually check the type
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Collection<DefaultParcel> ps =
                Collections.checkedCollection(
                    (Collection) pm.getContents(vehicle), DefaultParcel.class);
        return toMap(ps);
    }

    static VehicleState convertToVehicleState(PDPRoadModel rm, PDPModel pm,
            DefaultVehicle vehicle,
            ImmutableMap<ParcelDTO, DefaultParcel> contents) {
        final long remainingServiceTime =
                pm.getVehicleState(vehicle) == PDPModel.VehicleState.IDLE ? 0
                        : pm.getVehicleActionInfo(vehicle).timeNeeded();

        ParcelDTO destination = null;
        if (!rm.isVehicleDiversionAllowed()) {
            // check whether the vehicle is already underway to parcel
            final DefaultParcel p = rm.getDestinationToParcel(vehicle);
            if (p != null) {
                destination = p.dto;
            }
        }
        return new VehicleState(vehicle.getDTO(), rm.getPosition(vehicle),
                contents.keySet(), remainingServiceTime, destination);
    }

    static ImmutableMap<ParcelDTO, DefaultParcel> toMap(
            Collection<DefaultParcel> parcels) {
        final ImmutableMap.Builder<ParcelDTO, DefaultParcel> parcelMapBuilder =
                ImmutableMap.builder();
        for (final DefaultParcel dp : parcels) {
            parcelMapBuilder.put(dp.dto, dp);
        }
        return parcelMapBuilder.build();
    }

    static ImmutableMap<VehicleDTO, DefaultVehicle> toVehicleMap(
            Collection<DefaultVehicle> vehicles) {
        final ImmutableMap.Builder<VehicleDTO, DefaultVehicle> vehicleMapBuilder =
                ImmutableMap.builder();
        for (final DefaultVehicle dp : vehicles) {
            vehicleMapBuilder.put(dp.getDTO(), dp);
        }
        return vehicleMapBuilder.build();
    }
}
