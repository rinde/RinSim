/**
 * 
 */
package rinde.sim.examples.fabrirecht.simple;

import java.io.IOException;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.road.RoadModels;
import rinde.sim.core.model.road.RoadUser;
import rinde.sim.examples.fabrirecht.AddVehicleEvent;
import rinde.sim.examples.fabrirecht.FRDepot;
import rinde.sim.examples.fabrirecht.FRParcel;
import rinde.sim.examples.fabrirecht.FRVehicle;
import rinde.sim.examples.fabrirecht.FabriRechtParser;
import rinde.sim.examples.fabrirecht.FabriRechtProblem;
import rinde.sim.examples.fabrirecht.ParcelAssesor;
import rinde.sim.examples.fabrirecht.ParcelDTO;
import rinde.sim.examples.fabrirecht.VehicleDTO;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.PDPModelRenderer;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

import com.google.common.base.Predicate;

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

class Truck extends FRVehicle {

	/**
	 * @param pDto
	 */
	public Truck(VehicleDTO pDto) {
		super(pDto);
	}

	@Override
	protected void tickImpl(TimeLapse time) {
		// final FRParcel closest =
		// RoadModels.findClosestObject(roadModel.getPosition(this), roadModel,
		// FRParcel.class);

		final FRParcel closest = (FRParcel) RoadModels
				.findClosestObject(roadModel.getPosition(this), roadModel, new Predicate<RoadUser>() {
					@Override
					public boolean apply(RoadUser input) {
						return input instanceof FRParcel
								&& pdpModel.getParcelState(((FRParcel) input)) == ParcelState.AVAILABLE;
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
