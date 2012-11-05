/**
 * 
 */
package rinde.sim.examples.fabrirecht.simple;

import java.io.FileReader;
import java.io.IOException;

import rinde.sim.core.Simulator;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.road.RoadModels;
import rinde.sim.core.model.road.RoadUser;
import rinde.sim.problem.common.AddVehicleEvent;
import rinde.sim.problem.common.DefaultParcel;
import rinde.sim.problem.common.DefaultVehicle;
import rinde.sim.problem.common.DynamicPDPTWProblem;
import rinde.sim.problem.common.DynamicPDPTWProblem.Creator;
import rinde.sim.problem.common.VehicleDTO;
import rinde.sim.problem.fabrirecht.FabriRechtParser;
import rinde.sim.problem.fabrirecht.FabriRechtScenario;
import rinde.sim.scenario.ConfigurationException;

import com.google.common.base.Predicate;

/**
 * Simplest example showing how the Fabri & Recht problem can be configured
 * using a custom vehicle.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class FabriRechtExample {

	public static void main(String[] args) throws IOException, ConfigurationException {
		// we load a problem instance from disk, we instantiate it with 8
		// trucks, each with a capacity of 20 units
		final FabriRechtScenario scenario = FabriRechtParser.fromJson(new FileReader(
				"../problem/data/test/fabri-recht/lc101.scenario"), 8, 20);

		// instantiate the problem using the scenario and a random seed (which
		// will not be used in this example)
		final DynamicPDPTWProblem problem = new DynamicPDPTWProblem(scenario, 123);

		// we plug our custom vehicle in by specifying a creator
		problem.addCreator(AddVehicleEvent.class, new Creator<AddVehicleEvent>() {
			@Override
			public boolean create(Simulator sim, AddVehicleEvent event) {
				return sim.register(new Truck(event.vehicleDTO));
			}
		});

		// enable the default UI
		problem.enableUI();

		// start the simulation
		problem.simulate();

		// simulation is done, lets print the statistics!
		System.out.println(problem.getStatistics());
	}
}

/**
 * This truck implementation only picks parcels up, it does not deliver them.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
class Truck extends DefaultVehicle {

	public Truck(VehicleDTO pDto) {
		super(pDto);
	}

	@Override
	protected void tickImpl(TimeLapse time) {
		// we always go to the closest available parcel
		final DefaultParcel closest = (DefaultParcel) RoadModels
				.findClosestObject(roadModel.getPosition(this), roadModel, new Predicate<RoadUser>() {
					@Override
					public boolean apply(RoadUser input) {
						return input instanceof DefaultParcel
								&& pdpModel.getParcelState(((DefaultParcel) input)) == ParcelState.AVAILABLE;
					}
				});

		if (closest != null) {
			roadModel.moveTo(this, closest, time);
			if (roadModel.equalPosition(closest, this)
					&& pdpModel.getTimeWindowPolicy()
							.canPickup(closest.getPickupTimeWindow(), time.getTime(), closest.getPickupDuration())) {
				pdpModel.pickup(this, closest, time);
			}
		}
	}
}
