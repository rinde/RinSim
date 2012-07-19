/**
 * 
 */
package rinde.sim.core.model.pdp;

import static com.google.common.collect.Lists.newLinkedList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.TimeLapseFactory;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel.PickupAction;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
@SuppressWarnings("javadoc")
public class PDPModelTest {

    static final double EPSILON = 0.0000001;

    PDPModel model;
    RoadModel rm;

    @Before
    public void setUp() {
        rm = new PlaneRoadModel(new Point(0, 0), new Point(10, 10),
                Double.POSITIVE_INFINITY);
        model = new PDPModel(rm);
    }

    @Test
    public void testPickup() {
        final Package pack1 = new TestPackage(0, 0);
        final Truck truck = new TestTruck(new Point(1, 1));
        model.register(pack1);
        model.register(truck);
        rm.register(pack1);
        rm.register(truck);
        rm.addObjectAt(pack1, new Point(1, 1));

        assertEquals(0, model.getContentsSize(truck), EPSILON);
        assertTrue(model.getContents(truck).isEmpty());
        model.pickup(truck, pack1, TimeLapseFactory.create(0, 10000));

        assertFalse(rm.containsObject(pack1));
        assertTrue(model.truckContains(truck, pack1));
        assertEquals(2, model.getContentsSize(truck), EPSILON);
        assertTrue(model.getContents(truck).contains(pack1));
        assertEquals(1, model.getContents(truck).size());

        final Package pack2 = new TestPackage(100, 100);
        model.register(pack2);
        rm.register(pack2);
        rm.addObjectAt(pack2, new Point(1, 2));

        rm.followPath(truck, newLinkedList(asList(new Point(1, 2))), TimeLapseFactory
                .create(0, 3600000));

        assertEquals(new Point(1, 2), rm.getPosition(truck));
        assertEquals(new Point(1, 2), rm.getPosition(pack2));
        model.pickup(truck, pack2, TimeLapseFactory.create(0, 40));
        assertFalse(rm.containsObject(pack2));
        final PickupAction action = (PickupAction) model.truckActions
                .get(truck);
        assertFalse(action.isDone());
        assertEquals(60, action.timeNeeded());

        model.continuePreviousActions(truck, TimeLapseFactory.create(0, 40));
        assertFalse(action.isDone());
        assertEquals(20, action.timeNeeded());

        final TimeLapse tl = TimeLapseFactory.create(0, 40);
        model.continuePreviousActions(truck, tl);
        assertTrue(action.isDone());
        assertEquals(0, action.timeNeeded());
        assertEquals(20, tl.getTimeLeft());

    }

    class TestPackage extends Package {

        /**
         * @param pLoadingDuration
         * @param pUnloadingDuration
         */
        public TestPackage(int pLoadingDuration, int pUnloadingDuration) {
            super(pLoadingDuration, pUnloadingDuration);
        }

        @Override
        public void initPDPObject(PDPModel pModel) {}

        @Override
        public void initRoadUser(RoadModel pModel) {}

        @Override
        double getMagnitude() {
            return 2;
        }

    }

    class TestTruck extends Truck {

        /**
         * @param startPos
         */
        public TestTruck(Point startPos) {
            super(startPos);
        }

        @Override
        public double getCapacity() {
            return 10;
        }

        @Override
        public double getSpeed() {
            return 1;
        }

        @Override
        protected void controlLoop(TimeLapse time) {
            // TODO Auto-generated method stub

        }

        @Override
        public void afterTick(TimeLapse timeLapse) {}

        @Override
        protected void init() {
            // TODO Auto-generated method stub

        }

    }

    class TestDepot extends Depot {

        @Override
        public double getCapacity() {
            return 0;
        }

        @Override
        public void initPDPObject(PDPModel pModel) {}

        @Override
        public void initRoadUser(RoadModel pModel) {}

    }
}
