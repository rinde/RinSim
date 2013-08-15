/**
 * 
 */
package rinde.sim.problem.common;

import static com.google.common.collect.Lists.newLinkedList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.TimeLapseFactory;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class PDPRoadModelTest {

    DefaultParcel dp1;
    DefaultParcel dp2;
    DefaultParcel dp3;
    DefaultDepot depot;

    DefaultVehicle dv1;
    DefaultVehicle dv2;

    PDPRoadModel rm;

    static final TimeLapse TIME = TimeLapseFactory.create(0, 1);

    static final TimeWindow DEFAULT_TW = new TimeWindow(0, 100);

    @Before
    public void setUp() {
        rm =
                new PDPRoadModel(new PlaneRoadModel(new Point(0, 0), new Point(
                        10, 10), false, 0.1), false);
        dp1 = create(new Point(1, 0), new Point(0, 7));
        dp2 = create(new Point(5, 0), new Point(0, 5));
        dp3 = create(new Point(1, 0), new Point(0, 6));
        for (final DefaultParcel dp : asList(dp1, dp2, dp3)) {
            rm.addObjectAt(dp, dp.dto.pickupLocation);
        }

        final Point depotLocation = new Point(5, 5);
        depot = new DefaultDepot(depotLocation);
        rm.addObjectAt(depot, depotLocation);

        dv1 = new TestVehicle(new Point(0, 0));
        dv2 = new TestVehicle(new Point(0, 0));
        for (final DefaultVehicle tv : asList(dv1, dv2)) {
            rm.addObjectAt(tv, tv.getDTO().startPosition);
        }

        // to satisfy coverage tool
        PDPRoadModel.DestType.DEPOT.toString();
        PDPRoadModel.DestType.valueOf("DEPOT").toString();
    }

    @Test
    public void test1() {
        assertNull(rm.getDestinationToParcel(dv1));
        rm.moveTo(dv1, dp1, time(7));
        assertEquals(dp1, rm.getDestinationToParcel(dv1));

        rm.moveTo(dv1, dp1, time(4));
        assertNull(rm.getDestinationToParcel(dv1));

        rm.removeObject(dp1);
        rm.moveTo(dv1, dp1, time(1));
        assertEquals(dp1, rm.getDestinationToParcel(dv1));

        rm.moveTo(dv1, dp1, time(80));
        assertNull(rm.getDestinationToParcel(dv1));

        rm.moveTo(dv1, dp2, time(50));
        assertEquals(dp2, rm.getDestinationToParcel(dv1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void divertFromDest() {
        rm.moveTo(dv1, dp1, time(7));
        rm.moveTo(dv1, dp2, time(4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void divertToSameBeforePickup() {
        rm.moveTo(dv1, dp1, time(7));
        rm.moveTo(dv1, dp1, time(4));
        assertEquals(rm.getPosition(dv1), rm.getPosition(dp1));
        rm.moveTo(dv1, dp1, time(80));
    }

    @Test(expected = IllegalArgumentException.class)
    public void divertToOtherBeforePickup() {
        rm.moveTo(dv1, dp1, time(7));
        rm.moveTo(dv1, dp1, time(4));
        assertEquals(rm.getPosition(dv1), rm.getPosition(dp1));
        rm.moveTo(dv1, dp2, time(80));
    }

    @Test(expected = IllegalArgumentException.class)
    public void divertToOtherBeforeDelivery() {
        rm.moveTo(dv1, dp1, time(10));
        assertEquals(rm.getPosition(dv1), rm.getPosition(dp1));
        rm.removeObject(dp1);
        rm.moveTo(dv1, dp1, time(1));
        rm.moveTo(dv1, dp2, time(80));
    }

    @Test(expected = IllegalArgumentException.class)
    public void attemptVisitFor3rdTime() {
        // first pickup
        rm.moveTo(dv1, dp1, time(11));
        rm.removeObject(dp1);
        // then deliver
        rm.moveTo(dv1, dp1, time(80));
        // 3rd time is not allowed!
        rm.moveTo(dv1, dp1, time(80));
    }

    @Test(expected = IllegalArgumentException.class)
    public void attemptVisitFor3rdTime2() {
        // first pickup
        rm.moveTo(dv1, dp1, time(11));
        assertTrue(rm.equalPosition(dv1, dp1));
        rm.removeObject(dp1);
        // then deliver
        rm.moveTo(dv1, dp1, time(80));
        assertEquals(rm.getPosition(dv1), dp1.getDestination());

        rm.moveTo(dv1, dp2, time(180));
        assertTrue(rm.equalPosition(dv1, dp2));
        rm.removeObject(dp2);

        // it is not allowed to revisit this position
        rm.moveTo(dv1, dp1, time(80));
    }

    @Test(expected = IllegalArgumentException.class)
    public void twoVehiclesGoToSame() {
        rm.moveTo(dv1, dp1, time(1));
        rm.moveTo(dv2, dp1, time(1));
    }

    @Test
    public void moveToDepot() {
        rm.moveTo(dv1, depot, time(1));
        assertNull(rm.getDestinationToParcel(dv1));
        rm.moveTo(dv1, depot, time(1));
        rm.moveTo(dv1, dp1, time(1));
        assertEquals(dp1, rm.getDestinationToParcel(dv1));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRoadUser() {
        // does not exist
        rm.moveTo(dv1, new PlainTestParcel(new Point(6, 6)), time(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRoadUser2() {
        rm.addObjectAtSamePosition(create(new Point(1, 1), new Point(2, 2)),
            dp2);
        rm.addObjectAtSamePosition(new PlainTestParcel(new Point(6, 6)), dp2);
    }

    @Test(expected = UnsupportedOperationException.class)
    @SuppressWarnings("null")
    public void invalidMoveTo() {
        rm.moveTo(null, new Point(0, 0), null);
    }

    @Test(expected = UnsupportedOperationException.class)
    @SuppressWarnings("null")
    public void invalidFollowPath() {
        rm.followPath(dv1, newLinkedList(rm.getShortestPathTo(new Point(0, 0),
            new Point(10, 10))), time(1));
    }

    static TimeLapse time(long t) {
        return TimeLapseFactory.create(0, t);
    }

    static DefaultParcel create(Point p1, Point p2) {
        return new DefaultParcel(new ParcelDTO(p1, p2, DEFAULT_TW, DEFAULT_TW,
                0, 0, 0, 0));
    }

    static class TestVehicle extends DefaultVehicle {
        public TestVehicle(Point start) {
            super(new VehicleDTO(start, 10, 0, DEFAULT_TW));
        }

        @Override
        protected void tickImpl(TimeLapse time) {}

        @Override
        public String toString() {
            return "TestVehicle";
        }
    }

    static class PlainTestParcel extends Parcel {
        PlainTestParcel(Point pDestination) {
            super(pDestination, 0, DEFAULT_TW, 0, DEFAULT_TW, 0);
        }

        @Override
        public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
    }

}
