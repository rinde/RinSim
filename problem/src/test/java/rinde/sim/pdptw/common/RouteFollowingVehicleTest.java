/**
 * 
 */
package rinde.sim.pdptw.common;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static javax.measure.unit.NonSI.KILOMETERS_PER_HOUR;
import static javax.measure.unit.SI.KILOMETER;
import static javax.measure.unit.SI.MILLI;
import static javax.measure.unit.SI.SECOND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static rinde.sim.core.TimeLapseFactory.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import rinde.sim.core.model.pdp.twpolicy.TardyAllowedPolicy;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.util.TimeWindow;

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
  private RouteFollowingVehicle d;
  private DefaultParcel p1;
  private DefaultParcel p2;
  private DefaultDepot depot;
  private final boolean diversionIsAllowed;

  /**
   * Create test.
   * @param allowDiversion Is vehicle diversion allowed.
   */
  @SuppressWarnings("null")
  public RouteFollowingVehicleTest(boolean allowDiversion) {
    diversionIsAllowed = allowDiversion;
  }

  /**
   * @return parameters for constructor.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { { true }, { false } });
  }

  /**
   * Set up a simple simulation with models, one vehicle, two parcels and one
   * depot.
   */
  @Before
  public void setUp() {
    rm = new PDPRoadModel(new PlaneRoadModel(new Point(0, 0),
        new Point(10, 10), 30d), diversionIsAllowed);

    pm = new PDPModel(new TardyAllowedPolicy());
    @SuppressWarnings("unchecked")
    final ModelProvider mp = new TestModelProvider(new ArrayList<Model<?>>(
        asList(rm, pm)));
    rm.registerModelProvider(mp);
    pm.registerModelProvider(mp);

    final VehicleDTO v = new VehicleDTO(new Point(1, 1), 30, 1, new TimeWindow(
        0, minute(30)));
    d = new RouteFollowingVehicle(v, MILLI(SECOND), KILOMETERS_PER_HOUR,
        KILOMETER);

    p1 = new DefaultParcel(new ParcelDTO(new Point(1, 2), new Point(1, 4), //
        new TimeWindow(minute(5), minute(15)), // pickup tw
        new TimeWindow(minute(16), minute(30)),// delivery tw
        0, 0, minute(3), minute(1)));

    p2 = new DefaultParcel(new ParcelDTO(new Point(1, 3), new Point(1, 5), //
        new TimeWindow(minute(15) + 10, minute(25)), // pickup tw
        new TimeWindow(minute(22) + 10, minute(30)),// delivery tw
        0, 0, minute(0), minute(3)));

    depot = new DefaultDepot(new Point(3, 5));

    PDPTWTestUtil.register(rm, pm, depot, d, p1, p2);
  }

  /**
   * Test definition of isTooEarly.
   */
  @Test
  public void testIsTooEarly() {
    // travelling 1km at 30km/h should take 2 minutes
    assertEquals(minute(2), d.computeTravelTimeTo(new Point(1, 2)));

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

  void tick(long beginMinute, long endMinute) {
    final TimeLapse tl = time(minute(beginMinute), minute(endMinute));
    pm.tick(tl);
    d.tick(tl);
  }

  /**
   * Tests the proper execution of a route, including having to wait.
   */
  @Test
  public void test() {
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
    assertEquals(0d, Point.distance(new Point(1, 3.5), rm.getPosition(d)), EPSILON);
    assertEquals(newHashSet(p1, p2), pm.getContents(d));

    // move
    tick(16, 17);
    assertEquals(0d, Point.distance(new Point(1, 4d), rm.getPosition(d)), EPSILON);

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
    assertEquals(minute(4), d.computeTravelTimeTo(new Point(3, 5)));

    // don't do anything yet
    tick(25, 26);
    assertEquals(new Point(1, 5), rm.getPosition(d));

    // let's go home!
    tick(26, 27);
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
   * Tests whether the agent supports diversion (if the underlying models
   * support it).
   */
  @Test
  public void diversionTest() {
    d.setRoute(asList(p1, p2));
    tick(2, 3);
    assertEquals(new Point(1, 1), rm.getPosition(d));

    tick(3, 4);
    d.setRoute(asList(p2, p1));

    boolean error = false;
    try {
      tick(13, 14);
      tick(14, 20);
      // in case diversion is allowed, check that the vehicle actually
      // diverted to this parcel, and serviced it.
      assertEquals(newHashSet(p2), pm.getContents(d));
    } catch (final IllegalArgumentException e) {
      error = true;
    }
    assertEquals(!diversionIsAllowed, error);
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

  static long minute(long minutes) {
    return minutes * 60000;
  }
}
