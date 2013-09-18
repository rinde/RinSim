/**
 * 
 */
package rinde.sim.pdptw.central;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;

import rinde.sim.core.Simulator;
import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.PDPModel.VehicleParcelActionInfo;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.pdptw.central.GlobalStateObject.VehicleStateObject;
import rinde.sim.pdptw.common.DefaultParcel;
import rinde.sim.pdptw.common.DefaultVehicle;
import rinde.sim.pdptw.common.PDPRoadModel;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.common.VehicleDTO;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public final class Solvers {

  // FIXME write tests for the conversion methods!

  // TODO this class' methods requires explicit units, this is no longer
  // necessary since they can be retrieved from the models

  // TODO builder?
  private Solvers() {}

  public static SolverHandle solver(Solver sol, Simulator sim) {
    return new SolverHandle(sol, sim.getModelProvider(), sim);
  }

  public static SolverHandle solver(Solver sol, ModelProvider mp,
      SimulatorAPI simulator) {
    return new SolverHandle(sol, mp, simulator);
  }

  static class SolverHandle {
    private final Solver solver;
    private final SimulatorAPI simulator;
    private final PDPRoadModel rm;
    private final PDPModel pm;

    SolverHandle(Solver s, ModelProvider mp, SimulatorAPI sim) {
      solver = s;
      simulator = sim;
      final PDPRoadModel r = mp.getModel(PDPRoadModel.class);
      checkArgument(r != null);
      rm = r;
      final PDPModel p = mp.getModel(PDPModel.class);
      checkArgument(p != null);
      pm = p;
    }

    public List<Queue<DefaultParcel>> solve() {
      return solve(null);
    }

    public List<Queue<DefaultParcel>> solve(
        @Nullable ImmutableList<ImmutableList<DefaultParcel>> currentRoutes) {
      final StateContext state = convert(currentRoutes);
      return Solvers.convertRoutes(state, solver.solve(state.state));
    }

    StateContext convert(
        @Nullable ImmutableList<ImmutableList<DefaultParcel>> currentRoutes) {
      return Solvers.convert(rm, pm, rm.getObjectsOfType(DefaultVehicle.class),
          conv(pm.getParcels(ParcelState.ANNOUNCED, ParcelState.AVAILABLE,
              ParcelState.PICKING_UP)), Measure.valueOf(
              simulator.getCurrentTime(), simulator.getTimeUnit()),
          currentRoutes);
    }

  }

  // solver for single vehicle
  public static Queue<DefaultParcel> solveSingleVehicle(Solver solver,
      PDPRoadModel rm, PDPModel pm, DefaultVehicle vehicle,
      Collection<DefaultParcel> availableParcels, Measure<Long, Duration> time) {
    return solveSingleVehicle(solver, rm, pm, vehicle, availableParcels, time,
        null);
  }

  public static Queue<DefaultParcel> solveSingleVehicle(Solver solver,
      PDPRoadModel rm, PDPModel pm, DefaultVehicle vehicle,
      Collection<DefaultParcel> availableParcels, Measure<Long, Duration> time,
      @Nullable ImmutableList<DefaultParcel> currentRoute) {
    final StateContext state = convert(rm, pm, vehicle, availableParcels, time,
        currentRoute);
    return convertRoutes(state, solver.solve(state.state)).get(0);
  }

  // converts the routes received from Solver.solve(..) into a format which is
  // expected by the simulator
  static List<Queue<DefaultParcel>> convertRoutes(StateContext cont,
      List<? extends List<ParcelDTO>> routes) {
    final ImmutableList.Builder<Queue<DefaultParcel>> routesBuilder = ImmutableList
        .builder();
    for (final List<ParcelDTO> route : routes) {
      final Queue<DefaultParcel> newRoute = newLinkedList();
      for (final ParcelDTO dto : route) {
        newRoute.add(cont.parcelMap.get(dto));
      }
      routesBuilder.add(newRoute);
    }
    return routesBuilder.build();
  }

  // single vehicle
  public static StateContext convert(PDPRoadModel rm, PDPModel pm,
      DefaultVehicle vehicle, Collection<DefaultParcel> availableParcels,
      Measure<Long, Duration> time,
      @Nullable ImmutableList<DefaultParcel> currentRoute) {
    return convert(rm, pm, asList(vehicle), availableParcels, time,
        currentRoute == null ? null : ImmutableList.of(currentRoute));
  }

  // public static StateContext convert(PDPRoadModel rm, PDPModel pm,
  // Measure<Long, Duration> time,
  // @Nullable ImmutableList<ImmutableList<DefaultParcel>> currentRoutes) {
  // return convert(rm, pm, rm.getObjectsOfType(DefaultVehicle.class),
  // conv(pm.getParcels(ParcelState.ANNOUNCED, ParcelState.AVAILABLE,
  // ParcelState.PICKING_UP)), time, currentRoutes);
  // }

  static Collection<DefaultParcel> conv(Collection<Parcel> input) {
    final List<DefaultParcel> l = newArrayList();
    for (final Parcel p : input) {
      checkArgument(p instanceof DefaultParcel);
      l.add((DefaultParcel) p);
    }
    return l;
  }

  static StateContext convert(PDPRoadModel rm, PDPModel pm,
      Collection<DefaultVehicle> vehicles,
      Collection<DefaultParcel> availableParcels, Measure<Long, Duration> time,
      @Nullable ImmutableList<ImmutableList<DefaultParcel>> currentRoutes) {

    final ImmutableMap<ParcelDTO, DefaultParcel> parcelMap = toMap(availableParcels);
    final ImmutableMap<VehicleDTO, DefaultVehicle> vehicleMap = toVehicleMap(vehicles);
    final ImmutableList.Builder<VehicleStateObject> vbuilder = ImmutableList
        .builder();
    final ImmutableMap.Builder<ParcelDTO, DefaultParcel> allParcels = ImmutableMap
        .builder();
    allParcels.putAll(parcelMap);

    @Nullable
    Iterator<ImmutableList<DefaultParcel>> routeIterator = null;
    if (currentRoutes != null) {
      checkArgument(currentRoutes.size() == vehicles.size(),
          "The number of routes (%s) must equal the number of vehicles (%s).",
          currentRoutes.size(), vehicles.size());
      routeIterator = currentRoutes.iterator();
    }

    for (final DefaultVehicle v : vehicles) {
      final ImmutableMap<ParcelDTO, DefaultParcel> contentsMap = contentsToMap(
          pm, v);

      @Nullable
      ImmutableList<DefaultParcel> route = null;
      if (routeIterator != null) {
        route = routeIterator.next();
      }
      vbuilder.add(convertToVehicleState(rm, pm, v, contentsMap, route));
      allParcels.putAll(contentsMap);
    }
    return new StateContext(new GlobalStateObject(parcelMap.keySet(),
        vbuilder.build(), time.getValue().longValue(), time.getUnit(),
        rm.getSpeedUnit(), rm.getDistanceUnit()), vehicleMap,
        allParcels.build());
  }

  static ImmutableMap<ParcelDTO, DefaultParcel> contentsToMap(PDPModel pm,
      DefaultVehicle vehicle) {
    // this is ok since we actually check the type
    @SuppressWarnings({ "unchecked", "rawtypes" })
    final Set<DefaultParcel> ps = Collections.checkedSet(
        (Set) newLinkedHashSet(pm.getContents(vehicle)), DefaultParcel.class);
    return toMap(ps);
  }

  // TODO check for bugs
  static VehicleStateObject convertToVehicleState(PDPRoadModel rm, PDPModel pm,
      DefaultVehicle vehicle, ImmutableMap<ParcelDTO, DefaultParcel> contents,
      @Nullable ImmutableList<DefaultParcel> route) {
    final boolean isIdle = pm.getVehicleState(vehicle) == PDPModel.VehicleState.IDLE;

    long remainingServiceTime = 0;
    @Nullable
    ParcelDTO destination = null;
    if (!isIdle) {
      final VehicleParcelActionInfo vpai = pm.getVehicleActionInfo(vehicle);
      destination = ((DefaultParcel) vpai.getParcel()).dto;
      remainingServiceTime = vpai.timeNeeded();
    } else if (!rm.isVehicleDiversionAllowed()) {
      // check whether the vehicle is already underway to parcel
      final DefaultParcel p = rm.getDestinationToParcel(vehicle);
      if (p != null) {
        destination = p.dto;
      }
    }

    @Nullable
    ImmutableList<ParcelDTO> r = null;
    if (route != null) {
      r = toDtoList(route, pm);
    }
    return new VehicleStateObject(vehicle.getDTO(), rm.getPosition(vehicle),
        contents.keySet(), remainingServiceTime, destination, r);
  }

  static ImmutableList<ParcelDTO> toDtoList(Collection<DefaultParcel> parcels,
      PDPModel pm) {
    final ImmutableList.Builder<ParcelDTO> builder = ImmutableList.builder();
    for (final DefaultParcel dp : parcels) {
      builder.add(dp.dto);
    }
    return builder.build();
  }

  static ImmutableMap<ParcelDTO, DefaultParcel> toMap(
      Collection<DefaultParcel> parcels) {
    final ImmutableMap.Builder<ParcelDTO, DefaultParcel> parcelMapBuilder = ImmutableMap
        .builder();
    for (final DefaultParcel dp : parcels) {
      parcelMapBuilder.put(dp.dto, dp);
    }
    return parcelMapBuilder.build();
  }

  static ImmutableMap<VehicleDTO, DefaultVehicle> toVehicleMap(
      Collection<DefaultVehicle> vehicles) {
    final ImmutableMap.Builder<VehicleDTO, DefaultVehicle> vehicleMapBuilder = ImmutableMap
        .builder();
    for (final DefaultVehicle dp : vehicles) {
      vehicleMapBuilder.put(dp.getDTO(), dp);
    }
    return vehicleMapBuilder.build();
  }

  /**
   * Value object containing information a {@link GlobalStateObject} and two
   * maps with references to the original vehicles and parcels.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
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
}
