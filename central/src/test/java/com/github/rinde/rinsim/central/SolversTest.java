/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.central;

import static com.github.rinde.rinsim.core.model.time.TimeLapseFactory.create;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.central.GlobalStateObject.VehicleStateObject;
import com.github.rinde.rinsim.central.Solvers.ExtendedStats;
import com.github.rinde.rinsim.central.Solvers.SimulationConverter;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.arrays.MultiVehicleSolverAdapter;
import com.github.rinde.rinsim.central.arrays.RandomMVArraysSolver;
import com.github.rinde.rinsim.core.TestModelProvider;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.VehicleState;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.road.RoadModelSnapshot;
import com.github.rinde.rinsim.core.model.road.RoadModelSnapshotTestUtil;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.GeomHeuristic;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.PDPGraphRoadModel;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.PDPTWTestUtil;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author Rinde van Lon
 *
 */
public class SolversTest {

  // TODO check for determinism of outputs

  PDPRoadModel rm;
  PDPModel pm;
  ModelProvider mp;
  RoadModelSnapshot ss;

  TestVehicle v1;
  TestVehicle v2;
  TestVehicle v3;

  Parcel p1;
  Parcel p2;
  Parcel p3;

  /**
   * Setup an environment with three vehicles and three parcels.
   */
  @Before
  public void setUp() {

    final DependencyProvider dp = mock(DependencyProvider.class);

    rm = PDPRoadModel.builder(
      RoadModelBuilders.plane()
        .withMaxSpeed(300d))
      .withAllowVehicleDiversion(false)
      .build(dp);

    when(dp.get(RoadModel.class)).thenReturn(rm);

    pm = DefaultPDPModel.builder()
      .withTimeWindowPolicy(TimeWindowPolicies.TARDY_ALLOWED)
      .build(dp);

    mp = new TestModelProvider(new ArrayList<>(
      Arrays.<Model<?>>asList(rm, pm)));
    rm.registerModelProvider(mp);

    v1 = new TestVehicle(new Point(0, 1));
    v2 = new TestVehicle(new Point(0, 2));
    v3 = new TestVehicle(new Point(0, 3));

    p1 = createParcel(new Point(3, 0), new Point(0, 3));
    p2 = createParcel(new Point(6, 9), new Point(2, 9));
    p3 = createParcel(new Point(2, 8), new Point(8, 2));

    ss = rm.getSnapshot();
  }

  @Test
  public void convertTest() {
    // time unit = hour
    PDPTWTestUtil.register(rm, pm, v1, p1);

    final Clock clock = mock(Clock.class);
    when(clock.getCurrentTime()).thenReturn(0L);
    when(clock.getTickLength()).thenReturn(1L);
    when(clock.getTimeUnit()).thenReturn(NonSI.MINUTE);
    final SimulationConverter handle = Solvers.converterBuilder()
      .with(mp)
      .with(clock)
      .build();

    final GlobalStateObject state = handle.convert(SolveArgs.create()
      .useAllParcels()
      .noCurrentRoutes());

    assertEquals(ImmutableSet.of(p1), state.getAvailableParcels());
    checkVehicles(asList(v1), state.getVehicles());

    rm.moveTo(v1, p1, create(NonSI.HOUR, 0L, 1L));

    checkVehicles(
      asList(v1),
      handle.convert(SolveArgs.create().useAllParcels().noCurrentRoutes())
        .getVehicles());

    rm.moveTo(v1, p1, create(NonSI.HOUR, 0, 40));
    assertTrue(rm.equalPosition(v1, p1));
    pm.service(v1, p1, create(NonSI.HOUR, 0, 1));

    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(v1));
    final GlobalStateObject state2 =
      handle.convert(SolveArgs.create().useAllParcels()
        .noCurrentRoutes());

    assertTrue(state2.getAvailableParcels().contains(p1));
    assertFalse(state2.getVehicles().get(0).getContents().contains(p1));
    assertSame(p1, state2.getVehicles().get(0).getDestination().get());
    assertEquals(29, state2.getVehicles().get(0).getRemainingServiceTime());
    assertFalse(state2.getVehicles().get(0).getRoute().isPresent());

    // checkVehicles(asList(v1), sc2.state.vehicles);
  }

  /**
   * Checks whether conversion performs correctly in case a parcel is not
   * indicated as being available but is still listed as a destination (and is
   * available).
   */
  @Test
  public void convertWithAbsentDestination() {
    PDPTWTestUtil.register(rm, pm, v1, p1);

    final Parcel destination = rm.getObjectsOfType(Parcel.class)
      .iterator().next();
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000));
    assertEquals(destination, rm.getDestinationToParcel(v1));
    assertEquals(ParcelState.AVAILABLE, pm.getParcelState(p1));

    final Clock clock = mock(Clock.class);
    when(clock.getCurrentTime()).thenReturn(0L);
    when(clock.getTickLength()).thenReturn(1L);
    when(clock.getTimeUnit()).thenReturn(NonSI.MINUTE);

    final SimulationConverter handle = Solvers.converterBuilder()
      .with(mp)
      .with(clock)
      .build();

    final GlobalStateObject state = handle.convert(SolveArgs.create()
      .noCurrentRoutes());
    assertEquals(1, state.getAvailableParcels().size());
    assertEquals(0, state.getVehicles().get(0).getContents().size());
    final Solver solver = SolverValidator.wrap(new MultiVehicleSolverAdapter(
      new RandomMVArraysSolver(new MersenneTwister(123)), NonSI.MINUTE));
    Solvers.solverBuilder(solver)
      .with(mp)
      .with(clock)
      .build()
      .solve(state);

    // give enough time to reach destination
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000000000));
    assertEquals(rm.getPosition(destination), rm.getPosition(v1));

    pm.pickup(v1, destination, TimeLapseFactory.create(0, 1));
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(v1));

    final GlobalStateObject state2 = handle.convert(SolveArgs.create()
      .noCurrentRoutes());
    assertEquals(1, state2.getAvailableParcels().size());
    assertEquals(0, state2.getVehicles().get(0).getContents().size());

    // finish pickup operation
    v1.tick(TimeLapseFactory.create(0, 1000000000));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(v1));

    final GlobalStateObject state3 = handle.convert(SolveArgs.create()
      .useParcels(ImmutableSet.<Parcel>of())
      .noCurrentRoutes());
    assertTrue(state3.getAvailableParcels().isEmpty());
    assertEquals(1, state3.getVehicles().get(0).getContents().size());

    // move to delivery location
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000));
    assertEquals(destination, rm.getDestinationToParcel(v1));
    assertEquals(ParcelState.IN_CARGO, pm.getParcelState(p1));

    final GlobalStateObject state4 = handle.convert(SolveArgs.create()
      .useParcels(ImmutableSet.<Parcel>of())
      .noCurrentRoutes());
    assertEquals(1, state4.getVehicles().get(0).getContents().size());
    assertTrue(state4.getAvailableParcels().isEmpty());

    // service delivery
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000000000));
    assertEquals(destination.getDto().getDeliveryLocation(),
      rm.getPosition(v1));
    pm.deliver(v1, p1, TimeLapseFactory.create(0, 1));
    assertNull(rm.getDestinationToParcel(v1));
    assertEquals(VehicleState.DELIVERING, pm.getVehicleState(v1));
    final GlobalStateObject state5 = handle.convert(SolveArgs.create()
      .useParcels(ImmutableSet.<Parcel>of())
      .noCurrentRoutes());
    assertEquals(1, state5.getVehicles().get(0).getContents().size());
    assertTrue(state5.getAvailableParcels().isEmpty());

    // finish delivery operation
    v1.tick(TimeLapseFactory.create(0, 1000000000));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(v1));

    final GlobalStateObject state6 = handle.convert(SolveArgs.create()
      .useParcels(ImmutableSet.<Parcel>of())
      .noCurrentRoutes());
    assertTrue(state6.getVehicles().get(0).getContents().isEmpty());
    assertTrue(state6.getAvailableParcels().isEmpty());
  }

  /**
   * Tests whether the
   * {@link Solvers#computeStats(GlobalStateObject, ImmutableList)} method
   * produces the same result when providing decomposed state objects.
   */
  @Test
  public void convertDecompositionTest() {
    final Optional<Connection<?>> absent = Optional.absent();
    final VehicleDTO vd1 = VehicleDTO.builder()
      .startPosition(new Point(5, 5))
      .speed(30d)
      .capacity(0)
      .availabilityTimeWindow(TimeWindow.create(100L, 100000L))
      .build();

    final Parcel a = Parcel.builder(new Point(0, 0), new Point(10, 10))
      .pickupTimeWindow(TimeWindow.create(0, 30))
      .deliveryTimeWindow(TimeWindow.create(70, 80))
      .pickupDuration(5000L)
      .deliveryDuration(10000L)
      .build();

    final Parcel b = Parcel.builder(new Point(5, 0), new Point(10, 7))
      .pickupTimeWindow(TimeWindow.create(0, 30))
      .deliveryTimeWindow(TimeWindow.create(70, 80))
      .pickupDuration(5000L)
      .deliveryDuration(10000L)
      .build();

    final Parcel c = Parcel.builder(new Point(3, 0), new Point(6, 7))
      .pickupTimeWindow(TimeWindow.create(0, 30))
      .deliveryTimeWindow(TimeWindow.create(70, 80))
      .pickupDuration(5000L)
      .deliveryDuration(10000L)
      .build();

    final Parcel d = Parcel.builder(new Point(3, 0), new Point(6, 2))
      .pickupTimeWindow(TimeWindow.create(0, 30))
      .deliveryTimeWindow(TimeWindow.create(70, 80))
      .pickupDuration(5000L)
      .deliveryDuration(10000L)
      .build();

    final ImmutableSet<Parcel> availableParcels = ImmutableSet
      .<Parcel>builder()
      .add(b).add(c)
      .build();

    final ImmutableList<VehicleStateObject> vehicles = ImmutableList
      .<VehicleStateObject>builder()
      .add(
        VehicleStateObject.create(
          vd1,
          new Point(7, 9),
          absent,
          ImmutableSet.<Parcel>of(a),
          0L,
          null,
          null))
      .add(VehicleStateObject.create(
        vd1,
        new Point(3, 2),
        absent,
        ImmutableSet.<Parcel>of(d),
        0L,
        null,
        null))
      .build();

    final GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0L, SI.MILLI(SI.SECOND), NonSI.KILOMETERS_PER_HOUR,
      SI.KILOMETER, ss);

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList
      .<ImmutableList<Parcel>>builder()
      .add(ImmutableList.<Parcel>of(a, b, c, c, b))
      .add(ImmutableList.<Parcel>of(d, d))
      .build();

    final StatisticsDTO stats = Solvers.computeStats(state, routes);
    final ObjectiveFunction objFunc = Gendreau06ObjectiveFunction.instance();
    final double cost = objFunc.computeCost(stats);

    final double cost0 = objFunc.computeCost(Solvers.computeStats(
      state.withSingleVehicle(0),
      ImmutableList.of(routes.get(0))));
    final double cost1 = objFunc.computeCost(Solvers.computeStats(
      state.withSingleVehicle(1),
      ImmutableList.of(routes.get(1))));
    assertEquals(cost, cost0 + cost1, 0.001);
  }

  @Test
  public void precisionTest() throws InterruptedException {
    final Parcel A = Parcel.builder(new Point(5, 5), new Point(2, 0))
      .serviceDuration(180000L)
      .build();

    final GlobalStateObject gso = GlobalStateObjectBuilder.globalBuilder()
      .addAvailableParcel(A)
      .addVehicle(GlobalStateObjectBuilder.vehicleBuilder()
        .setLocation(new Point(5, 5))
        .setRoute(ImmutableList.<Parcel>of(A, A))
        .setRemainingServiceTime(120000L)
        .setDestination(A)
        .build())
      .setPlaneTravelTimes(new Point(0, 0), new Point(10, 10))
      .build();

    final ImmutableList<ImmutableList<Parcel>> schedule =
      ImmutableList.of(ImmutableList.of(A, A));

    // cost in milliseconds
    final double rinSimCost = Gendreau06ObjectiveFunction.instance(50d)
      .computeCost(Solvers.computeStats(gso, schedule)) * 60000d;
    assertThat(rinSimCost).isWithin(0.00001).of(563828.5364288616);

    final Graph<LengthData> g = new TableGraph<>();
    Graphs.addBiPath(g, new Point(5, 5), new Point(2, 0), new Point(0, 0),
      new Point(5, 5));

    final GlobalStateObject gso2 = GlobalStateObjectBuilder.globalBuilder()
      .addAvailableParcel(A)
      .addVehicle(GlobalStateObjectBuilder.vehicleBuilder()
        .setLocation(new Point(5, 5))
        .setRoute(ImmutableList.<Parcel>of(A, A))
        .setRemainingServiceTime(120000L)
        .setDestination(A)
        .build())
      .setSnapshot(RoadModelSnapshotTestUtil.createGraphRoadModelSnapshot(g,
        SI.KILOMETER))
      .build();

    final double rinSimCost2 = Gendreau06ObjectiveFunction.instance(50d)
      .computeCost(Solvers.computeStats(gso2, schedule)) * 60000d;
    assertThat(rinSimCost2).isWithin(0.00001).of(563828.5364288616);
  }

  /**
   * Tests whether the
   * {@link Solvers#computeStats(GlobalStateObject, ImmutableList)} method
   * succeeds when the vehicle is moving on a connection.
   */
  @Test
  public void vehicleOnConnectionTest() {
    final DependencyProvider graphdp = mock(DependencyProvider.class);

    // A graph for the vehicle to be present on
    final Graph g = new TableGraph<>();
    // The connection points
    final Point a = new Point(0, 0);
    final Point b = new Point(2, 0);
    final Point c = new Point(2, 1);
    // The connections
    Graphs.addBiPath(g, a, b, c);

    // Build a PDPRoadModel with the graph
    final PDPRoadModel graphRm =
      PDPGraphRoadModel.builderForGraphRm(
        RoadModelBuilders.staticGraph(g)
          .withDistanceUnit(SI.METER)
          .withSpeedUnit(SI.METERS_PER_SECOND))
        .withAllowVehicleDiversion(true)
        .build(graphdp);

    when(graphdp.get(RoadModel.class)).thenReturn(graphRm);

    // The test vehicle
    final MovingTestVehicle mv1 = new MovingTestVehicle(a, b, graphRm);
    graphRm.register(mv1);

    // The test parcel for the route
    final Parcel mp1 = createParcel(b, c);

    // Build State
    final ImmutableSet<Parcel> availableParcels = ImmutableSet
      .<Parcel>builder()
      .add(mp1)
      .build();

    Optional<? extends Connection<?>> vehicleConnection =
      Optional.absent();

    ImmutableList<VehicleStateObject> vehicles = ImmutableList
      .<VehicleStateObject>builder()
      .add(
        VehicleStateObject.create(
          mv1.getDTO(),
          graphRm.getPosition(mv1),
          vehicleConnection,
          ImmutableSet.<Parcel>of(mp1),
          0L,
          null,
          null))
      .build();

    GlobalStateObject state = GlobalStateObject.create(availableParcels,
      vehicles, 0L, SI.SECOND, SI.METERS_PER_SECOND,
      SI.METER, graphRm.getSnapshot());

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList
      .<ImmutableList<Parcel>>builder()
      .add(ImmutableList.<Parcel>of(mp1))
      .build();

    // Should succeed
    ExtendedStats stats = Solvers.computeStats(state, routes);
    // Assert initial solver values
    assertEquals(6d, stats.totalDistance, 0.00001);
    assertEquals(6d, stats.totalTravelTime, 0.00001);

    // Move the vehicle a bit
    mv1.tickImpl(TimeLapseFactory.create(SI.SECOND, 0L, 1L));
    // Assert new location of vehicle
    assertEquals(new Point(1, 0), graphRm.getPosition(mv1));

    vehicleConnection =
      ((GraphRoadModel) graphRm).getConnection(mv1);

    vehicles = ImmutableList
      .<VehicleStateObject>builder()
      .add(
        VehicleStateObject.create(
          mv1.getDTO(),
          graphRm.getPosition(mv1),
          vehicleConnection,
          ImmutableSet.<Parcel>of(mp1),
          0L,
          null,
          null))
      .build();

    state =
      GlobalStateObject.create(availableParcels,
        vehicles, 1L, SI.SECOND, SI.METERS_PER_SECOND,
        SI.METER, graphRm.getSnapshot());

    // Should succeed
    stats = Solvers.computeStats(state, routes);

    // Assert correct new stats
    assertEquals(5d, stats.totalDistance, 0.00001);
    assertEquals(5d, stats.totalTravelTime, 0.00001);
  }

  /**
   * When a vehicle starts in a depot, the solver has to calculate the distance
   * to the depot from the starting position. Similar if a parcel is at the same
   * location as the vehicle, the distance to this parcel is zero. These paths
   * have only one element (when calculated with
   * {@link Graphs#shortestPath(Graph, Point, Point, GeomHeuristic)}) and the
   * distance should be correctly interpreted as zero.
   */
  @Test
  public void vehicleOnStartPositionTest() {
    final TestVehicle vehicle = new TestVehicle(new Point(0, 0));
    final Parcel parcel = createParcel(new Point(0, 0), new Point(0, 0));

    final Graph g = new TableGraph<>();
    g.addConnection(new Point(0, 0), new Point(1, 0));
    final DependencyProvider graphdp = mock(DependencyProvider.class);
    final PDPRoadModel graphRm = PDPGraphRoadModel.builderForGraphRm(
      RoadModelBuilders.staticGraph(g).withDistanceUnit(SI.METER)
        .withSpeedUnit(SI.METERS_PER_SECOND))
      .withAllowVehicleDiversion(true)
      .build(graphdp);
    when(graphdp.get(RoadModel.class)).thenReturn(graphRm);

    // Build State
    final ImmutableSet<Parcel> availableParcels = ImmutableSet
      .<Parcel>builder()
      .add(parcel)
      .build();

    final VehicleStateObject vehicelState =
      GlobalStateObjectBuilder.vehicleBuilder().setVehicleDTO(vehicle.getDTO())
        .setDestination(parcel).setRoute(availableParcels.asList()).build();
    final GlobalStateObject globalState =
      GlobalStateObjectBuilder.globalBuilder().addAvailableParcel(parcel)
        .addVehicle(vehicelState).setSnapshot(graphRm.getSnapshot())
        .buildUnsafe();

    final ImmutableList<ImmutableList<Parcel>> routes = ImmutableList
      .<ImmutableList<Parcel>>builder()
      .add(ImmutableList.<Parcel>of(parcel))
      .build();

    // Should succeed
    final ExtendedStats stats = Solvers.computeStats(globalState, routes);
    // Assert that the vehicle doesn't need to move.
    assertEquals(0d, stats.totalDistance, 0.00001);
  }

  // doesn't check the contents!
  void checkVehicles(List<? extends TestVehicle> expected,
      ImmutableList<VehicleStateObject> states) {

    assertEquals(expected.size(), states.size());

    for (int i = 0; i < expected.size(); i++) {

      final TestVehicle vehicle = expected.get(i);
      final VehicleDTO dto = vehicle.dto;
      final VehicleStateObject vs = states.get(i);

      assertEquals(dto.getAvailabilityTimeWindow(),
        vs.getDto().getAvailabilityTimeWindow());
      assertEquals(dto.getCapacity(), vs.getDto().getCapacity());
      assertEquals(dto.getSpeed(), vs.getDto().getSpeed(), 0);
      assertEquals(dto.getStartPosition(), vs.getDto().getStartPosition());

      assertEquals(rm.getPosition(expected.get(i)), vs.getLocation());

      final Parcel dest = rm.getDestinationToParcel(vehicle);
      if (dest == null) {
        assertFalse(vs.getDestination().isPresent());
      } else {
        assertEquals(dest, vs.getDestination().get());
      }

      if (pm.getVehicleState(vehicle) == VehicleState.IDLE) {
        assertEquals(0, vs.getRemainingServiceTime());
      } else {
        assertEquals(pm.getVehicleActionInfo(vehicle).timeNeeded(),
          vs.getRemainingServiceTime());
      }
    }
  }

  static Set<ParcelDTO> toParcelDTOs(Collection<? extends Parcel> ps) {
    final ImmutableSet.Builder<ParcelDTO> builder = ImmutableSet.builder();
    for (final Parcel p : ps) {
      builder.add(p.getDto());
    }
    return builder.build();
  }

  static final TimeWindow TW = TimeWindow.create(0, 1000);

  static Parcel createParcel(Point origin, Point dest) {
    return Parcel.builder(origin, dest)
      .pickupTimeWindow(TW)
      .deliveryTimeWindow(TW)
      .serviceDuration(30)
      .build();
  }

  static class TestVehicle extends Vehicle {
    public final VehicleDTO dto;

    TestVehicle(Point start) {
      super(VehicleDTO.builder()
        .startPosition(start)
        .speed(.1)
        .capacity(1)
        .availabilityTimeWindow(TW)
        .build());
      dto = getDTO();
    }

    @Override
    protected void tickImpl(TimeLapse time) {}
  }

  static class MovingTestVehicle extends Vehicle {
    public final VehicleDTO dto;
    private final Point target;
    private final PDPRoadModel model;

    MovingTestVehicle(Point start, Point destination, PDPRoadModel m) {
      super(VehicleDTO.builder()
        .startPosition(start)
        .speed(1)
        .capacity(1)
        .availabilityTimeWindow(TW)
        .build());
      dto = getDTO();
      target = destination;
      model = m;
    }

    @Override
    protected void tickImpl(TimeLapse time) {
      model.moveTo(this, target, time);
    }
  }
}
