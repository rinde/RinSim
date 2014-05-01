/**
 * 
 */
package rinde.sim.pdptw.fabrirecht;

import static com.google.common.base.Preconditions.checkArgument;
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
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.common.VehicleDTO;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.ScenarioBuilder.ScenarioCreator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Parser for {@link FabriRechtScenario}s.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class FabriRechtParser {
  private static final Gson GSON = initialize();
  private static final String LINE_SEPARATOR = ";";
  private static final String VALUE_SEPARATOR = ",";

  private FabriRechtParser() {}

  /**
   * Parse Fabri & Recht scenario.
   * @param coordinateFile The coordinate file.
   * @param ordersFile The orders file.
   * @return The scenario.
   * @throws IOException When parsing fails.
   */
  public static FabriRechtScenario parse(String coordinateFile,
      String ordersFile) throws IOException {
    final ScenarioBuilder sb = new ScenarioBuilder(PDPScenarioEvent.ADD_DEPOT,
        PDPScenarioEvent.ADD_PARCEL, PDPScenarioEvent.ADD_VEHICLE,
        PDPScenarioEvent.TIME_OUT);

    final BufferedReader coordinateFileReader = new BufferedReader(
        new FileReader(coordinateFile));
    final BufferedReader ordersFileReader = new BufferedReader(new FileReader(
        ordersFile));

    final List<Point> coordinates = newArrayList();
    String line;
    int coordinateCounter = 0;
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    while ((line = coordinateFileReader.readLine()) != null) {
      final String[] parts = line.split(LINE_SEPARATOR);
      if (Integer.parseInt(parts[0]) != coordinateCounter) {
        coordinateFileReader.close();
        ordersFileReader.close();
        throw new IllegalArgumentException(
            "The coordinate file seems to be in an unrecognized format.");
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

    // Anzahl der Fahrzeuge; Kapazität; untere Zeitfenstergrenze; obere
    // Zeitfenstergrenze
    final String firstLineString = ordersFileReader.readLine();
    checkArgument(firstLineString != null);
    final String[] firstLine = firstLineString.split(LINE_SEPARATOR);
    // line 0 contains number of vehicles, but this is not needed
    final int capacity = Integer.parseInt(firstLine[1]);
    final long startTime = Long.parseLong(firstLine[2]);
    final long endTime = Long.parseLong(firstLine[3]);
    final TimeWindow timeWindow = new TimeWindow(startTime, endTime);

    sb.addEvent(new TimedEvent(PDPScenarioEvent.TIME_OUT, endTime));
    final VehicleDTO defaultVehicle = VehicleDTO.builder()
        .startPosition(coordinates.get(0))
        .speed(1d)
        .capacity(capacity)
        .availabilityTimeWindow(timeWindow)
        .build();

    // Nr. des Pickup-Orts; Nr. des Delivery-Orts; untere Zeitfenstergrenze
    // Pickup; obere Zeitfenstergrenze Pickup; untere Zeitfenstergrenze
    // Delivery; obere Zeitfenstergrenze Delivery; benötigte Kapazität;
    // Anrufzeit; Servicezeit Pickup; Servicezeit Delivery
    while ((line = ordersFileReader.readLine()) != null) {
      final String[] parts = line.split(LINE_SEPARATOR);

      final int neededCapacity = 1;

      final ParcelDTO o = ParcelDTO
          .builder(coordinates.get(Integer
              .parseInt(parts[0])), coordinates.get(Integer.parseInt(parts[1])))
          .pickupTimeWindow(
              new TimeWindow(Long.parseLong(parts[2]), Long.parseLong(parts[3])))
          .deliveryTimeWindow(
              new TimeWindow(Long.parseLong(parts[4]), Long.parseLong(parts[5])))
          .neededCapacity(neededCapacity)
          .orderAnnounceTime(Long.parseLong(parts[7]))
          .pickupDuration(Long.parseLong(parts[8]))
          .deliveryDuration(Long.parseLong(parts[9]))
          .build();

      sb.addEvent(new AddParcelEvent(o));
    }
    ordersFileReader.close();

    return sb.build(new ScenarioCreator<FabriRechtScenario>() {
      @Override
      public FabriRechtScenario create(List<TimedEvent> eventList,
          Set<Enum<?>> eventTypes) {
        return new FabriRechtScenario(eventList, eventTypes, min, max,
            timeWindow, defaultVehicle);
      }
    });
  }

  static String toJson(FabriRechtScenario scenario) {
    return GSON.toJson(scenario);
  }

  /**
   * Write the scenario to disk in JSON format.
   * @param scenario The scenario to write.
   * @param writer The writer to use.
   * @throws IOException When writing fails.
   */
  public static void toJson(FabriRechtScenario scenario, Writer writer)
      throws IOException {
    GSON.toJson(scenario, FabriRechtScenario.class, writer);
    writer.close();
  }

  static FabriRechtScenario fromJson(String json) {
    return GSON.fromJson(json, FabriRechtScenario.class);
  }

  /**
   * Read a scenario from JSON string.
   * @param json The JSON string.
   * @param numVehicles The number of vehicles in the resulting scenario.
   * @param vehicleCapacity The vehicle capacity of the vehicles in the
   *          resulting scenario.
   * @return The scenario.
   */
  public static FabriRechtScenario fromJson(String json, int numVehicles,
      int vehicleCapacity) {
    final FabriRechtScenario scen = fromJson(json);
    return change(scen, numVehicles, vehicleCapacity);
  }

  /**
   * Read a scenario from JSON reader.
   * @param reader The JSON reader.
   * @param numVehicles The number of vehicles in the resulting scenario.
   * @param vehicleCapacity The vehicle capacity of the vehicles in the
   *          resulting scenario.
   * @return The scenario.
   */
  public static FabriRechtScenario fromJson(Reader reader, int numVehicles,
      int vehicleCapacity) {
    final FabriRechtScenario scen = fromJson(reader);
    return change(scen, numVehicles, vehicleCapacity);
  }

  /**
   * Read a scenario from JSON reader.
   * @param reader The JSON reader.
   * @return The scenario.
   */
  public static FabriRechtScenario fromJson(Reader reader) {
    return GSON.fromJson(reader, FabriRechtScenario.class);
  }

  static FabriRechtScenario change(FabriRechtScenario scen, int numVehicles,
      int vehicleCapacity) {
    final List<TimedEvent> events = newArrayList();
    for (int i = 0; i < numVehicles; i++) {
      events.add(new AddVehicleEvent(0,
          VehicleDTO.builder()
              .use(scen.defaultVehicle)
              .capacity(vehicleCapacity)
              .build()
          ));
    }
    events.addAll(scen.asList());
    return new FabriRechtScenario(events, scen.getPossibleEventTypes(),
        scen.min, scen.max, scen.timeWindow, scen.defaultVehicle);
  }

  static class EnumDeserializer implements JsonDeserializer<Set<Enum<?>>>,
      JsonSerializer<Set<Enum<?>>> {
    @Override
    public Set<Enum<?>> deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      final Set<Enum<?>> eventTypes = newLinkedHashSet();
      final List<String> list = context
          .deserialize(json, new TypeToken<List<String>>() {}.getType());
      for (final String s : list) {
        eventTypes.add(PDPScenarioEvent.valueOf(s));
      }
      return eventTypes;
    }

    @Override
    public JsonElement serialize(Set<Enum<?>> src, Type typeOfSrc,
        JsonSerializationContext context) {
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
      final String[] parts = xy.split(VALUE_SEPARATOR);
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
      final String xy = value.x + VALUE_SEPARATOR + value.y;
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
      final String[] parts = xy.split(VALUE_SEPARATOR);
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
      final String xy = value.begin + VALUE_SEPARATOR + value.end;
      writer.value(xy);
    }
  }

  static class TimedEventDeserializer implements JsonDeserializer<TimedEvent> {
    @Override
    public TimedEvent deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {

      final long time = json.getAsJsonObject().get("time").getAsLong();
      final PDPScenarioEvent type = PDPScenarioEvent.valueOf(json
          .getAsJsonObject().get("eventType").getAsJsonObject().get("name")
          .getAsString());

      switch (type) {
      case ADD_DEPOT:
        return new AddDepotEvent(time, (Point) context.deserialize(json
            .getAsJsonObject().get("position"), Point.class));
      case ADD_VEHICLE:
        return new AddVehicleEvent(time, (VehicleDTO) context.deserialize(json
            .getAsJsonObject().get("vehicleDTO"), VehicleDTO.class));
      case ADD_PARCEL:
        return new AddParcelEvent((ParcelDTO) context.deserialize(json
            .getAsJsonObject().get("parcelDTO"), ParcelDTO.class));
      case TIME_OUT:
        return new TimedEvent(type, time);
      case REMOVE_DEPOT:
        // FALL THROUGH
      case REMOVE_PARCEL:
        // FALL THROUGH
      case REMOVE_VEHICLE:
        // FALL THROUGH
      default:
        throw new UnsupportedOperationException();
      }
    }
  }

  private static Gson initialize() {
    final Type collectionType = new TypeToken<Set<Enum<?>>>() {}.getType();
    final GsonBuilder builder = new GsonBuilder();
    builder
        .registerTypeAdapter(Point.class, new PointAdapter())
        .registerTypeAdapter(TimeWindow.class, new TimeWindowAdapter())
        .registerTypeHierarchyAdapter(TimedEvent.class,
            new TimedEventDeserializer())
        .registerTypeAdapter(collectionType, new EnumDeserializer());
    return builder.create();
  }
}
