/**
 * 
 */
package rinde.sim.problem.fabrirecht.example;

import java.io.IOException;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.road.RoadModels;
import rinde.sim.problem.fabrirecht.AddVehicleEvent;
import rinde.sim.problem.fabrirecht.FRDepot;
import rinde.sim.problem.fabrirecht.FRParcel;
import rinde.sim.problem.fabrirecht.FRVehicle;
import rinde.sim.problem.fabrirecht.FabriRechtParser;
import rinde.sim.problem.fabrirecht.FabriRechtProblem;
import rinde.sim.problem.fabrirecht.VehicleDTO;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.PDPModelRenderer;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class FabriRechtExample {

	public static void main(String[] args) throws IOException, ConfigurationException {
		final ProblemInstance pi = new ProblemInstance("data/test/fabri-recht/lc101_coord.csv",
				"data/test/fabri-recht/lc101.csv");

	}

}

class ProblemInstance extends FabriRechtProblem {

	/**
	 * @param scen
	 * @throws IOException
	 * @throws ConfigurationException
	 */
	public ProblemInstance(String coordinateFile, String ordersFile) throws IOException, ConfigurationException {
		super(FabriRechtParser.parse(coordinateFile, ordersFile));
		initialize();
	}

	@Override
	protected boolean handleAddVehicle(AddVehicleEvent event) {
		return getSimulator().register(new Truck(event.vehicleDTO));
	}

	@Override
	protected boolean handleTimeOut() {

		System.out.println("total traveled distance: " + statisticsListener.getTotalTraveledDistance());
		System.out.println(statisticsListener.getTotalPickups() + " / " + parcelCount);
		System.out.println(statisticsListener.getTotalDeliveries() + " / " + parcelCount);

		return true;
	}

	@Override
	protected boolean createUserInterface() {
		final UiSchema schema = new UiSchema(false);
		schema.add(Truck.class, "/graphics/perspective/bus-44.png");
		schema.add(FRDepot.class, "/graphics/flat/warehouse-32.png");
		schema.add(FRParcel.class, "/graphics/flat/hailing-cab-32.png");
		View.startGui(getSimulator(), 1, new PlaneRoadModelRenderer(40), new RoadUserRenderer(schema, false), new PDPModelRenderer());
		return true;
	}
}

class Truck extends FRVehicle {

	/**
	 * @param pDto
	 */
	public Truck(VehicleDTO pDto) {
		super(pDto);
	}

	@Override
	protected void tickImpl(TimeLapse time) {
		final FRParcel closest = RoadModels.findClosestObject(roadModel.getPosition(this), roadModel, FRParcel.class);

		if (closest != null) {
			roadModel.moveTo(this, closest, time);
			if (roadModel.equalPosition(closest, this)) {
				pdpModel.pickup(this, closest, time);
			}
		}

	}
}
