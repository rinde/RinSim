/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.FileWriter;
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

		final String json = FabriRechtParser.toJson(scen);
		// System.out.println(json);
		FabriRechtParser.toJson(scen, new BufferedWriter(new FileWriter("data/test/fabri-recht/lc101.scenario")));

		final FabriRechtScenario scen2 = FabriRechtParser.fromJson(json);
		assertEquals(scen, scen2);
		assertEquals(scen.getPossibleEventTypes(), scen2.getPossibleEventTypes());
		final String json2 = FabriRechtParser.toJson(scen2);
		assertEquals(json, json2);
		final FabriRechtScenario scen3 = FabriRechtParser.fromJson(json2);
		assertEquals(scen2, scen3);
		assertEquals(scen2.getPossibleEventTypes(), scen3.getPossibleEventTypes());

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
