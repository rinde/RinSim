/**
 * 
 */
package rinde.sim.problem.gendreau06;

import static com.google.common.base.Preconditions.checkArgument;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.ADD_DEPOT;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.ADD_PARCEL;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.ADD_VEHICLE;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import rinde.sim.core.graph.Point;
import rinde.sim.problem.common.AddDepotEvent;
import rinde.sim.problem.common.AddParcelEvent;
import rinde.sim.problem.common.AddVehicleEvent;
import rinde.sim.problem.common.ParcelDTO;
import rinde.sim.problem.common.VehicleDTO;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.ScenarioBuilder.ScenarioCreator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Gendreau06Parser {

	/**
	 * <b>Format: (columns)</b>
	 * <ul>
	 * <li>1: request arrival time</li>
	 * <li>2: pick-up service time</li>
	 * <li>3 and 4: x and y position for the pick-up</li>
	 * <li>5 and 6: service window time of the pick-up</li>
	 * <li>7: delivery service time</li>
	 * <li>8 and 9:x and y position for the delivery</li>
	 * <li>10 and 11: service window time of the delivery</li>
	 * </ul>
	 */

	public static Gendreau06Scenario parse(String file, int numVehicles) throws IOException {
		checkArgument(numVehicles > 0, "at least one vehicle is necessary in the scenario");
		final ScenarioBuilder sb = new ScenarioBuilder(ADD_PARCEL, ADD_DEPOT, ADD_VEHICLE);
		final BufferedReader reader = new BufferedReader(new FileReader(file));

		final Point depotPosition = new Point(2.0, 2.5);
		final double truckSpeed = 30;
		sb.addEvent(new AddDepotEvent(0, depotPosition));
		for (int i = 0; i < numVehicles; i++) {
			sb.addEvent(new AddVehicleEvent(0, new VehicleDTO(depotPosition, truckSpeed, 0, TimeWindow.ALWAYS)));
		}
		String line;
		while ((line = reader.readLine()) != null) {
			final String[] parts = line.split(" ");
			final long requestArrivalTime = (long) (Double.parseDouble(parts[0]) * 1000.0);
			// FIXME currently filtering out first and last lines of file. Is
			// this ok?
			if (requestArrivalTime >= 0) {
				final long pickupServiceTime = Long.parseLong(parts[1]);
				final double pickupX = Double.parseDouble(parts[2]);
				final double pickupY = Double.parseDouble(parts[3]);
				final long pickupTimeWindowBegin = (long) (Double.parseDouble(parts[4]) * 1000.0);
				final long pickupTimeWindowEnd = (long) (Double.parseDouble(parts[5]) * 1000.0);
				final long deliveryServiceTime = Long.parseLong(parts[6]);
				final double deliveryX = Double.parseDouble(parts[7]);
				final double deliveryY = Double.parseDouble(parts[8]);
				final long deliveryTimeWindowBegin = (long) (Double.parseDouble(parts[9]) * 1000.0);
				final long deliveryTimeWindowEnd = (long) (Double.parseDouble(parts[10]) * 1000.0);

				final ParcelDTO dto = new ParcelDTO(new Point(pickupX, pickupY), new Point(deliveryX, deliveryY),
						new TimeWindow(pickupTimeWindowBegin, pickupTimeWindowEnd), new TimeWindow(
								deliveryTimeWindowBegin, deliveryTimeWindowEnd), 0, requestArrivalTime,
						pickupServiceTime, deliveryServiceTime);
				sb.addEvent(new AddParcelEvent(dto));
			}
		}
		return sb.build(new ScenarioCreator<Gendreau06Scenario>() {
			@Override
			public Gendreau06Scenario create(List<TimedEvent> eventList, Set<Enum<?>> eventTypes) {
				return new Gendreau06Scenario(eventList, eventTypes);
			}
		});
	}
}
