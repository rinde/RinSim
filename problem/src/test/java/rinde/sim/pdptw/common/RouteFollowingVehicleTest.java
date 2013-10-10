/**
 * 
 */
package rinde.sim.pdptw.common;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static rinde.sim.core.TimeLapseFactory.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.TestModelProvider;
import rinde.sim.core.model.pdp.DefaultPDPModel;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.PDPModelEventType;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import rinde.sim.core.model.pdp.PDPModelEvent;
import rinde.sim.core.model.pdp.twpolicy.TardyAllowedPolicy;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.pdptw.common.RouteFollowingVehicle.StateEvent;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Optional;

/**
 * Tests for {@link RouteFollowingVehicle}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
@RunWith(value = Parameterized.class)
public class RouteFollowingVehicleTest {

  // EPSILON in km: maximum allowed deviation is 10 cm
  static final double EPSILON = 0.0001;

  private PDPRoadModel rm;
  private PDPModel pm;
  private RouteFollowingVehicle d, d2;
  private DefaultParcel p1, p2, p3;
  private DefaultDepot depot;
  private final boolean diversionIsAllowed;
  private final boolean allowDelayedRouteChanges;

  /**
   * Create test.
   * @param allowDiversion Is vehicle diversion allowed.
   * @param allowDelayedRouteChange Are delayed route changes allowed.
   */
  @SuppressWarnings("null")
  public RouteFollowingVehicleTest(boolean allowDiversion,
      boolean allowDelayedRouteChange) {
    diversionIsAllowed = allowDiversion;
    allowDelayedRouteChanges = allowDelayedRouteChange;
    StateEvent.values();
    StateEvent.valueOf("GOTO");
  }

  /**
   * @return parameters for constructor.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { { true, true }, { true, false },
        { false, true }, { false, false } });
  }

  /**
   * Set up a simple simulation with models, one vehicle, two parcels and one
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
    rm = new PDPRoadModel(new PlaneRoadModel(new Point(0, 0),
        new Point(10, 10), 30d), diversionIsAllowed);

    pm = new DefaultPDPModel(new TardyAllowedPolicy());
    @SuppressWarnings("unchecked")
    final ModelProvider mp = new TestModelProvider(new ArrayList<Model<?>>(
        asList(rm, pm)));
    rm.registerModelProvider(mp);
    pm.registerModelProvider(mp);

    final VehicleDTO v = new VehicleDTO(new Point(1, 1), 30, 1, new TimeWindow(
        0, minute(30)));
    d = new RouteFollowingVehicle(v, allowDelayedRouteChanges);
    d2 = new RouteFollowingVehicle(v, allowDelayedRouteChanges);

    p1 = new DefaultParcel(new ParcelDTO(new Point(1, 2), new Point(1, 4), //
        new TimeWindow(minute(5), minute(15)), // pickup tw
        new TimeWindow(minute(16), minute(30)),// delivery tw
        0, 0, minute(3), minute(1)));

    p2 = new DefaultParcel(new ParcelDTO(new Point(1, 3), new Point(1, 5), //
        new TimeWindow(minute(15) + 10, minute(25)), // pickup tw
        new TimeWindow(minute(22) + 10, minute(30)),// delivery tw
        0, 0, minute(0), minute(3)));

    p3 = new DefaultParcel(new ParcelDTO(new Point(1, 3), new Point(1, 5), //
        new TimeWindow(minute(15) + 10, minute(25)), // pickup tw
        new TimeWindow(minute(22) + 10, minute(30)),// delivery tw
        0, 0, minute(3), minute(3)));

    depot = new DefaultDepot(new Point(3, 5));

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
    PDPTWTestUtil.register(rm, pm, depot, new DefaultDepot(new Point(3, 5)), d);
  }

  /**
   * Test definition of isTooEarly.
   */
  @Test
  public void testIsTooEarly() {
    // travelling 1km at 30km/h should take 2 minutes
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

  void tick(long beginMinute, long endMinute, long consumeSeconds) {
    final TimeLapse tl = time(minute(beginMinute), minute(endMinute));
    if (consumeSeconds > 0) {
      tl.consume(consumeSeconds * 1000);
    }
    pm.tick(tl);
    d.tick(tl);
    assertSame(tl, d.getCurrentTime());
    assertFalse(tl.hasTimeLeft());
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
    assertTrue(exception);
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
    assertTrue(exception);

    d2.setRoute(asList(p3));
    d2.tick(time(minute(10), minute(16)));
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(p1));
    assertEquals(ParcelState.PICKING_UP, pm.getParcelState(p3));
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(d));
    assertEquals(VehicleState.PICKING_UP, pm.getVehicleState(d2));

    // vehicle must be picking up the same parcel
    boolean exception2 = false;
    try {
      d2.setRoute(asList(p1));
    } catch (final IllegalArgumentException e) {
      exception2 = true;
    }
    assertTrue(exception2);

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
    assertTrue(exception3);
  }

  /**
   * Tests whether setRoute correctly rejects too many occurences of a parcel.
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
    assertTrue(exception2);

    // too many of incargo parcel
    boolean exception3 = false;
    try {
      d.setRoute(asList(p1, p1));
    } catch (final IllegalArgumentException e) {
      exception3 = true;
    }
    assertTrue(exception3);
  }

  /**
   * Checks wheter delayed route changing is rejected if neccessary.
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
    assertTrue(exception != allowDelayedRouteChanges);
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
      d.setRoute(Arrays.<DefaultParcel> asList());
      tick(12, 13);
      if (diversionIsAllowed) {
        // route has changed, here the actual NOGO event will be sent
        assertEquals(d.waitState, d.stateMachine.getCurrentState());
        assertEquals(asList(), d.route);
        assertFalse(d.newRoute.isPresent());
      } else {
        // route has not yet changed
        assertEquals(d.gotoState, d.stateMachine.getCurrentState());
        assertEquals(asList(p2), d.route);
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
      d.setRoute(Arrays.<DefaultParcel> asList(p2));
      tick(12, 13);
      if (diversionIsAllowed) {
        // route has changed, here the actual REROUTE event will be sent
        assertEquals(d.gotoState, d.stateMachine.getCurrentState());
        assertEquals(asList(p2), d.route);
        assertFalse(d.newRoute.isPresent());
      } else {
        // route has not yet changed
        assertEquals(d.serviceState, d.stateMachine.getCurrentState());
        assertEquals(asList(p1), d.route);
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
      d.setRoute(new ArrayList<DefaultParcel>());
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
      assertEquals(asList(p1, p2, p1), d.route);
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
      assertEquals(asList(p2), d.route);
      assertEquals(Optional.absent(), d.newRoute);
      assertFalse(exception2);
    } else {
      assertEquals(asList(p1), d.route);

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
    assertTrue(d.route.isEmpty());
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
    assertEquals(asList(p1, p2, p1), d.route);
    assertEquals(Optional.absent(), d.newRoute);
    d.setRoute(asList(p1));
    assertEquals(asList(p1), d.route);
    assertEquals(Optional.absent(), d.newRoute);

    boolean excep = false;
    try {
      d.setRoute(asList(p2));
      assertEquals(asList(p2), d.newRoute.get());
      assertEquals(asList(p1), d.route);
    } catch (final IllegalArgumentException e) {
      excep = true;
    }
    assertTrue(excep != allowDelayedRouteChanges);

    tick(6, 7);
    assertEquals(d.serviceState, d.stateMachine.getCurrentState());
    tick(7, 8);
    assertEquals(d.serviceState, d.stateMachine.getCurrentState());
    assertEquals(asList(p1), pm.getContents(d).asList());

    // it is still too early to go to p2, but the route should be updated
    tick(8, 9);
    assertEquals(d.waitState, d.stateMachine.getCurrentState());
    if (excep) {
      assertEquals(asList(), d.route);
      assertEquals(Optional.absent(), d.newRoute);
    } else {
      assertEquals(asList(p2), d.route);
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
    assertEquals(asList(p1), d.route);

    d.setRoute(asList(p1, p2, p1, p2));
    assertEquals(Optional.absent(), d.newRoute);
    assertEquals(asList(p1, p2, p1, p2), d.route);

    d.setRoute(asList(p2, p2, p1, p1));
    assertEquals(Optional.absent(), d.newRoute);
    assertEquals(asList(p2, p2, p1, p1), d.route);
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
      d.setRoute(Arrays.<DefaultParcel> asList());
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
    final DefaultParcel p4 = new DefaultParcel(new ParcelDTO(new Point(1, 2),
        new Point(1, 4), //
        new TimeWindow(minute(5) + second(30), minute(15)), // pickup tw
        new TimeWindow(minute(16), minute(30)),// delivery tw
        0, 0, minute(3), minute(1)));

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
    final DefaultParcel p4 = new DefaultParcel(new ParcelDTO(new Point(1, 2.2),
        new Point(1, 4), //
        new TimeWindow(minute(5) + second(30), minute(15)), // pickup tw
        new TimeWindow(minute(16), minute(30)),// delivery tw
        0, 0, minute(3), minute(1)));

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
    final DefaultParcel p4 = new DefaultParcel(new ParcelDTO(
        new Point(1, 1.99), new Point(1, 4), //
        new TimeWindow(minute(5) + second(30), minute(15)), // pickup tw
        new TimeWindow(minute(16), minute(30)),// delivery tw
        0, 0, minute(3), minute(1)));

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

  static long minute(long minutes) {
    return minutes * 60000;
  }

  static long second(long sec) {
    return sec * 1000;
  }
}
