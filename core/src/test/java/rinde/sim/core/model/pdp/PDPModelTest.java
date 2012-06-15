/**
 * 
 */
package rinde.sim.core.model.pdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class PDPModelTest {

	static final double EPSILON = 0.0000001;

	PDPModel pdp;
	RoadModel rm;

	@Before
	public void setUp() {
		rm = new PlaneRoadModel(new Point(0, 0), new Point(10, 10), Double.POSITIVE_INFINITY);
		pdp = new PDPModel(rm);
	}

	@Test
	public void testPickup() {

		Package p = new TestPackage();
		Truck t = new TestTruck();

		pdp.register(p);
		pdp.register(t);

		rm.register(p);
		rm.register(t);

		rm.addObjectAt(t, new Point(1, 1));
		rm.addObjectAt(p, new Point(1, 1));

		assertEquals(0, pdp.getContentsSize(t), EPSILON);

		pdp.pickup(t, p);

		assertFalse(rm.containsObject(p));
		assertTrue(pdp.truckContains(t, p));

		assertEquals(2, pdp.getContentsSize(t), EPSILON);

	}

	class TestPackage extends Package {

		@Override
		public void initPDPObject(PDPModel pModel) {

		}

		@Override
		public void initRoadUser(RoadModel pModel) {

			// TODO Auto-generated method stub

		}

		@Override
		double getMagnitude() {
			// TODO Auto-generated method stub
			return 2;
		}

	}

	class TestTruck extends Truck {

		/*
		 * (non-Javadoc)
		 * @see rinde.sim.core.model.pdp.PackageContainer#getCapacity()
		 */
		@Override
		public double getCapacity() {
			// TODO Auto-generated method stub
			return 10;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * rinde.sim.core.model.pdp.PDPObject#initPDPObject(rinde.sim.core.model
		 * .pdp.PDPModel)
		 */
		@Override
		public void initPDPObject(PDPModel model) {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * @see
		 * rinde.sim.core.model.road.RoadUser#initRoadUser(rinde.sim.core.model
		 * .road.RoadModel)
		 */
		@Override
		public void initRoadUser(RoadModel model) {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * @see rinde.sim.core.model.road.MovingRoadUser#getSpeed()
		 */
		@Override
		public double getSpeed() {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	class TestDepot extends Depot {

		/*
		 * (non-Javadoc)
		 * @see rinde.sim.core.model.pdp.PackageContainer#getCapacity()
		 */
		@Override
		public double getCapacity() {
			// TODO Auto-generated method stub
			return 0;
		}

		/*
		 * (non-Javadoc)
		 * @see
		 * rinde.sim.core.model.pdp.PDPObject#initPDPObject(rinde.sim.core.model
		 * .pdp.PDPModel)
		 */
		@Override
		public void initPDPObject(PDPModel pModel) {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * @see
		 * rinde.sim.core.model.road.RoadUser#initRoadUser(rinde.sim.core.model
		 * .road.RoadModel)
		 */
		@Override
		public void initRoadUser(RoadModel pModel) {
			// TODO Auto-generated method stub

		}

	}

}
