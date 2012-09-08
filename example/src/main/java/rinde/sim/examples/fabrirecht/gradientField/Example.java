/**
 * 
 */
package rinde.sim.examples.fabrirecht.gradientField;

import java.io.IOException;

import rinde.sim.core.Simulator;
import rinde.sim.examples.fabrirecht.AddParcelEvent;
import rinde.sim.examples.fabrirecht.AddVehicleEvent;
import rinde.sim.examples.fabrirecht.FRDepot;
import rinde.sim.examples.fabrirecht.FRParcel;
import rinde.sim.examples.fabrirecht.FabriRechtParser;
import rinde.sim.examples.fabrirecht.FabriRechtProblem;
import rinde.sim.examples.fabrirecht.ParcelAssesor;
import rinde.sim.examples.fabrirecht.ParcelDTO;
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
public class Example {

	public static void main(String[] args) throws IOException, ConfigurationException {
		final ProblemInstance pi = new ProblemInstance("data/test/fabri-recht/lc101_coord.csv",
				"data/test/fabri-recht/lc101.csv");

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
		//Simple solution: always accept any parcel requests
		return new ParcelAssesor() {
			@Override
			public boolean acceptParcel(ParcelDTO parcel) {
				return true;
			}
		};
	}
}