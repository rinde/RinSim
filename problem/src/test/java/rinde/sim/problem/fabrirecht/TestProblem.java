/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import java.io.IOException;

import org.junit.Test;

import rinde.sim.core.TimeLapse;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class TestProblem {

	@Test
	public void test() throws IOException {

		final FabriRechtScenario scen = FabriRechtParser
				.parse("data/test/fabri-recht/lc101_coord.csv", "data/test/fabri-recht/lc101.csv");

		final ProblemInstance prob = new ProblemInstance(scen);

	}

	class ProblemInstance extends FabriRechtProblem {

		/**
		 * @param scen
		 */
		public ProblemInstance(FabriRechtScenario scen) {
			super(scen);
		}

		@Override
		protected boolean handleAddVehicle(AddVehicleEvent event) {
			System.out.println("add a vehicle");
			return getSimulator().register(new TestVehicle(event.vehicleDTO));
		}

		@Override
		protected boolean handleTimeOut() {
			return true;
		}

	}

	class TestVehicle extends FRVehicle {

		/**
		 * @param pDto
		 */
		public TestVehicle(VehicleDTO pDto) {
			super(pDto);
		}

		@Override
		protected void tickImpl(TimeLapse time) {
			// TODO Auto-generated method stub

		}

	}

}
