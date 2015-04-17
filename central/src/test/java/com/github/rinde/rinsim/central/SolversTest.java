/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import static com.github.rinde.rinsim.central.Solvers.convertRoutes;
import static com.github.rinde.rinsim.core.model.time.TimeLapseFactory.create;
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
import com.github.rinde.rinsim.central.Solvers.SimulationConverter;
import com.github.rinde.rinsim.central.Solvers.SolveArgs;
import com.github.rinde.rinsim.central.Solvers.StateContext;
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
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.core.pdptw.DefaultParcel;
import com.github.rinde.rinsim.core.pdptw.DefaultVehicle;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.PDPTWTestUtil;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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

  TestVehicle v1;
  TestVehicle v2;
  TestVehicle v3;

  DefaultParcel p1;
  DefaultParcel p2;
  DefaultParcel p3;

  /**
   * Setup an environment with three vehicles and three parcels.
   */
  @Before
  public void setUp() {

    DependencyProvider dp = mock(DependencyProvider.class);

    rm = PDPRoadModel.builder(
      RoadModelBuilders.plane()
        .setMaxSpeed(300d)
      )
      .setAllowVehicleDiversion(false)
      .build(dp);

    when(dp.get(RoadModel.class)).thenReturn(rm);

    pm = DefaultPDPModel.builder()
      .setTimeWindowPolicy(TimeWindowPolicies.TARDY_ALLOWED)
      .build(dp);

    mp = new TestModelProvider(new ArrayList<>(
      Arrays.<Model<?>> asList(rm, pm)));
    rm.registerModelProvider(mp);

    v1 = new TestVehicle(new Point(0, 1));
    v2 = new TestVehicle(new Point(0, 2));
    v3 = new TestVehicle(new Point(0, 3));

    p1 = createParcel(new Point(3, 0), new Point(0, 3));
    p2 = createParcel(new Point(6, 9), new Point(2, 9));
    p3 = createParcel(new Point(2, 8), new Point(8, 2));
  }

  @Test
  public void convertTest() {
    // time unit = hour
    PDPTWTestUtil.register(rm, pm, v1, p1);

    Clock clock = mock(Clock.class);
    when(clock.getCurrentTime()).thenReturn(0L);
    when(clock.getTimeStep()).thenReturn(1L);
    when(clock.getTimeUnit()).thenReturn(NonSI.MINUTE);
    final SimulationConverter handle = Solvers.converterBuilder()
      .with(mp)
      .with(clock)
      .build();

    final StateContext sc = handle.convert(SolveArgs.create().useAllParcels()
      .noCurrentRoutes());

    assertEquals(1, sc.vehicleMap.size());
    assertEquals(v1.dto, sc.vehicleMap.keySet().iterator().next().getDto());
    assertTrue(sc.vehicleMap.containsValue(v1));
    assertEquals(ImmutableMap.of(p1.dto, p1), sc.parcelMap);

    assertEquals(ImmutableSet.of(p1.dto), sc.state.availableParcels);
    checkVehicles(asList(v1), sc.state.vehicles);

    rm.moveTo(v1, p1, create(NonSI.HOUR, 0L, 1L));

    checkVehicles(
      asList(v1),
      handle.convert(SolveArgs.create().useAllParcels().noCurrentRoutes()).state.vehicles);

    rm.moveTo(v1, p1, create(NonSI.HOUR, 0, 40));
    assertTrue(rm.equalPosition(v1, p1));
    pm.service(v1, p1, create(NonSI.HOUR, 0, 1));

    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(v1));
    final StateContext sc2 = handle.convert(SolveArgs.create().useAllParcels()
      .noCurrentRoutes());

    assertTrue(sc2.state.availableParcels.contains(p1.dto));
    assertFalse(sc2.state.vehicles.get(0).contents.contains(p1.dto));
    assertSame(p1.dto, sc2.state.vehicles.get(0).destination);
    assertEquals(29, sc2.state.vehicles.get(0).remainingServiceTime);
    assertFalse(sc2.state.vehicles.get(0).route.isPresent());

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

    final DefaultParcel destination = rm.getObjectsOfType(DefaultParcel.class)
      .iterator().next();
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000));
    assertEquals(destination, rm.getDestinationToParcel(v1));
    assertEquals(ParcelState.AVAILABLE, pm.getParcelState(p1));

    Clock clock = mock(Clock.class);
    when(clock.getCurrentTime()).thenReturn(0L);
    when(clock.getTimeStep()).thenReturn(1L);
    when(clock.getTimeUnit()).thenReturn(NonSI.MINUTE);

    final SimulationConverter handle = Solvers.converterBuilder()
      .with(mp)
      .with(clock)
      .build();

    final StateContext sc = handle.convert(SolveArgs.create()
      .useParcels(ImmutableSet.<DefaultParcel> of())
      .noCurrentRoutes());
    assertEquals(1, sc.state.availableParcels.size());
    assertEquals(0, sc.state.vehicles.get(0).contents.size());
    final Solver solver = SolverValidator.wrap(new MultiVehicleSolverAdapter(
      new RandomMVArraysSolver(new MersenneTwister(123)), NonSI.MINUTE));
    Solvers.solverBuilder(solver)
      .with(mp)
      .with(clock)
      .build()
      .solve(sc);

    // give enough time to reach destination
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000000000));
    assertEquals(rm.getPosition(destination), rm.getPosition(v1));

    pm.pickup(v1, destination, TimeLapseFactory.create(0, 1));
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(v1));

    final StateContext sc2 = handle.convert(SolveArgs.create()
      .useParcels(ImmutableSet.<DefaultParcel> of())
      .noCurrentRoutes());
    assertEquals(1, sc2.state.availableParcels.size());
    assertEquals(0, sc2.state.vehicles.get(0).contents.size());

    // finish pickup operation
    v1.tick(TimeLapseFactory.create(0, 1000000000));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(v1));

    final StateContext sc3 = handle.convert(SolveArgs.create()
      .useParcels(ImmutableSet.<DefaultParcel> of())
      .noCurrentRoutes());
    assertTrue(sc3.state.availableParcels.isEmpty());
    assertEquals(1, sc3.state.vehicles.get(0).contents.size());

    // move to delivery location
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000));
    assertEquals(destination, rm.getDestinationToParcel(v1));
    assertEquals(ParcelState.IN_CARGO, pm.getParcelState(p1));

    final StateContext sc4 = handle.convert(SolveArgs.create()
      .useParcels(ImmutableSet.<DefaultParcel> of())
      .noCurrentRoutes());
    assertEquals(1, sc4.state.vehicles.get(0).contents.size());
    assertTrue(sc4.state.availableParcels.isEmpty());

    // service delivery
    rm.moveTo(v1, destination, TimeLapseFactory.create(0, 1000000000));
    assertEquals(destination.dto.deliveryLocation, rm.getPosition(v1));
    pm.deliver(v1, p1, TimeLapseFactory.create(0, 1));
    assertNull(rm.getDestinationToParcel(v1));
    assertEquals(VehicleState.DELIVERING, pm.getVehicleState(v1));
    final StateContext sc5 = handle.convert(SolveArgs.create()
      .useParcels(ImmutableSet.<DefaultParcel> of())
      .noCurrentRoutes());
    assertEquals(1, sc5.state.vehicles.get(0).contents.size());
    assertTrue(sc5.state.availableParcels.isEmpty());

    // finish delivery operation
    v1.tick(TimeLapseFactory.create(0, 1000000000));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(v1));

    final StateContext sc6 = handle.convert(SolveArgs.create()
      .useParcels(ImmutableSet.<DefaultParcel> of())
      .noCurrentRoutes());
    assertTrue(sc6.state.vehicles.get(0).contents.isEmpty());
    assertTrue(sc6.state.availableParcels.isEmpty());
  }

  /**
   * Tests whether the
   * {@link Solvers#computeStats(GlobalStateObject, ImmutableList)} method
   * produces the same result when providing decomposed state objects.
   */
  @Test
  public void convertDecompositionTest() {
    final VehicleDTO vd1 = VehicleDTO.builder()
      .startPosition(new Point(5, 5))
      .speed(30d)
      .capacity(0)
      .availabilityTimeWindow(new TimeWindow(100L, 100000L))
      .build();

    final ParcelDTO a = ParcelDTO.builder(new Point(0, 0), new Point(10, 10))
      .pickupTimeWindow(new TimeWindow(0, 30))
      .deliveryTimeWindow(new TimeWindow(70, 80))
      .pickupDuration(5000L)
      .deliveryDuration(10000L)
      .build();

    final ParcelDTO b = ParcelDTO.builder(new Point(5, 0), new Point(10, 7))
      .pickupTimeWindow(new TimeWindow(0, 30))
      .deliveryTimeWindow(new TimeWindow(70, 80))
      .pickupDuration(5000L)
      .deliveryDuration(10000L)
      .build();

    final ParcelDTO c = ParcelDTO.builder(new Point(3, 0), new Point(6, 7))
      .pickupTimeWindow(new TimeWindow(0, 30))
      .deliveryTimeWindow(new TimeWindow(70, 80))
      .pickupDuration(5000L)
      .deliveryDuration(10000L)
      .build();

    final ParcelDTO d = ParcelDTO.builder(new Point(3, 0), new Point(6, 2))
      .pickupTimeWindow(new TimeWindow(0, 30))
      .deliveryTimeWindow(new TimeWindow(70, 80))
      .pickupDuration(5000L)
      .deliveryDuration(10000L)
      .build();

    final ImmutableSet<ParcelDTO> availableParcels = ImmutableSet
      .<ParcelDTO> builder()
      .add(b).add(c)
      .build();

    final ImmutableList<VehicleStateObject> vehicles = ImmutableList
      .<VehicleStateObject> builder()
      .add(
        new VehicleStateObject(
          vd1,
          new Point(7, 9),
          ImmutableSet.<ParcelDTO> of(a),
          0L,
          null,
          null))
      .add(new VehicleStateObject(
        vd1,
        new Point(3, 2),
        ImmutableSet.<ParcelDTO> of(d),
        0L,
        null,
        null))
      .build();

    final GlobalStateObject state = new GlobalStateObject(availableParcels,
      vehicles, 0L, SI.MILLI(SI.SECOND), NonSI.KILOMETERS_PER_HOUR,
      SI.KILOMETER);

    final ImmutableList<ImmutableList<ParcelDTO>> routes = ImmutableList
      .<ImmutableList<ParcelDTO>> builder()
      .add(ImmutableList.<ParcelDTO> of(a, b, c, c, b))
      .add(ImmutableList.<ParcelDTO> of(d, d))
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

  /**
   * Tests whether a mismatch in arguments supplied to convertRoutes is handled
   * correctly.
   */
  @Test
  public void convertRoutesFail() {
    final DefaultParcel a = new DefaultParcel(ParcelDTO.builder(
      new Point(0, 0), new Point(1, 1)).build());
    final DefaultParcel b = new DefaultParcel(ParcelDTO.builder(
      new Point(0, 1), new Point(1, 1)).build());

    final DefaultVehicle vehicle = new TestVehicle(new Point(1, 1));
    final GlobalStateObject gso = mock(GlobalStateObject.class);
    final VehicleStateObject vso = mock(VehicleStateObject.class);

    final StateContext sc = new StateContext(gso,
      ImmutableMap.of(vso, vehicle),
      ImmutableMap.of(a.dto, a));

    boolean fail = false;
    try {
      convertRoutes(sc, ImmutableList.of(ImmutableList.of(a.dto, b.dto)));
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  // doesn't check the contents!
  void checkVehicles(List<? extends TestVehicle> expected,
    ImmutableList<VehicleStateObject> states) {

    assertEquals(expected.size(), states.size());

    for (int i = 0; i < expected.size(); i++) {

      final TestVehicle vehicle = expected.get(i);
      final VehicleDTO dto = vehicle.dto;
      final VehicleStateObject vs = states.get(i);

      assertEquals(dto.availabilityTimeWindow,
        vs.getDto().availabilityTimeWindow);
      assertEquals(dto.capacity, vs.getDto().capacity);
      assertEquals(dto.speed, vs.getDto().speed, 0);
      assertEquals(dto.startPosition, vs.getDto().startPosition);

      assertEquals(rm.getPosition(expected.get(i)), vs.location);

      final DefaultParcel dest = rm.getDestinationToParcel(vehicle);
      if (dest == null) {
        assertNull(vs.destination);
      } else {
        assertEquals(dest.dto, vs.destination);
      }

      if (pm.getVehicleState(vehicle) == VehicleState.IDLE) {
        assertEquals(0, vs.remainingServiceTime);
      } else {
        assertEquals(pm.getVehicleActionInfo(vehicle).timeNeeded(),
          vs.remainingServiceTime);
      }
    }
  }

  static Set<ParcelDTO> toParcelDTOs(Collection<? extends Parcel> ps) {
    final ImmutableSet.Builder<ParcelDTO> builder = ImmutableSet.builder();
    for (final Parcel p : ps) {
      builder.add(((DefaultParcel) p).dto);
    }
    return builder.build();
  }

  static final TimeWindow TW = new TimeWindow(0, 1000);

  static DefaultParcel createParcel(Point origin, Point dest) {
    return new DefaultParcel(ParcelDTO.builder(origin, dest)
      .pickupTimeWindow(TW)
      .deliveryTimeWindow(TW)
      .serviceDuration(30)
      .build());
  }

  static class TestVehicle extends DefaultVehicle {
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
}
