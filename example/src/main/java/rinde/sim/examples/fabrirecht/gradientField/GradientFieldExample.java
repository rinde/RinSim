/**
 * 
 */
package rinde.sim.examples.fabrirecht.gradientField;

import java.io.FileReader;
import java.io.IOException;

import rinde.sim.core.Simulator;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DefaultDepot;
import rinde.sim.pdptw.common.DefaultParcel;
import rinde.sim.pdptw.common.DynamicPDPTWProblem;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.Creator;
import rinde.sim.pdptw.fabrirecht.FabriRechtParser;
import rinde.sim.pdptw.fabrirecht.FabriRechtScenario;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.scenario.ScenarioController.UICreator;
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
		final FabriRechtScenario scenario = FabriRechtParser.fromJson(new FileReader(
				"../problem/data/test/fabri-recht/lc101.scenario"), 10, 4);

		// instantiate the problem and adding our custom model
		final DynamicPDPTWProblem problem = new DynamicPDPTWProblem(scenario, 123, new GradientModel(0, 95, 5, 85));

		// plugging our own vehicle in
		problem.addCreator(AddVehicleEvent.class, new Creator<AddVehicleEvent>() {
			@Override
			public boolean create(Simulator sim, AddVehicleEvent event) {
				return sim.register(new Truck(event.vehicleDTO));
			}
		});
		// pluggin our custom parcel in
		problem.addCreator(AddParcelEvent.class, new Creator<AddParcelEvent>() {
			@Override
			public boolean create(Simulator sim, AddParcelEvent event) {
				// all parcels are accepted by default
				return sim.register(new GFParcel(event.parcelDTO));
			}
		});

		// enabling UI using our custom viz
		problem.enableUI(new UICreator() {

			@Override
			public void createUI(Simulator sim) {
				final UiSchema schema = new UiSchema(false);
				schema.add(Truck.class, "/graphics/perspective/bus-44.png");
				schema.add(DefaultDepot.class, "/graphics/flat/warehouse-32.png");
				schema.add(DefaultParcel.class, "/graphics/flat/hailing-cab-32.png");
				View.startGui(sim, 1, new PlaneRoadModelRenderer(0.05), new RoadUserRenderer(schema, false), new PDPModelRenderer(), new GradientFieldRenderer());

			}
		});

		problem.simulate();
		System.out.println(problem.getStatistics());
	}

}