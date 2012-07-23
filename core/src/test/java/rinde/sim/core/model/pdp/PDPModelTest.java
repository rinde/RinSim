/**
 * 
 */
package rinde.sim.core.model.pdp;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.TimeLapseFactory;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel.PackageState;
import rinde.sim.core.model.pdp.PDPModel.PickupAction;
import rinde.sim.core.model.pdp.PDPModel.TruckState;
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
        final Parcel pack1 = new TestPackage(new Point(2, 2), 0, 0, 2);
        final Truck truck = new TestTruck(new Point(1, 1), 10.0, 1.0);
        model.register(pack1);
        model.register(truck);
        rm.register(pack1);
        rm.register(truck);
        rm.addObjectAt(pack1, new Point(1, 1));

        assertEquals(0, model.getContentsSize(truck), EPSILON);
        assertTrue(model.getContents(truck).isEmpty());
        assertEquals(PackageState.AVAILABLE, model.getPackageState(pack1));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));
        model.pickup(truck, pack1, TimeLapseFactory.create(0, 10000));
        assertEquals(PackageState.IN_CARGO, model.getPackageState(pack1));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));

        assertFalse(rm.containsObject(pack1));
        assertTrue(model.truckContains(truck, pack1));
        assertEquals(2, model.getContentsSize(truck), EPSILON);
        assertTrue(model.getContents(truck).contains(pack1));
        assertEquals(1, model.getContents(truck).size());

        final Parcel pack2 = new TestPackage(new Point(2, 2), 100, 100, 2);
        model.register(pack2);
        rm.register(pack2);
        rm.addObjectAt(pack2, new Point(1, 2));

        rm.followPath(truck, newLinkedList(asList(new Point(1, 2))), TimeLapseFactory
                .create(0, 3600000));

        assertEquals(new Point(1, 2), rm.getPosition(truck));
        assertEquals(new Point(1, 2), rm.getPosition(pack2));
        assertEquals(PackageState.AVAILABLE, model.getPackageState(pack2));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));
        model.pickup(truck, pack2, TimeLapseFactory.create(0, 40));
        assertFalse(rm.containsObject(pack2));
        final PickupAction action = (PickupAction) model.truckActions
                .get(truck);
        assertFalse(action.isDone());
        assertEquals(60, action.timeNeeded());
        assertEquals(PackageState.LOADING, model.getPackageState(pack2));
        assertEquals(TruckState.LOADING, model.getTruckState(truck));

        model.continuePreviousActions(truck, TimeLapseFactory.create(0, 40));
        assertFalse(action.isDone());
        assertEquals(20, action.timeNeeded());
        assertEquals(PackageState.LOADING, model.getPackageState(pack2));
        assertEquals(TruckState.LOADING, model.getTruckState(truck));

        final TimeLapse tl = TimeLapseFactory.create(0, 40);
        model.continuePreviousActions(truck, tl);
        assertTrue(action.isDone());
        assertEquals(0, action.timeNeeded());
        assertEquals(20, tl.getTimeLeft());
        assertEquals(PackageState.IN_CARGO, model.getPackageState(pack2));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testPickupFail1() {
        // not enough time
        final TimeLapse tl = TimeLapseFactory.create(0, 1);
        tl.consumeAll();
        model.pickup(null, null, tl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPickupFail2() {
        // truck not in roadmodel
        final Truck truck = new TestTruck(new Point(1, 1), 10.0, 1.0);
        model.pickup(truck, null, TimeLapseFactory.create(0, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPickupFail3() {
        // package not in roadmodel
        final Truck truck = new TestTruck(new Point(1, 1), 10.0, 1.0);
        rm.register(truck);
        final Parcel pack1 = new TestPackage(new Point(2, 2), 0, 0, 2.0);
        model.pickup(truck, pack1, TimeLapseFactory.create(0, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPickupFail4A() {
        // package not in available state (it is already been picked up)
        final Truck truck = new TestTruck(new Point(1, 1), 10.0, 1.0);
        rm.register(truck);
        model.register(truck);
        final Parcel pack1 = new TestPackage(new Point(2, 2), 10, 10, 2.0);
        rm.register(pack1);
        model.register(pack1);
        rm.addObjectAtSamePosition(pack1, truck);
        assertTrue(rm.equalPosition(truck, pack1));
        assertEquals(PackageState.AVAILABLE, model.getPackageState(pack1));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));

        model.pickup(truck, pack1, TimeLapseFactory.create(0, 100));
        assertEquals(PackageState.IN_CARGO, model.getPackageState(pack1));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));

        // checks what happens when you add a package to the roadmodel which has
        // already been picked up
        rm.addObjectAt(pack1, new Point(1, 1));
        model.pickup(truck, pack1, TimeLapseFactory.create(0, 100));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testPickupFail4B() {
        // package is not registered
        final Truck truck = new TestTruck(new Point(1, 1), 10.0, 1.0);
        rm.register(truck);
        final Parcel pack1 = new TestPackage(new Point(2, 2), 0, 0, 2.0);
        rm.register(pack1);
        rm.addObjectAtSamePosition(pack1, truck);
        model.pickup(truck, pack1, TimeLapseFactory.create(0, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPickupFail5() {
        final Truck truck = new TestTruck(new Point(1, 1), 10.0, 1.0);
        rm.register(truck);
        final Parcel pack1 = new TestPackage(new Point(2, 2), 0, 0, 2.0);
        rm.register(pack1);
        rm.addObjectAtSamePosition(pack1, truck);
        model.register(pack1);
        model.pickup(truck, pack1, TimeLapseFactory.create(0, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPickupFail6() {
        // wrong position
        final Truck truck = new TestTruck(new Point(1, 1), 10.0, 1.0);
        rm.register(truck);
        model.register(truck);
        final Parcel pack1 = new TestPackage(new Point(2, 2), 0, 0, 2.0);
        rm.register(pack1);
        rm.addObjectAt(pack1, new Point(0, 0));
        model.register(pack1);
        model.pickup(truck, pack1, TimeLapseFactory.create(0, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPickupFail7() {
        // package does not fit in truck
        final Truck truck = new TestTruck(new Point(1, 1), 1.0, 1.0);
        rm.register(truck);
        model.register(truck);
        final Parcel pack1 = new TestPackage(new Point(2, 2), 0, 0, 2.0);
        rm.register(pack1);
        model.register(pack1);
        rm.addObjectAtSamePosition(pack1, truck);
        assertTrue(rm.equalPosition(truck, pack1));
        model.pickup(truck, pack1, TimeLapseFactory.create(0, 100));
    }

    @Test
    public void testDeliver() {
        final Truck truck = new TestTruck(new Point(1, 1), 10.0, 1.0);
        rm.register(truck);
        model.register(truck);
        final Parcel pack1 = new TestPackage(new Point(2, 2), 10, 10, 2.0);
        rm.register(pack1);
        model.register(pack1);
        rm.addObjectAtSamePosition(pack1, truck);
        assertTrue(rm.equalPosition(truck, pack1));
        assertEquals(PackageState.AVAILABLE, model.getPackageState(pack1));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));
        assertEquals(newHashSet(pack1), model.getAvailableParcels());

        model.pickup(truck, pack1, TimeLapseFactory.create(0, 100));
        assertEquals(PackageState.IN_CARGO, model.getPackageState(pack1));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));
        assertTrue(model.getAvailableParcels().isEmpty());

        rm.moveTo(truck, pack1.getDestination(), TimeLapseFactory
                .create(0, 3600000 * 3));
        assertEquals(pack1.getDestination(), rm.getPosition(truck));

        model.deliver(truck, pack1, TimeLapseFactory.create(0, 8));
        assertEquals(PackageState.UNLOADING, model.getPackageState(pack1));
        assertEquals(TruckState.UNLOADING, model.getTruckState(truck));

        final TimeLapse tl = TimeLapseFactory.create(0, 10);
        model.continuePreviousActions(truck, tl);
        assertEquals(PackageState.DELIVERED, model.getPackageState(pack1));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));
        assertEquals(8, tl.getTimeLeft());

        final Parcel pack2 = new TestPackage(new Point(2, 2), 10, 10, 2.0);
        rm.register(pack2);
        model.register(pack2);
        rm.addObjectAtSamePosition(pack2, truck);
        assertEquals(PackageState.AVAILABLE, model.getPackageState(pack2));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));
        assertEquals(newHashSet(pack2), model.getAvailableParcels());

        model.pickup(truck, pack2, TimeLapseFactory.create(0, 10));
        assertEquals(PackageState.IN_CARGO, model.getPackageState(pack2));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));
        assertTrue(model.getAvailableParcels().isEmpty());

        model.deliver(truck, pack2, TimeLapseFactory.create(0, 10));
        assertEquals(PackageState.DELIVERED, model.getPackageState(pack2));
        assertEquals(TruckState.IDLE, model.getTruckState(truck));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeliverFail1() {
        // not enough time
        final TimeLapse tl = TimeLapseFactory.create(0, 1);
        tl.consumeAll();
        model.deliver(null, null, tl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeliverFail2() {
        // truck does not exist in roadmodel
        model.deliver(new TestTruck(new Point(1, 1), 20.0, 1.0), null, TimeLapseFactory
                .create(0, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeliverFail3() {
        final Truck truck = new TestTruck(new Point(1, 1), 10.0, 1.0);
        rm.register(truck);
        model.register(truck);
        final Parcel pack1 = new TestPackage(new Point(2, 2), 10, 10, 2.0);
        rm.register(pack1);
        model.register(pack1);
        rm.addObjectAtSamePosition(pack1, truck);

        model.pickup(truck, pack1, TimeLapseFactory.create(0, 1));
        assertEquals(TruckState.LOADING, model.getTruckState(truck));
        model.deliver(truck, pack1, TimeLapseFactory.create(0, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeliverFail4() {
        final Truck truck = new TestTruck(new Point(1, 1), 10.0, 1.0);
        rm.register(truck);
        model.register(truck);
        final Parcel pack1 = new TestPackage(new Point(2, 2), 10, 10, 2.0);
        rm.register(pack1);
        model.register(pack1);
        rm.addObjectAtSamePosition(pack1, truck);

        model.deliver(truck, pack1, TimeLapseFactory.create(0, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeliverFail5() {
        final Truck truck = new TestTruck(new Point(1, 1), 10.0, 1.0);
        rm.register(truck);
        model.register(truck);
        final Parcel pack1 = new TestPackage(new Point(2, 2), 10, 10, 2.0);
        rm.register(pack1);
        model.register(pack1);
        rm.addObjectAtSamePosition(pack1, truck);

        model.pickup(truck, pack1, TimeLapseFactory.create(0, 10));
        assertTrue(model.getContents(truck).contains(pack1));
        model.deliver(truck, pack1, TimeLapseFactory.create(0, 1));
    }

    @Test
    public void addPackageIn() {
        assertTrue(model.getAvailableParcels().isEmpty());
        final Depot d = new TestDepot(10);
        final Parcel p1 = new TestPackage(new Point(0, 0), 0, 0, 1);
        model.register(d);
        model.register(p1);
        rm.addObjectAt(d, new Point(0, 0));
        model.addPackageIn(d, p1);

        assertEquals(newHashSet(p1), model.getAvailableParcels());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addPackageInFail1() {
        final Depot d = new TestDepot(10);
        final Parcel p1 = new TestPackage(new Point(0, 0), 0, 0, 1);
        rm.addObjectAt(p1, new Point(0, 0));
        model.addPackageIn(d, p1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addPackageInFail2() {
        final Depot d = new TestDepot(10);
        final Parcel p1 = new TestPackage(new Point(0, 0), 0, 0, 1);
        model.addPackageIn(d, p1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addPackageInFail3() {
        final Depot d = new TestDepot(10);
        final Parcel p1 = new TestPackage(new Point(0, 0), 0, 0, 1);
        model.register(p1);
        model.addPackageIn(d, p1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addPackageInFail4() {
        final Depot d = new TestDepot(10);
        final Parcel p1 = new TestPackage(new Point(0, 0), 0, 0, 1);
        model.register(p1);
        model.register(d);
        model.addPackageIn(d, p1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addPackageInFail5() {
        final Depot d = new TestDepot(10);
        final Parcel p1 = new TestPackage(new Point(0, 0), 0, 0, 11);
        model.register(p1);
        model.register(d);
        rm.addObjectAt(d, new Point(0, 0));
        model.addPackageIn(d, p1);
    }

    class TestPackage extends Parcel {

        /**
         * @param pLoadingDuration
         * @param pUnloadingDuration
         */
        public TestPackage(Point pDestination, int pLoadingDuration,
                int pUnloadingDuration, double pMagnitude) {
            super(pDestination, pLoadingDuration, pUnloadingDuration,
                    pMagnitude);
        }

        @Override
        public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    }

    class TestTruck extends Truck {

        private final double speed;

        /**
         * @param startPos
         */
        public TestTruck(Point startPos, double pCapacity, double pSpeed) {
            setStartPosition(startPos);
            setCapacity(pCapacity);
            speed = pSpeed;
        }

        @Override
        public double getSpeed() {
            return speed;
        }

        @Override
        protected void tickImpl(TimeLapse time) {}

        @Override
        public void afterTick(TimeLapse timeLapse) {}

        @Override
        public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
    }

    class TestDepot extends Depot {

        public TestDepot(int pCapacity) {
            setCapacity(pCapacity);
        }

        @Override
        public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

    }
}
