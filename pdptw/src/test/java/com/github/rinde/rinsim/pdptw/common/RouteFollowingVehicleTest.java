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
package com.github.rinde.rinsim.pdptw.common;

import static com.github.rinde.rinsim.core.model.time.TimeLapseFactory.time;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.rinde.rinsim.core.TestModelProvider;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.VehicleState;
import com.github.rinde.rinsim.core.model.pdp.PDPModelEvent;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.event.ListenerEventHistory;
import com.github.rinde.rinsim.fsm.StateMachine.StateMachineEvent;
import com.github.rinde.rinsim.fsm.StateMachine.StateTransitionEvent;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle.DefaultEvent;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle.RouteAdjuster;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle.RouteAdjusters;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle.StateEvent;
import com.github.rinde.rinsim.pdptw.common.SubVehicle.ExtraEvent;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;

/**
 * Tests for {@link RouteFollowingVehicle}.
 * @author Rinde van Lon
 */
@RunWith(value = Parameterized.class)
public class RouteFollowingVehicleTest {

  // EPSILON in km: maximum allowed deviation is 10 cm
  static final double EPSILON = 0.0001;

  private PDPRoadModel rm;
  private PDPModel pm;
  private RouteFollowingVehicle d, d2;
  private Parcel p1, p2, p3;
  private Depot depot;
  private final boolean diversionIsAllowed;
  private final boolean allowDelayedRouteChanges;
  private final RouteAdjuster routeAdjuster;

  /**
   * Create test.
   * @param allowDiversion Is vehicle diversion allowed.
   * @param allowDelayedRouteChange Are delayed route changes allowed.
   * @param adj The route adjuster.
   */
  @SuppressWarnings("null")
  public RouteFollowingVehicleTest(boolean allowDiversion,
      boolean allowDelayedRouteChange, RouteAdjuster adj) {
    diversionIsAllowed = allowDiversion;
    allowDelayedRouteChanges = allowDelayedRouteChange;
    routeAdjuster = adj;
    TestUtil.testEnum(DefaultEvent.class);
  }

  /**
   * @return parameters for constructor.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
      {true, true, RouteAdjusters.NOP},
      {true, true, RouteFollowingVehicle.delayAdjuster()},
      {true, false, RouteAdjusters.NOP},
      {true, false, RouteAdjusters.DELAY_ADJUSTER},
      {false, true, RouteAdjusters.NOP},
      {false, true, RouteAdjusters.DELAY_ADJUSTER},
      {false, false, RouteAdjusters.NOP},
      {false, false, RouteAdjusters.DELAY_ADJUSTER}
    });
  }

  /**
   * Set up a simple simulation with models, two vehicles, three parcels and one
   * depot.
   */
  @Before
  public void setUp() {
    init(true);
  }

  /**
   * Sets up the test environment.
   * @param register If true the objects will be registered in their models.
   */
  protected void init(boolean register) {
    final DependencyProvider dp = mock(DependencyProvider.class);
    rm = PDPRoadModel.builder(
      RoadModelBuilders.plane()
        .withMaxSpeed(30d))
      .withAllowVehicleDiversion(diversionIsAllowed)
      .build(dp);

    when(dp.get(RoadModel.class)).thenReturn(rm);

    pm = DefaultPDPModel.builder()
      .withTimeWindowPolicy(TimeWindowPolicies.TARDY_ALLOWED)
      .build(dp);

    final ModelProvider mp = new TestModelProvider(new ArrayList<Model<?>>(
      asList(rm, pm)));
    rm.registerModelProvider(mp);

    final VehicleDTO v = VehicleDTO.builder()
      .startPosition(new Point(1, 1))
      .speed(30d)
      .capacity(1)
      .availabilityTimeWindow(TimeWindow.create(0, minute(30)))
      .build();
    d = new RouteFollowingVehicle(v, allowDelayedRouteChanges, routeAdjuster);
    d2 = new RouteFollowingVehicle(v, allowDelayedRouteChanges, routeAdjuster);

    assertThat(d.isDelayedRouteChangingAllowed())
      .isEqualTo(allowDelayedRouteChanges);
    assertThat(d2.isDelayedRouteChangingAllowed())
      .isEqualTo(allowDelayedRouteChanges);

    p1 = Parcel
      .builder(new Point(1, 2), new Point(1, 4))
      .pickupTimeWindow(TimeWindow.create(minute(5), minute(15)))
      .deliveryTimeWindow(TimeWindow.create(minute(16), minute(30)))
      .pickupDuration(minute(3))
      .deliveryDuration(minute(1))
      .build();

    p2 = Parcel
      .builder(new Point(1, 3), new Point(1, 5))
      .pickupTimeWindow(TimeWindow.create(minute(15) + 10, minute(25)))
      .deliveryTimeWindow(TimeWindow.create(minute(22) + 10, minute(30)))
      .deliveryDuration(minute(3))
      .build();

    p3 = Parcel.builder(new Point(1, 3), new Point(1, 5))
      .pickupTimeWindow(TimeWindow.create(minute(15) + 10, minute(25)))
      .deliveryTimeWindow(TimeWindow.create(minute(22) + 10, minute(30)))
      .serviceDuration(minute(3))
      .build();

    depot = new Depot(new Point(3, 5));
    if (register) {
      PDPTWTestUtil.register(rm, pm, depot, d, d2, p1, p2, p3);
      assertEquals(depot, d.getDepot());
    }

    d.waitState.toString();
  }

  /**
   * Tests the check for the number of depots, only 1 is allowed.
   */
  @Test(expected = IllegalArgumentException.class)
  public void multiDepotFail() {
    init(false);
    PDPTWTestUtil.register(rm, pm, depot, new Depot(new Point(3, 5)), d);
  }

  /**
   * Test definition of isTooEarly.
   */
  @Test
  public void testIsTooEarly() {
    // traveling 1km at 30km/h should take 2 minutes
    assertEquals(minute(2),
      d.computeTravelTimeTo(new Point(1, 2), SI.MILLI(SI.SECOND)));

    // if we start immediately we are too early
    assertTrue(d.isTooEarly(p1, time(0, 10)));
    assertTrue(d.isTooEarly(p1, time(minute(2), minute(3))));
    assertFalse(d.isTooEarly(p1, time(minute(2) + 1, minute(3) + 1)));
    assertFalse(d.isTooEarly(p1, time(minute(3), minute(4))));
    assertFalse(d.isTooEarly(p1, time(minute(4), minute(5))));
    assertFalse(d.isTooEarly(p1, time(minute(10), minute(11))));

    assertTrue(d.isTooEarly(p2, time(0, 10)));
    assertTrue(d.isTooEarly(p2, time(minute(10) + 10, minute(11) + 10)));
    assertFalse(d.isTooEarly(p2, time(minute(10) + 11, minute(11) + 11)));
    assertFalse(d.isTooEarly(p2, time(minute(11), minute(12))));
    assertFalse(d.isTooEarly(p2, time(minute(12), minute(13))));
    assertFalse(d.isTooEarly(p2, time(minute(20), minute(21))));
  }

  static TimeLapse copy(TimeLapse tl) {
    final TimeLapse copy = TimeLapseFactory.create(tl.getTimeUnit(),
      tl.getStartTime(), tl.getEndTime());
    if (tl.getTimeConsumed() > 0) {
      copy.consume(tl.getTimeConsumed());
    }
    return copy;
  }

  void tick(long beginMinute, long endMinute, long consumeSeconds) {
    final TimeLapse tl = time(minute(beginMinute), minute(endMinute));
    if (consumeSeconds > 0) {
      tl.consume(consumeSeconds * 1000);
    }
    pm.tick(copy(tl));

    final TimeLapse dtl = copy(tl);
    d.tick(dtl);
    assertSame(dtl, d.getCurrentTimeLapse());
    assertFalse(dtl.hasTimeLeft());

    // tests whether internal states of vehicle match the state of the pdp model
    // at all times
    if (pm.getVehicleState(d) == VehicleState.DELIVERING
      || pm.getVehicleState(d) == VehicleState.PICKING_UP) {
      assertSame(d.serviceState, d.stateMachine.getCurrentState());
    } else {
      assertThat(d.serviceState).isNotSameAs(d.stateMachine.getCurrentState());
    }
  }

  void tick(long beginMinute, long endMinute) {
    tick(beginMinute, endMinute, 0);
  }

  /**
   * Tests the proper execution of a route, including having to wait.
   */
  @Test
  public void testRouteExecution() {
    d.setRoute(asList(p1, p2, p1));

    // we are too early, don't move
    tick(0, 1);
    tick(1, 2);
    tick(2, 3);
    assertEquals(new Point(1, 1), rm.getPosition(d));

    // we can go
    tick(3, 4);
    assertEquals(new Point(1, 1.5), rm.getPosition(d));

    // we arrive, start pickup
    tick(4, 5);
    assertEquals(new Point(1, 2), rm.getPosition(d));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(d));

    // we can pickup in this tick
    tick(5, 6);
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(d));
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(p1));
    assertEquals(pm.getVehicleActionInfo(d).timeNeeded(), minute(2));

    // still picking up
    tick(6, 7);
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(d));
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(p1));
    assertEquals(pm.getVehicleActionInfo(d).timeNeeded(), minute(1));

    // done picking up
    tick(7, 8);
    assertEquals(VehicleState.IDLE, pm.getVehicleState(d));
    assertEquals(ParcelState.IN_CARGO, pm.getParcelState(p1));
    assertEquals(newHashSet(p1), pm.getContents(d));

    // too early
    tick(8, 9);
    tick(12, 13);
    assertEquals(new Point(1, 2), rm.getPosition(d));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(d));

    // let's go
    tick(13, 14);
    assertEquals(new Point(1, 2.5), rm.getPosition(d));

    // arrive, don't pickup yet
    tick(14, 15);
    assertEquals(new Point(1, 3), rm.getPosition(d));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(d));

    // pickup and move towards destination p1
    tick(15, 16);
    assertEquals(0d, Point.distance(new Point(1, 3.5), rm.getPosition(d)),
      EPSILON);
    assertEquals(newHashSet(p1, p2), pm.getContents(d));

    // move
    tick(16, 17);
    assertEquals(0d, Point.distance(new Point(1, 4d), rm.getPosition(d)),
      EPSILON);

    // arrive and start
    tick(17, 18);
    assertEquals(new Point(1, 4), rm.getPosition(d));
    assertEquals(VehicleState.DELIVERING, pm.getVehicleState(d));
    assertEquals(ParcelState.DELIVERING, pm.getParcelState(p1));

    // deliver p1
    tick(18, 19);
    assertEquals(VehicleState.IDLE, pm.getVehicleState(d));
    assertEquals(ParcelState.DELIVERED, pm.getParcelState(p1));

    // route is empty, should wait here since it is not yet end of the day
    tick(19, 20);
    assertEquals(new Point(1, 4), rm.getPosition(d));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(d));

    d.setRoute(asList(p2));

    // move towards delivery p2
    tick(20, 21);
    assertEquals(new Point(1, 4.5), rm.getPosition(d));

    // arrival at delivery p2
    tick(21, 22);
    assertEquals(new Point(1, 5), rm.getPosition(d));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(d));

    // delivering p2
    tick(22, 23);
    assertEquals(VehicleState.DELIVERING, pm.getVehicleState(d));
    assertEquals(minute(2) + 10, pm.getVehicleActionInfo(d).timeNeeded());

    tick(23, 24);
    assertEquals(VehicleState.DELIVERING, pm.getVehicleState(d));
    assertEquals(minute(1) + 10, pm.getVehicleActionInfo(d).timeNeeded());

    tick(24, 25);
    assertEquals(VehicleState.DELIVERING, pm.getVehicleState(d));
    assertEquals(minute(0) + 10, pm.getVehicleActionInfo(d).timeNeeded());

    assertEquals(new Point(1, 5), rm.getPosition(d));

    // travel time back to the depot is 4 minutes. so we should go to depot
    // at 26'.
    assertEquals(minute(4),
      d.computeTravelTimeTo(new Point(3, 5), SI.MILLI(SI.SECOND)));

    // don't do anything yet
    tick(25, 26);
    assertEquals(new Point(1, 5), rm.getPosition(d));

    // let's go home!
    tick(26, 27);
    assertEquals(new Point(1.5, 5), rm.getPosition(d));

    // make sure that no movement takes place when there is no time to consume
    tick(27, 28, 60);
    assertEquals(new Point(1.5, 5), rm.getPosition(d));

    tick(27, 28);
    assertEquals(new Point(2, 5), rm.getPosition(d));

    tick(28, 29);
    assertEquals(new Point(2.5, 5), rm.getPosition(d));

    tick(29, 30);
    assertEquals(new Point(3, 5), rm.getPosition(d));

    // stay there
    tick(30, 31);
    assertEquals(new Point(3, 5), rm.getPosition(d));
  }

  /**
   * Test that an already delivered parcel is rejected by setRoute.
   */
  @Test
  public void setRouteTest1() {
    d.setRoute(asList(p2, p2));

    // when delivering the route may still contain p2
    tick(0, 25);
    assertEquals(ParcelState.DELIVERING, pm.getParcelState(p2));
    d.setRoute(asList(p2));

    // when delivered the route may no longer contain p2
    tick(25, 30);
    assertEquals(ParcelState.DELIVERED, pm.getParcelState(p2));
    boolean exception = false;
    try {
      d.setRoute(asList(p2));
    } catch (final IllegalArgumentException e) {
      exception = true;
    }
    assertThat(exception).isSameAs(routeAdjuster == RouteAdjusters.NOP);
  }

  /**
   * Tests whether setRoute correctly rejects vehicles that attempt to go to
   * parcels already being serviced by someone else.
   */
  @Test
  public void setRouteTest2() {
    d.setRoute(asList(p1, p1));
    tick(12, 15);
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(p1));
    assertEquals(ParcelState.ANNOUNCED, pm.getParcelState(p3));
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(d));
    assertEquals(VehicleState.IDLE, pm.getVehicleState(d2));

    // vehicle must also be in picking up state
    boolean exception = false;
    try {
      d2.setRoute(asList(p1));
    } catch (final IllegalArgumentException e) {
      exception = true;
    }
    assertThat(exception).isSameAs(routeAdjuster == RouteAdjusters.NOP);

    d2.setRoute(asList(p3));
    d2.tick(time(minute(10), minute(16)));
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(p1));
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(p3));
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(d));
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(d2));

    assertThat(pm.getVehicleActionInfo(d2).getParcel()).isSameAs(p3);

    // vehicle must be picking up the same parcel
    boolean exception2 = false;
    try {
      d2.setRoute(asList(p1));
    } catch (final IllegalArgumentException e) {
      exception2 = true;
    }
    // assertTrue(exception2);
    assert_()
      .withFailureMessage(
        "diversion: " + diversionIsAllowed + ", delayed route changes: "
          + allowDelayedRouteChanges + ", routeAdjuster: " + routeAdjuster)
      .that(exception2)
      .isSameAs(
        routeAdjuster == RouteAdjusters.NOP
          || routeAdjuster == RouteAdjusters.DELAY_ADJUSTER
            && !allowDelayedRouteChanges);

    tick(16, 23, 10);
    assertEquals(ParcelState.DELIVERING, pm.getParcelState(p1));
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(p3));
    assertEquals(VehicleState.DELIVERING, pm.getVehicleState(d));
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(d2));

    // vehicle must be in delivering state
    boolean exception3 = false;
    try {
      d2.setRoute(asList(p1));
    } catch (final IllegalArgumentException e) {
      exception3 = true;
    }
    assertThat(exception3).isSameAs(routeAdjuster == RouteAdjusters.NOP
      || routeAdjuster == RouteAdjusters.DELAY_ADJUSTER
        && !allowDelayedRouteChanges);
  }

  /**
   * Tests whether setRoute correctly rejects too many occurrences of a parcel.
   */
  @Test
  public void setRouteTest3() {
    // too many of available parcel
    boolean exception = false;
    try {
      d.setRoute(asList(p1, p1, p1));
    } catch (final IllegalArgumentException e) {
      exception = true;
    }
    assertTrue(exception);

    // vehicle doesn't have this parcel in cargo
    d.setRoute(asList(p1));
    tick(0, 10);
    assertEquals(ParcelState.IN_CARGO, pm.getParcelState(p1));
    boolean exception2 = false;
    try {
      d2.setRoute(asList(p1));
    } catch (final IllegalArgumentException e) {
      exception2 = true;
    }
    assertThat(exception2).isSameAs(routeAdjuster == RouteAdjusters.NOP);

    // too many of incargo parcel
    boolean exception3 = false;
    try {
      d.setRoute(asList(p1, p1));
    } catch (final IllegalArgumentException e) {
      exception3 = true;
    }
    assertThat(exception3).isSameAs(routeAdjuster == RouteAdjusters.NOP);
  }

  /**
   * Checks whether delayed route changing is rejected if necessary.
   */
  @Test
  public void setRouteTest4() {
    d.setRoute(asList(p1, p1));
    tick(12, 15);
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(p1));
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(d));

    boolean exception = false;
    try {
      d.setRoute(asList(p2));
      assertEquals(asList(p2), d.newRoute.get());
    } catch (final IllegalArgumentException e) {
      exception = true;
    }
    assertThat(exception).isNotSameAs(allowDelayedRouteChanges);
  }

  /**
   * Test for: A parcel that occurs in the route has been delivered, all
   * occurrences of this parcel will be removed from the route.
   */
  @Test
  public void setRouteSafeTest1() {
    d.setRoute(asList(p1, p1));
    tick(0, 17);
    assertThat(pm.getParcelState(p1)).isSameAs(ParcelState.DELIVERED);
    assertThat(d.getRoute()).isEmpty();

    // p1 is already delivered, therefore it can not occur in the route
    boolean fail = false;
    try {
      d.setRoute(asList(p1, p2, p1, p2));
      assertThat(d.getRoute()).containsExactly(p2, p2).inOrder();
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(
        "A parcel that is already delivered can not be part of a route.");
      fail = true;
    }
    assertThat(routeAdjuster == RouteAdjusters.NOP).isEqualTo(fail);
  }

  /**
   * Test for: A parcel that occurs in the route has been picked up by another
   * vehicle, all occurrences of this parcel will be removed from the route.
   */
  @Test
  public void setRouteSafeTest2() {
    d.setRoute(asList(p1, p1));

    tick(0, 8);
    assertThat(pm.getParcelState(p1)).isSameAs(ParcelState.IN_CARGO);
    assertThat(pm.getContents(d)).containsExactly(p1);
    assertThat(d.getRoute()).containsExactly(p1);

    boolean fail = false;
    // p1 is already in cargo of d, therefore it can occur only once in the
    // route
    try {
      d.setRoute(asList(p1, p2, p1, p2));
      assertThat(d.getRoute()).containsExactly(p2, p1, p2).inOrder();
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage()).contains(
        "A parcel that is in cargo may not occur more than once in a route");
    }
    assertThat(routeAdjuster == RouteAdjusters.NOP).isEqualTo(fail);

    fail = false;
    try {
      // p1 is in another cargo and should be removed from this route
      d2.setRoute(asList(p1, p2, p1, p2));
      assertThat(d2.getRoute()).containsExactly(p2, p2).inOrder();
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(
        "A parcel that is in cargo state must be in cargo of this vehicle");
      fail = true;
    }
    assertThat(routeAdjuster == RouteAdjusters.NOP).isEqualTo(fail);
  }

  /**
   * Test for: A parcel that occurs twice in the route but has already been
   * picked up by this vehicle, the first occurrence in the route will be
   * removed.
   */
  @Test
  public void setRouteSafeTest3() {
    d.setRoute(asList(p1, p1));
    tick(0, 8);
    assertThat(pm.getParcelState(p1)).isSameAs(ParcelState.IN_CARGO);
    assertThat(d.getRoute()).containsExactly(p1);

    boolean fail = false;
    // p1 is already in cargo, therefore it can occur only once in the route
    try {
      d.setRoute(asList(p1, p2, p1, p2));
      assertThat(d.getRoute()).containsExactly(p2, p1, p2).inOrder();
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage()).contains(
        "A parcel that is in cargo may not occur more than once in a route");
    }
    assertThat(routeAdjuster == RouteAdjusters.NOP).isEqualTo(fail);

    d.setRoute(asList(p1, p2, p2));
    assertThat(d.getRoute()).containsExactly(p1, p2, p2).inOrder();
  }

  /**
   * Test for: A parcel in the route that is being picked up by another vehicle.
   */
  @Test
  public void setRouteSafeTest4() {
    d.setRoute(asList(p1, p1));
    tick(0, 7);
    assertThat(pm.getParcelState(p1)).isSameAs(ParcelState.PICKING_UP);
    assertThat(d.getRoute()).containsExactly(p1, p1);

    boolean fail = false;
    try {
      // it is being picked up by the other vehicle, so this should be ignored
      d2.setRoute(asList(p1, p1));
      assertThat(d2.getRoute()).isEmpty();
    } catch (final IllegalArgumentException e) {
      assertThat(e.getMessage()).contains(
        "When a parcel in the route is in PICKING UP state the vehicle "
          + "must also be in that state");
      fail = true;
    }
    assertThat(routeAdjuster == RouteAdjusters.NOP).isEqualTo(fail);
  }

  /**
   * Test for: A parcel in the route that is being picked up by another vehicle,
   * while the vehicle itself is also picking up another parcel.
   */
  @Test
  public void setRouteSafeTest5() {
    final Parcel p4 = Parcel.builder(new Point(1, 2), new Point(1, 5))
      .pickupTimeWindow(TimeWindow.create(minute(5), minute(25)))
      .deliveryTimeWindow(TimeWindow.create(minute(22), minute(30)))
      .serviceDuration(minute(3))
      .build();

    PDPTWTestUtil.register(rm, pm, p4);

    d.setRoute(asList(p1, p1));
    d2.setRoute(asList(p4, p4));
    tick(0, 7);
    d2.tick(TimeLapseFactory.create(minute(0), minute(7)));

    assertThat(pm.getParcelState(p1)).isSameAs(ParcelState.PICKING_UP);
    assertThat(pm.getParcelState(p4)).isSameAs(ParcelState.PICKING_UP);
    assertThat(pm.getVehicleState(d)).isSameAs(VehicleState.PICKING_UP);
    assertThat(pm.getVehicleState(d2)).isSameAs(VehicleState.PICKING_UP);
    assertThat(pm.getVehicleActionInfo(d).getParcel()).isSameAs(p1);
    assertThat(pm.getVehicleActionInfo(d2).getParcel()).isSameAs(p4);
    assertThat(d.getRoute()).containsExactly(p1, p1);
    assertThat(d2.getRoute()).containsExactly(p4, p4);

    boolean fail = false;
    try {
      d.setRoute(asList(p1, p4, p1, p4));
      assertThat(d.getRoute()).containsExactly(p1, p1);
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage()).contains(
        "A parcel in the route that is being serviced should be serviced by"
          + " this truck.");
    }
    assertThat(routeAdjuster == RouteAdjusters.NOP).isEqualTo(fail);

    fail = false;
    try {
      d2.setRoute(asList(p1, p4, p4, p1));
      assertThat(d2.getRoute()).containsExactly(p4, p4);
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage()).contains(
        "A parcel in the route that is being serviced should be serviced by"
          + " this truck.");
    }
    assertThat(routeAdjuster == RouteAdjusters.NOP).isEqualTo(fail);
  }

  /**
   * Tests that the delay adjuster adds parcels in cargo.
   */
  @Test
  public void setRouteSafeTest6() {
    d.setRoute(asList(p1, p1));
    tick(0, 8);
    assertThat(pm.getParcelState(p1)).isSameAs(ParcelState.IN_CARGO);

    d.setRoute(asList(p2, p2));
    if (routeAdjuster == RouteAdjusters.DELAY_ADJUSTER) {
      assertThat(d.getRoute()).containsExactly(p2, p2, p1).inOrder();
    } else {
      assertThat(d.getRoute()).containsExactly(p2, p2).inOrder();
    }
  }

  /**
   * Tests the NOGO event.
   */
  @Test
  public void testNogoEvent() {
    d.setRoute(asList(p2));
    tick(0, 11);
    tick(11, 12);
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    boolean exception = false;
    try {
      d.setRoute(Arrays.<Parcel>asList());
      tick(12, 13);
      if (diversionIsAllowed) {
        // route has changed, here the actual NOGO event will be sent
        assertEquals(d.waitState, d.stateMachine.getCurrentState());
        assertEquals(new LinkedList<>(), d.route);
        assertFalse(d.newRoute.isPresent());
      } else {
        // route has not yet changed
        assertEquals(d.gotoState, d.stateMachine.getCurrentState());
        assertEquals(new LinkedList<>(asList(p2)), d.route);
        assertEquals(asList(), d.newRoute.get());
      }
    } catch (final IllegalArgumentException e) {
      exception = true;
    }
    assertEquals(!allowDelayedRouteChanges && !diversionIsAllowed, exception);
  }

  /**
   * Tests the REROUTE event.
   */
  @Test
  public void testRerouteEvent() {
    d.setRoute(asList(p1));
    tick(0, 5, 20);
    assertEquals(d.waitForServiceState, d.stateMachine.getCurrentState());

    boolean exception = false;
    try {
      d.setRoute(Arrays.<Parcel>asList(p2));
      tick(12, 13);
      if (diversionIsAllowed) {
        // route has changed, here the actual REROUTE event will be sent
        assertEquals(d.gotoState, d.stateMachine.getCurrentState());
        assertEquals(new LinkedList<>(asList(p2)), d.route);
        assertFalse(d.newRoute.isPresent());
      } else {
        // route has not yet changed
        assertEquals(d.serviceState, d.stateMachine.getCurrentState());
        assertEquals(new LinkedList<>(asList(p1)), d.route);
        assertEquals(asList(p2), d.newRoute.get());
      }
    } catch (final IllegalArgumentException e) {
      exception = true;
    }
    assertEquals(!allowDelayedRouteChanges && !diversionIsAllowed, exception);
  }

  /**
   * Tests agent behavior when diversion is attempted while in the goto state.
   */
  @Test
  public void diversionTestInGotoState() {
    d.setRoute(asList(p1));
    assertEquals(Optional.absent(), d.newRoute);
    tick(2, 3);
    assertEquals(d.waitState, d.stateMachine.getCurrentState());
    assertEquals(new Point(1, 1), rm.getPosition(d));

    tick(3, 4);
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    // changing the remainder of the route is always allowed
    d.setRoute(asList(p1, p2, p1));
    assertEquals(asList(p1, p2, p1), newArrayList(d.getRoute()));
    assertEquals(Optional.absent(), d.newRoute);

    // making it empty is not always immediately allowed
    boolean exception = false;
    try {
      d.setRoute(new ArrayList<Parcel>());
    } catch (final IllegalArgumentException e) {
      exception = true;
    }

    if (diversionIsAllowed) {
      assertTrue(d.route.isEmpty());
      assertEquals(Optional.absent(), d.newRoute);
      assertFalse(exception);
    } else {
      if (allowDelayedRouteChanges) {
        assertFalse(exception);
        assertTrue(d.newRoute.isPresent());
      } else {
        assertTrue(exception);
        assertFalse(d.newRoute.isPresent());
      }
      // no diversion allowed, no change yet
      assertEquals(new LinkedList<>(asList(p1, p2, p1)), d.route);
    }

    // change it back
    d.setRoute(asList(p1));

    // changing the first destination in the route is not always immediately
    // allowed
    boolean exception2 = false;
    try {
      d.setRoute(asList(p2));
    } catch (final IllegalArgumentException e) {
      exception2 = true;
    }
    if (diversionIsAllowed) {
      assertEquals(new LinkedList<>(asList(p2)), d.route);
      assertEquals(Optional.absent(), d.newRoute);
      assertFalse(exception2);
    } else {
      assertEquals(new LinkedList<>(asList(p1)), d.route);

      if (allowDelayedRouteChanges) {
        assertFalse(exception2);
        assertTrue(d.newRoute.isPresent());
      } else {
        assertTrue(exception2);
        assertFalse(d.newRoute.isPresent());
      }
    }

    tick(13, 14);
    tick(14, 20);

    if (diversionIsAllowed) {
      // in case diversion is allowed, check that the vehicle actually
      // diverted to this parcel, and serviced it.
      assertEquals(newHashSet(p2), pm.getContents(d));
    } else {
      if (allowDelayedRouteChanges) {
        // pickup both
        assertEquals(newHashSet(p1, p2), pm.getContents(d));
      } else {
        // continue moving to p1 and pickup
        assertEquals(newHashSet(p1), pm.getContents(d));
      }
    }
    if (!exception2 && routeAdjuster == RouteAdjusters.DELAY_ADJUSTER
      && !diversionIsAllowed) {
      assertThat(d.route).containsExactly(p1);
    } else {
      assertThat(d.route).isEmpty();
    }
  }

  /**
   * Test diversion with REROUTE events.
   */
  @Test
  public void diversionTestInGotoState2() {
    final ListenerEventHistory leh = new ListenerEventHistory();
    d.stateMachine.getEventAPI().addListener(leh,
      StateMachineEvent.STATE_TRANSITION);
    assertEquals(0, leh.getHistory().size());
    assertFalse(d.gotoState.destination.isPresent());
    assertFalse(d.gotoState.prevDestination.isPresent());

    d.setRoute(asList(p1));
    assertEquals(diversionIsAllowed, d.isDiversionAllowed());
    assertEquals(Optional.absent(), d.newRoute);
    tick(5, 6);
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());
    assertEquals(1, leh.getHistory().size());
    assertEquals(p1, d.gotoState.getDestination());
    assertFalse(d.gotoState.prevDestination.isPresent());

    @SuppressWarnings("unchecked")
    final StateTransitionEvent<StateEvent, RouteFollowingVehicle> ev1 =
      (StateTransitionEvent<StateEvent, RouteFollowingVehicle>) leh
        .getHistory().get(0);
    assertEquals(DefaultEvent.GOTO, ev1.trigger);
    assertEquals(d.waitState, ev1.previousState);
    assertEquals(d.gotoState, ev1.newState);

    assertEquals(d.gotoState, d.stateMachine.getCurrentState());
    assertEquals(p1, d.getRoute().iterator().next());

    boolean fail = false;
    try {
      d.setRoute(asList(p2));
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertEquals(!diversionIsAllowed && !allowDelayedRouteChanges, fail);

    tick(4, 5);
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    if (diversionIsAllowed) {
      assertEquals(2, leh.getHistory().size());
      @SuppressWarnings("unchecked")
      final StateTransitionEvent<StateEvent, RouteFollowingVehicle> ev2 =
        (StateTransitionEvent<StateEvent, RouteFollowingVehicle>) leh
          .getHistory().get(1);
      assertEquals(DefaultEvent.REROUTE, ev2.trigger);
      assertEquals(d.gotoState, ev2.previousState);
      assertEquals(d.gotoState, ev2.newState);

      assertEquals(p2, d.gotoState.getDestination());
      assertEquals(p1, d.gotoState.getPreviousDestination());
    } else {
      assertEquals(1, leh.getHistory().size());
      assertEquals(p1, d.gotoState.getDestination());
      assertFalse(d.gotoState.prevDestination.isPresent());
    }
  }

  /**
   * Tests agent behavior when diversion is attempted in the service state.
   */
  @Test
  public void diversionTestInServiceState() {
    d.setRoute(asList(p1));
    assertEquals(Optional.absent(), d.newRoute);

    // too early
    tick(2, 3);
    assertEquals(d.waitState, d.stateMachine.getCurrentState());

    // start moving towards
    tick(3, 4);
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    tick(4, 5);
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    // start servicing
    tick(5, 6);
    assertEquals(d.serviceState, d.stateMachine.getCurrentState());

    // during servicing, the first part of the route can not be changed

    d.setRoute(asList(p1, p2, p1));
    assertEquals(new LinkedList<>(asList(p1, p2, p1)), d.route);
    assertEquals(Optional.absent(), d.newRoute);
    d.setRoute(asList(p1));
    assertEquals(new LinkedList<>(asList(p1)), d.route);
    assertEquals(Optional.absent(), d.newRoute);

    boolean excep = false;
    try {
      d.setRoute(asList(p2));
      assertEquals(asList(p2), d.newRoute.get());
      assertEquals(new LinkedList<>(asList(p1)), d.route);
    } catch (final IllegalArgumentException e) {
      excep = true;
    }
    assertThat(excep).isNotSameAs(allowDelayedRouteChanges);

    tick(6, 7);
    assertEquals(d.serviceState, d.stateMachine.getCurrentState());
    tick(7, 8);
    assertEquals(d.waitState, d.stateMachine.getCurrentState());
    assertEquals(asList(p1), pm.getContents(d).asList());

    // it is still too early to go to p2, but the route should be updated
    tick(8, 9);
    assertEquals(d.waitState, d.stateMachine.getCurrentState());
    if (allowDelayedRouteChanges) {
      if (routeAdjuster == RouteAdjusters.DELAY_ADJUSTER) {
        assertEquals(new LinkedList<>(asList(p2, p1)), d.route);
      } else {
        assertEquals(new LinkedList<>(asList(p2)), d.route);
      }
      assertEquals(Optional.absent(), d.newRoute);
    } else {
      assertEquals(new LinkedList<>(asList()), d.route);
      assertEquals(Optional.absent(), d.newRoute);
    }
  }

  /**
   * Tests agent behavior when diversion is attempted in the wait state. This is
   * always allowed.
   */
  @Test
  public void diversionTestInWaitState() {
    assertEquals(d.waitState, d.stateMachine.getCurrentState());
    d.setRoute(asList(p1));
    assertEquals(Optional.absent(), d.newRoute);
    assertEquals(new LinkedList<>(asList(p1)), d.route);

    d.setRoute(asList(p1, p2, p1, p2));
    assertEquals(Optional.absent(), d.newRoute);
    assertEquals(new LinkedList<>(asList(p1, p2, p1, p2)), d.route);

    d.setRoute(asList(p2, p2, p1, p1));
    assertEquals(Optional.absent(), d.newRoute);
    assertEquals(new LinkedList<>(asList(p2, p2, p1, p1)), d.route);
  }

  /**
   * Tests agent behavior when diversion is attempted in the wait at service
   * state.
   */
  @Test
  public void diversionTestInWaitAtServiceState() {
    d.setRoute(asList(p1));
    assertEquals(Optional.absent(), d.newRoute);

    // too early
    tick(2, 3);
    assertEquals(d.waitState, d.stateMachine.getCurrentState());

    // start moving towards
    tick(3, 4);
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    tick(3, 4);
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    tick(3, 4);
    assertEquals(d.waitForServiceState, d.stateMachine.getCurrentState());

    boolean fails = false;
    try {
      d.setRoute(Collections.<Parcel>emptyList());
      if (!allowDelayedRouteChanges && !diversionIsAllowed) {
        assertTrue(d.getRoute().isEmpty());
      }
    } catch (final IllegalArgumentException e) {
      fails = true;
    }
    assertThat(allowDelayedRouteChanges || diversionIsAllowed)
      .isNotSameAs(fails);

    if (allowDelayedRouteChanges && !diversionIsAllowed) {
      assertEquals(1, d.getRoute().size());
    }

    tick(4, 5);
    if (diversionIsAllowed) {
      assertEquals(d.waitState, d.stateMachine.getCurrentState());
    } else {
      assertEquals(d.waitForServiceState, d.stateMachine.getCurrentState());
    }
  }

  /**
   *
   */
  @Test
  public void brokenWaitState() {
    d.setRoute(asList(p1));
    tick(4, 5, 30);
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    if (allowDelayedRouteChanges) {
      d.setRoute(Arrays.<Parcel>asList());
      rm.moveTo(d, p1, time(minute(0), minute(10)));
      assertTrue(rm.equalPosition(d, p1));
      pm.pickup(d, p1, time(minute(16), minute(17)));
      assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(d));
      if (diversionIsAllowed) {
        boolean exception = false;
        try {
          tick(5, 6);
        } catch (final IllegalStateException e) {
          exception = true;
        }
        assertTrue(exception);
      }
    }
  }

  /**
   * Tests a check what happens if someone else is picking up a parcel that a
   * vehicle wants to pick up.
   */
  @Test
  public void testParcelOwnerShip() {
    d.setRoute(asList(p1));
    d2.setRoute(asList(p1));
    tick(0, 7);
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(p1));

    boolean exception = false;
    try {
      d2.checkCurrentParcelOwnership();
    } catch (final IllegalStateException e) {
      exception = true;
    }
    assertTrue(exception);
  }

  /**
   * Test the correctness of end of day definition.
   */
  @Test
  public void isEndOfDayTest() {
    // travel time = 0
    rm.removeObject(d);
    rm.addObjectAtSamePosition(d, depot);
    assertFalse(d.isEndOfDay(time(minute(0), minute(1))));
    assertFalse(d.isEndOfDay(time(minute(29), minute(30))));
    assertTrue(d.isEndOfDay(time(minute(30), minute(31))));

    // travel time = 4 minutes from the depot at (3,5)
    rm.removeObject(d);
    rm.addObjectAt(d, new Point(5, 5));
    assertFalse(d.isEndOfDay(time(minute(25), minute(26))));
    assertTrue(d.isEndOfDay(time(minute(26), minute(27))));

    // travel time = 4.5 minutes from the depot
    rm.removeObject(d);
    rm.addObjectAt(d, new Point(5, 5.5));
    assertFalse(d.isEndOfDay(time(minute(24), minute(25))));
    assertTrue(d.isEndOfDay(time(minute(25), minute(26))));
  }

  /**
   * Test illegal argument: transition state.
   */
  @Test(expected = IllegalArgumentException.class)
  public void isTooEarlyTransitionStateFail() {
    d.setRoute(asList(p1));
    tick(3, 6);
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(p1));
    d.isTooEarly(p1, time(2));
  }

  /**
   * Test illegal argument: delivered state.
   */
  @Test(expected = IllegalArgumentException.class)
  public void isTooEarlyDeliveredStateFail() {
    d.setRoute(asList(p1, p1));
    tick(0, 60);
    assertEquals(ParcelState.DELIVERED, pm.getParcelState(p1));
    d.isTooEarly(p1, time(2));
  }

  /**
   * Test method when parcel is already in the available state.
   */
  @Test
  public void isTooEarlyAvailableState() {
    tick(5, 6);
    assertFalse(d.isTooEarly(p1, time(2)));
  }

  /**
   * Arrive too early, pickup at start of next tick.
   */
  @Test
  public void tooEarlyTest1() {
    final Parcel p4 = Parcel
      .builder(new Point(1, 2), new Point(1, 4))
      .pickupTimeWindow(TimeWindow.create(minute(5) + second(30), minute(15)))
      .deliveryTimeWindow(TimeWindow.create(minute(16), minute(30)))
      .pickupDuration(minute(3))
      .deliveryDuration(minute(1))
      .build();

    PDPTWTestUtil.register(rm, pm, p4);

    d.setRoute(asList(p4));

    // we are too early, don't move
    tick(0, 1);
    tick(1, 2);
    tick(2, 3);
    assertEquals(d.waitState, d.stateMachine.getCurrentState());
    assertEquals(new Point(1, 1), rm.getPosition(d));

    // we can go
    tick(3, 4);
    assertEquals(new Point(1, 1.5), rm.getPosition(d));
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    tick(4, 5);
    // we have arrived, but no time left to start pickup
    assertEquals(new Point(1, 2.0), rm.getPosition(d));
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    // check that pickup occurs exactly at opening of time window
    pm.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        assertEquals(minute(5) + second(30), ((PDPModelEvent) e).time);
      }
    }, PDPModelEventType.START_PICKUP);

    // now start pickup
    tick(5, 6);
    assertEquals(d.serviceState, d.stateMachine.getCurrentState());
  }

  /**
   * Arrive too early, pickup in same tick.
   */
  @Test
  public void tooEarlyTest2() {
    final Parcel p4 = Parcel
      .builder(new Point(1, 2.2), new Point(1, 4))
      .pickupTimeWindow(TimeWindow.create(minute(5) + second(30), minute(15)))
      .deliveryTimeWindow(TimeWindow.create(minute(16), minute(30)))
      .pickupDuration(minute(3))
      .deliveryDuration(minute(1))
      .build();

    PDPTWTestUtil.register(rm, pm, p4);

    d.setRoute(asList(p4));

    // we are too early, don't move
    tick(0, 1);
    tick(1, 2);
    tick(2, 3);
    assertEquals(d.waitState, d.stateMachine.getCurrentState());
    assertEquals(new Point(1, 1), rm.getPosition(d));

    // we can go
    tick(3, 4);
    assertEquals(new Point(1, 1.5), rm.getPosition(d));
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    tick(4, 5);
    assertEquals(new Point(1, 2.0), rm.getPosition(d));
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    // check that pickup occurs exactly at opening of time window
    pm.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        assertEquals(minute(5) + second(30), ((PDPModelEvent) e).time);
      }
    }, PDPModelEventType.START_PICKUP);

    // now start pickup
    tick(5, 6);
    assertEquals(d.serviceState, d.stateMachine.getCurrentState());
  }

  /**
   * Arrive too early with some time left, pickup in middle of next tick.
   */
  @Test
  public void tooEarlyTest3() {
    final Parcel p4 = Parcel
      .builder(new Point(1, 1.99), new Point(1, 4))
      .pickupTimeWindow(TimeWindow.create(minute(5) + second(30), minute(15)))
      .deliveryTimeWindow(TimeWindow.create(minute(16), minute(30)))
      .pickupDuration(minute(3))
      .deliveryDuration(minute(1))
      .build();

    PDPTWTestUtil.register(rm, pm, p4);

    d.setRoute(asList(p4));

    // we are too early, don't move
    tick(0, 1);
    tick(1, 2);
    tick(2, 3);
    assertEquals(d.waitState, d.stateMachine.getCurrentState());
    assertEquals(new Point(1, 1), rm.getPosition(d));

    // we can go
    tick(3, 4);
    assertEquals(new Point(1, 1.5), rm.getPosition(d));
    assertEquals(d.gotoState, d.stateMachine.getCurrentState());

    // we arrive with some time left, so we transition to service state
    tick(4, 5);
    assertEquals(new Point(1, 1.99), rm.getPosition(d));
    assertEquals(d.waitForServiceState, d.stateMachine.getCurrentState());

    // check that pickup occurs exactly at opening of time window
    pm.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        assertEquals(minute(5) + second(30), ((PDPModelEvent) e).time);
      }
    }, PDPModelEventType.START_PICKUP);

    // now start pickup
    tick(5, 6);
    assertEquals(d.serviceState, d.stateMachine.getCurrentState());
    tick(6, 7);

  }

  /**
   * Tests a subclass that changes the state machine.
   */
  @Test
  public void testExtension() {
    final VehicleDTO v = VehicleDTO.builder()
      .startPosition(new Point(1, 1))
      .speed(30d)
      .capacity(1)
      .availabilityTimeWindow(TimeWindow.create(0, minute(30)))
      .build();
    final SubVehicle vehicle = new SubVehicle(v, allowDelayedRouteChanges);
    d = vehicle;

    PDPTWTestUtil.register(rm, pm, vehicle);
    tick(0, 1);

    assertEquals(vehicle.waitState, vehicle.stateMachine.getCurrentState());
    vehicle.stateMachine.handle(ExtraEvent.TEST_EVENT, vehicle);
    assertEquals(vehicle.extraState, vehicle.stateMachine.getCurrentState());
    tick(1, 2);
    assertEquals(vehicle.waitState, vehicle.stateMachine.getCurrentState());
  }

  /**
   * Tests correct exception when calling destination at wrong time.
   */
  @Test(expected = IllegalStateException.class)
  public void getDestinationFail() {
    d.gotoState.getDestination();
  }

  /**
   * Tests correct exception when calling prev destination at wrong time.
   */
  @Test(expected = IllegalStateException.class)
  public void getpreviousDestinationFail() {
    d.gotoState.getPreviousDestination();
  }

  static long minute(long minutes) {
    return minutes * 60000;
  }

  static long second(long sec) {
    return sec * 1000;
  }

}
