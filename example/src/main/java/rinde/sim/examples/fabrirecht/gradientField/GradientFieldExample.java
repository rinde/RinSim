/**
 * 
 */
package rinde.sim.examples.fabrirecht.gradientField;

import java.io.IOException;

import rinde.sim.core.Simulator;
import rinde.sim.problem.fabrirecht.AddParcelEvent;
import rinde.sim.problem.fabrirecht.AddVehicleEvent;
import rinde.sim.problem.fabrirecht.FRDepot;
import rinde.sim.problem.fabrirecht.FRParcel;
import rinde.sim.problem.fabrirecht.FabriRechtParser;
import rinde.sim.problem.fabrirecht.FabriRechtProblem;
import rinde.sim.problem.fabrirecht.ParcelAssesor;
import rinde.sim.problem.fabrirecht.ParcelDTO;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.PDPModelRenderer;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

/**
 * @author David Merckx
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class GradientFieldExample {

	public static void main(String[] args) throws IOException, ConfigurationException {
		final ProblemInstance pi = new ProblemInstance("../problem/data/test/fabri-recht/lc101_coord.csv",
				"../problem/data/test/fabri-recht/lc101.csv");

	}

}

class ProblemInstance extends FabriRechtProblem {
	private GradientModel gradientModel;

	public ProblemInstance(String coordinateFile, String ordersFile) throws IOException, ConfigurationException {
		super(FabriRechtParser.parse(coordinateFile, ordersFile));
		initialize();
	}

	@Override
	protected Simulator createSimulator() throws Exception {
		super.createSimulator();

		gradientModel = new GradientModel(0, 95, 5, 85, pdpModel);
		simulator.register(gradientModel);

		return simulator;
	}

	@Override
	protected boolean handleAddVehicle(AddVehicleEvent event) {
		return getSimulator().register(new Truck(event.vehicleDTO));
	}

	@Override
	protected boolean handleAddParcel(AddParcelEvent event) {
		return getSimulator().register(new GFParcel(event.parcelDTO));
	}

	@Override
	protected boolean handleTimeOut() {
		System.out.println(statisticsListener.getDTO());
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

	@Override
	protected ParcelAssesor createParcelAssesor() {
		// Simple solution: always accept any parcel requests
		return new ParcelAssesor() {
			@Override
			public boolean acceptParcel(ParcelDTO parcel) {
				return true;
			}
		};
	}
}