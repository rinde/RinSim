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

  private Solvers() {}

  /**
   * Create a {@link MVSolverHandle} which can be used to repeatedly solve multi
   * vehicle routing problem instances using the same {@link Solver} and
   * {@link Simulator} instance.
   * @param sol The {@link Solver} to use.
   * @param sim The {@link Simulator} to use.
   * @return The handle.
   */
  public static MVSolverHandle solver(Solver sol, Simulator sim) {
    return new SolverHandle(sol, sim.getModelProvider(), sim);
  }

  /**
   * Create a {@link MVSolverHandle} which can be used to repeatedly solve multi
   * vehicle routing problem instances using the same {@link Solver},
   * {@link ModelProvider} and {@link SimulatorAPI} instance.
   * @param sol The {@link Solver} to use.
   * @param mp The {@link ModelProvider} to use.
   * @param simulator The {@link Simulator} to use.
   * @return The handle.
   */
  public static MVSolverHandle solver(Solver sol, ModelProvider mp,
      SimulatorAPI simulator) {
    return new SolverHandle(sol, mp, simulator);
  }

  /**
   * Create a {@link SVSolverHandle} which can be used to repeatedly solve
   * single vehicle routing problem instances.
   * @param s The {@link Solver} to use.
   * @param rm The {@link PDPRoadModel} to use.
   * @param pm The {@link PDPModel} to use.
   * @param sim The {@link SimulatorAPI} of the simulator.
   * @param v The {@link DefaultVehicle} to solve the problem for.
   * @return A {@link SVSolverHandle}.
   */
  public static SVSolverHandle singleVehicleSolver(Solver s, PDPRoadModel rm,
      PDPModel pm, SimulatorAPI sim, DefaultVehicle v) {
    return new SingleVehicleSolverHandle(s, rm, pm, sim, v);
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
   * A handle for solving single vehicle routing problems.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface SVSolverHandle {
    /**
     * Solve the single vehicle problem instance using the provided collection
     * of {@link DefaultParcel}s.
     * @param parcels The parcels for which a route is searched for.
     * @return A route containing all specified parcels and the parcels which
     *         are currently in the cargo of the vehicle.
     */
    Queue<DefaultParcel> solve(Collection<DefaultParcel> parcels);

    /**
     * Solve the single vehicle problem instance using the provided collection
     * of {@link DefaultParcel}s. And additionally a list containing the route
     * the vehicle is currently following.
     * @param parcels The parcels for which a route is searched for.
     * @param currentRoute The current route.
     * @return A route containing all specified parcels and the parcels which
     *         are currently in the cargo of the vehicle.
     */
    Queue<DefaultParcel> solve(Collection<DefaultParcel> parcels,
        @Nullable ImmutableList<DefaultParcel> currentRoute);

    /**
     * Converts the simulation state into a value object which is used by the
     * solver internally.
     * @param parcels
     * @param currentRoute
     * @return The {@link StateContext} containig the simulation state.
     */
    StateContext convert(Collection<DefaultParcel> parcels,
        @Nullable ImmutableList<DefaultParcel> currentRoute);
  }

  /**
   * A handle for solving multi vehicle routing problems.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface MVSolverHandle {
    /**
     * Solve the multi vehicle problem instance as represented by this handle's
     * simulator instance.
     * @return A list of routes, one for every vehicle in the simulation.
     */
    List<Queue<DefaultParcel>> solve();

    /**
     * Solve the multi vehicle problem instance as represented by this handle's
     * simulator instance.
     * @param currentRoutes A list of current routes the vehicles are following.
     * @return A list of routes, one for every vehicle in the simulation.
     */
    List<Queue<DefaultParcel>> solve(
        @Nullable ImmutableList<ImmutableList<DefaultParcel>> currentRoutes);
  }

  static class SingleVehicleSolverHandle extends SolverHandle implements
      SVSolverHandle {

    private final DefaultVehicle vehicle;

    SingleVehicleSolverHandle(Solver s, PDPRoadModel rm, PDPModel pm,
        SimulatorAPI sim, DefaultVehicle v) {
      super(s, rm, pm, sim);
      vehicle = v;
    }

    @Override
    public Queue<DefaultParcel> solve(Collection<DefaultParcel> parcels) {
      return solve(parcels, null);
    }

    @Override
    public Queue<DefaultParcel> solve(Collection<DefaultParcel> parcels,
        @Nullable ImmutableList<DefaultParcel> currentRoute) {
      final StateContext state = convert(parcels, currentRoute);
      return Solvers.convertRoutes(state, solver.solve(state.state)).get(0);
    }

    @Override
    public StateContext convert(Collection<DefaultParcel> parcels,
        @Nullable ImmutableList<DefaultParcel> currentRoute) {
      return Solvers.convert(rm, pm, asList(vehicle), parcels, time(),
          currentRoute == null ? null : ImmutableList.of(currentRoute));
    }
  }

  static class SolverHandle implements MVSolverHandle {
    final Solver solver;
    final SimulatorAPI simulator;
    final PDPRoadModel rm;
    final PDPModel pm;

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

    SolverHandle(Solver s, PDPRoadModel rm, PDPModel pm, SimulatorAPI sim) {
      solver = s;
      simulator = sim;
      this.rm = rm;
      this.pm = pm;
    }

    @Override
    public List<Queue<DefaultParcel>> solve() {
      return solve(null);
    }

    @Override
    public List<Queue<DefaultParcel>> solve(
        @Nullable ImmutableList<ImmutableList<DefaultParcel>> currentRoutes) {
      final StateContext state = convert(currentRoutes);
      return Solvers.convertRoutes(state, solver.solve(state.state));
    }

    Measure<Long, Duration> time() {
      return Measure.valueOf(simulator.getCurrentTime(),
          simulator.getTimeUnit());
    }

    StateContext convert(
        @Nullable ImmutableList<ImmutableList<DefaultParcel>> currentRoutes) {
      return Solvers.convert(rm, pm, rm.getObjectsOfType(DefaultVehicle.class),
          conv(pm.getParcels(ParcelState.ANNOUNCED, ParcelState.AVAILABLE,
              ParcelState.PICKING_UP)), time(), currentRoutes);
    }

  }

  /**
   * Value object containing information a {@link GlobalStateObject} and two
   * maps with references to the original vehicles and parcels.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static class StateContext {
    /**
     * A reference to the {@link GlobalStateObject}.
     */
    public final GlobalStateObject state;
    /**
     * A mapping of {@link VehicleDTO} to {@link DefaultVehicle}.
     */
    public final ImmutableMap<VehicleDTO, DefaultVehicle> vehicleMap;
    /**
     * A mapping of {@link ParcelDTO} to {@link DefaultParcel}.
     */
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
