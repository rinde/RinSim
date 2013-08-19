/**
 * 
 */
package rinde.sim.pdptw.central;

import static com.google.common.collect.Lists.newLinkedList;
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

import org.junit.Before;
import org.junit.Test;

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
import rinde.sim.pdptw.central.Central.RemoteDriver;
import rinde.sim.pdptw.common.DefaultDepot;
import rinde.sim.pdptw.common.DefaultParcel;
import rinde.sim.pdptw.common.PDPRoadModel;
import rinde.sim.pdptw.common.PDPTWTestUtil;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.common.VehicleDTO;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class DriverTest {

    // EPSILON in km: maximum allowed deviation is 10 cm
    static final double EPSILON = 0.0001;

    protected PDPRoadModel rm;
    protected PDPModel pm;
    protected RemoteDriver d;
    protected DefaultParcel p1;
    protected DefaultParcel p2;

    @Before
    public void setUp() {
        rm =
                new PDPRoadModel(new PlaneRoadModel(new Point(0, 0), new Point(
                        10, 10), true, 30), false);

        pm = new PDPModel(new TardyAllowedPolicy());
        @SuppressWarnings("unchecked")
        final ModelProvider mp =
                new TestModelProvider(new ArrayList<Model<?>>(asList(rm, pm)));
        rm.registerModelProvider(mp);
        pm.registerModelProvider(mp);

        final VehicleDTO v =
                new VehicleDTO(new Point(1, 1), 30, 1, new TimeWindow(0,
                        minute(30)));
        d = new RemoteDriver(v, MILLI(SECOND), KILOMETERS_PER_HOUR, KILOMETER);

        p1 = new DefaultParcel(new ParcelDTO(new Point(1, 2), new Point(1, 4), //
                new TimeWindow(minute(5), minute(15)), // pickup tw
                new TimeWindow(minute(16), minute(30)),// delivery tw
                0, 0, minute(3), minute(1)));

        p2 = new DefaultParcel(new ParcelDTO(new Point(1, 3), new Point(1, 5), //
                new TimeWindow(minute(15) + 10, minute(25)), // pickup tw
                new TimeWindow(minute(22) + 10, minute(30)),// delivery tw
                0, 0, minute(0), minute(3)));

        final DefaultDepot depot = new DefaultDepot(new Point(3, 5));

        PDPTWTestUtil.register(rm, pm, depot, d, p1, p2);
    }

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

    @Test
    public void test() {
        d.setRoute(newLinkedList(asList(p1, p2, p1)));

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

        d.setRoute(newLinkedList(asList(p2)));

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

    static long minute(long minutes) {
        return minutes * 60000;
    }

}
