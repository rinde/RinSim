/**
 * 
 */
package rinde.sim.core.model.pdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.TimeLapseFactory;
import rinde.sim.core.graph.Point;
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

        final Package p = new TestPackage();
        final Truck t = new TestTruck();

        model.register(p);
        model.register(t);

        rm.register(p);
        rm.register(t);

        rm.addObjectAt(t, new Point(1, 1));
        rm.addObjectAt(p, new Point(1, 1));

        assertEquals(0, model.getContentsSize(t), EPSILON);

        model.pickup(t, p, TimeLapseFactory.create(0, 10000));

        assertFalse(rm.containsObject(p));
        assertTrue(model.truckContains(t, p));

        assertEquals(2, model.getContentsSize(t), EPSILON);

    }

    class TestPackage extends Package {

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

        @Override
        public double getCapacity() {
            return 10;
        }

        @Override
        public double getSpeed() {
            return 0;
        }

        @Override
        protected void controlLoop(TimeLapse time) {
            // TODO Auto-generated method stub

        }

        @Override
        public void afterTick(TimeLapse timeLapse) {}

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
