/**
 * 
 */
package rinde.sim.example.pdp;

import org.apache.commons.math3.random.MersenneTwister;

import rinde.sim.core.Simulator;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Truck;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class PDPExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		final Simulator sim = new Simulator(new MersenneTwister(123), 1000);

		final RoadModel rm = new PlaneRoadModel(new Point(0, 0), new Point(100, 100), 1.0);
		sim.register(rm);
		sim.register(new PDPModel(rm));
		sim.configure();

		sim.register(new ExampleTruck(new Point(1, 1)));

	}

	static class ExampleTruck extends Truck {

		/**
		 * @param startPos
		 */
		public ExampleTruck(Point startPos) {
			setStartPosition(startPos);

			// TODO Auto-generated constructor stub
		}

		@Override
		public double getSpeed() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void afterTick(TimeLapse timeLapse) {
			// TODO Auto-generated method stub

		}

		@Override
		protected void tickImpl(TimeLapse time) {
			// TODO Auto-generated method stub

		}

		@Override
		public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
			// TODO Auto-generated method stub

		}

	}
}
