/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import static com.google.common.collect.Lists.newArrayList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.ScenarioBuilder.ScenarioCreator;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class FabriRechtParser {

	public static FabriRechtScenario parse(String coordinateFile, String ordersFile) throws IOException {
		final ScenarioBuilder sb = new ScenarioBuilder(PDPScenarioEvent.ADD_DEPOT, PDPScenarioEvent.ADD_PARCEL,
				PDPScenarioEvent.ADD_VEHICLE, PDPScenarioEvent.REMOVE_DEPOT);

		final BufferedReader coordinateFileReader = new BufferedReader(new FileReader(coordinateFile));
		final BufferedReader ordersFileReader = new BufferedReader(new FileReader(ordersFile));

		final List<Point> coordinates = newArrayList();
		String line;
		int coordinateCounter = 0;
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		while ((line = coordinateFileReader.readLine()) != null) {
			final String[] parts = line.split(";");
			if (Integer.parseInt(parts[0]) != coordinateCounter) {
				throw new IllegalArgumentException("The coordinate file seems to be in an unrecognized format.");
			}
			final int x = Integer.parseInt(parts[1]);
			final int y = Integer.parseInt(parts[2]);

			minX = Math.min(x, minX);
			minY = Math.min(y, minY);
			maxX = Math.max(x, maxX);
			maxY = Math.max(y, maxY);

			coordinates.add(new Point(x, y));
			coordinateCounter++;
		}
		coordinateFileReader.close();

		final Point min = new Point(minX, minY);
		final Point max = new Point(maxX, maxY);
		//
		// sb.addMultipleEvents(0, 10, PDPScenarioEvent.ADD_DEPOT);

		// Anzahl der Fahrzeuge; Kapazität; untere Zeitfenstergrenze; obere
		// Zeitfenstergrenze
		final String[] firstLine = ordersFileReader.readLine().split(";");
		final int numVehicles = Integer.parseInt(firstLine[0]);
		final int capacity = Integer.parseInt(firstLine[1]);
		final long startTime = Long.parseLong(firstLine[2]);
		final long endTime = Long.parseLong(firstLine[3]);
		final TimeWindow timeWindow = new TimeWindow(startTime, endTime);

		for (int i = 0; i < numVehicles; i++) {
			sb.addEvent(new AddVehicleEvent(0, new VehicleDTO(coordinates.get(0), 1.0, capacity, timeWindow)));
		}

		// Nr. des Pickup-Orts; Nr. des Delivery-Orts; untere Zeitfenstergrenze
		// Pickup; obere Zeitfenstergrenze Pickup; untere Zeitfenstergrenze
		// Delivery; obere Zeitfenstergrenze Delivery; benötigte Kapazität;
		// Anrufzeit; Servicezeit Pickup; Servicezeit Delivery
		while ((line = ordersFileReader.readLine()) != null) {
			final String[] parts = line.split(";");
			final ParcelDTO o = new ParcelDTO(coordinates.get(Integer.parseInt(parts[0])), coordinates.get(Integer
					.parseInt(parts[1])), new TimeWindow(Long.parseLong(parts[2]), Long.parseLong(parts[3])),
					new TimeWindow(Long.parseLong(parts[4]), Long.parseLong(parts[5])), Integer.parseInt(parts[6]),
					Long.parseLong(parts[7]), Long.parseLong(parts[8]), Long.parseLong(parts[9]));

			sb.addEvent(new AddParcelEvent(o));
		}
		ordersFileReader.close();

		return sb.build(new ScenarioCreator<FabriRechtScenario>() {
			@Override
			public FabriRechtScenario create(List<TimedEvent> eventList, Set<Enum<?>> eventTypes) {
				return new FabriRechtScenario(eventList, eventTypes, min, max, timeWindow);
			}
		});
	}
}
