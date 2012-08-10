/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.ScenarioBuilder.ScenarioCreator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class FabriRechtParser {

	private final static Gson gson = initialize();

	public static FabriRechtScenario parse(String coordinateFile, String ordersFile) throws IOException {
		final ScenarioBuilder sb = new ScenarioBuilder(PDPScenarioEvent.ADD_DEPOT, PDPScenarioEvent.ADD_PARCEL,
				PDPScenarioEvent.ADD_VEHICLE, PDPScenarioEvent.TIME_OUT);

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
			if (Integer.parseInt(parts[0]) == 0) {
				sb.addEvent(new AddDepotEvent(0, new Point(x, y)));
			}
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

		sb.addEvent(new TimedEvent(PDPScenarioEvent.TIME_OUT, endTime));

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

	public static String toJson(FabriRechtScenario scenario) {
		return gson.toJson(scenario);
	}

	public static void toJson(FabriRechtScenario scenario, Writer writer) throws IOException {
		gson.toJson(scenario, FabriRechtScenario.class, writer);
		writer.close();
	}

	public static FabriRechtScenario fromJson(String json) {
		return gson.fromJson(json, FabriRechtScenario.class);
	}

	public static FabriRechtScenario fromJson(Reader reader) {
		return gson.fromJson(reader, FabriRechtScenario.class);
	}

	static class EnumDeserializer implements JsonDeserializer<Set<Enum<?>>>, JsonSerializer<Set<Enum<?>>> {
		@Override
		public Set<Enum<?>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			final Set<Enum<?>> eventTypes = newLinkedHashSet();
			final List<String> list = context.deserialize(json, new TypeToken<List<String>>() {}.getType());
			for (final String s : list) {
				eventTypes.add(PDPScenarioEvent.valueOf(s));
			}
			return eventTypes;
		}

		@Override
		public JsonElement serialize(Set<Enum<?>> src, Type typeOfSrc, JsonSerializationContext context) {
			final List<String> list = newArrayList();
			for (final Enum<?> e : src) {
				list.add(e.name());
			}
			return context.serialize(src, new TypeToken<List<String>>() {}.getType());
		}
	}

	static class PointAdapter extends TypeAdapter<Point> {
		@Override
		public Point read(JsonReader reader) throws IOException {
			if (reader.peek() == JsonToken.NULL) {
				reader.nextNull();
				return null;
			}
			final String xy = reader.nextString();
			final String[] parts = xy.split(",");
			final double x = Double.parseDouble(parts[0]);
			final double y = Double.parseDouble(parts[1]);
			return new Point(x, y);
		}

		@Override
		public void write(JsonWriter writer, Point value) throws IOException {
			if (value == null) {
				writer.nullValue();
				return;
			}
			final String xy = value.x + "," + value.y;
			writer.value(xy);
		}
	}

	static class TimeWindowAdapter extends TypeAdapter<TimeWindow> {
		@Override
		public TimeWindow read(JsonReader reader) throws IOException {
			if (reader.peek() == JsonToken.NULL) {
				reader.nextNull();
				return null;
			}
			final String xy = reader.nextString();
			final String[] parts = xy.split(",");
			final long x = Long.parseLong(parts[0]);
			final long y = Long.parseLong(parts[1]);
			return new TimeWindow(x, y);
		}

		@Override
		public void write(JsonWriter writer, TimeWindow value) throws IOException {
			if (value == null) {
				writer.nullValue();
				return;
			}
			final String xy = value.begin + "," + value.end;
			writer.value(xy);
		}
	}

	static class TimedEventDeserializer implements JsonDeserializer<TimedEvent> {
		@Override
		public TimedEvent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {

			final long time = json.getAsJsonObject().get("time").getAsLong();
			final PDPScenarioEvent type = PDPScenarioEvent.valueOf(json.getAsJsonObject().get("eventType")
					.getAsJsonObject().get("name").getAsString());

			switch (type) {
			case ADD_DEPOT:
				return new AddDepotEvent(time,
						(Point) context.deserialize(json.getAsJsonObject().get("position"), Point.class));
			case ADD_VEHICLE:
				return new AddVehicleEvent(time, (VehicleDTO) context.deserialize(json.getAsJsonObject()
						.get("vehicleDTO"), VehicleDTO.class));
			case ADD_PARCEL:
				return new AddParcelEvent(
						(ParcelDTO) context.deserialize(json.getAsJsonObject().get("parcelDTO"), ParcelDTO.class));
			case TIME_OUT:
				return new TimedEvent(type, time);
			}
			throw new IllegalStateException();
		}
	}

	private final static Gson initialize() {
		final Type collectionType = new TypeToken<Set<Enum<?>>>() {}.getType();

		final GsonBuilder builder = new GsonBuilder();
		// builder.setPrettyPrinting();
		builder.registerTypeAdapter(Point.class, new PointAdapter())
				.registerTypeAdapter(TimeWindow.class, new TimeWindowAdapter())
				.registerTypeHierarchyAdapter(TimedEvent.class, new TimedEventDeserializer())
				.registerTypeAdapter(collectionType, new EnumDeserializer());
		return builder.create();
	}
}
