package rinde.sim.pdptw.common;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.unit.Unit;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.pdptw.common.DynamicPDPTWScenario.ProblemClass;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class ScenarioIO {

  private final static Gson gson = initialize();

  private final static Gson initialize() {
    final Type collectionType = new TypeToken<Set<Enum<?>>>() {}.getType();

    final GsonBuilder builder = new GsonBuilder();
    builder
        .registerTypeHierarchyAdapter(ProblemClass.class,
            new ProblemClassSerializer())
        .registerTypeAdapter(Point.class, new PointAdapter())
        .registerTypeAdapter(TimeWindow.class, new TimeWindowAdapter())
        .registerTypeHierarchyAdapter(TimedEvent.class,
            new TimedEventDeserializer())
        .registerTypeAdapter(collectionType, new EnumSetDeserializer())
        .registerTypeAdapter(Unit.class, new UnitSerializer());
    return builder.create();
  }

  public static String write(Scenario s) {
    return gson.toJson(s);
  }

  public static <T> T read(String s, Class<T> type) {
    return gson.fromJson(s, type);
  }

  static class PointAdapter extends TypeAdapter<Point> {
    @Override
    public Point read(@Nullable JsonReader reader) throws IOException {
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
    public void write(@Nullable JsonWriter writer, @Nullable Point value)
        throws IOException {
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
    public TimeWindow read(@Nullable JsonReader reader) throws IOException {
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
    public void write(@Nullable JsonWriter writer, @Nullable TimeWindow value)
        throws IOException {
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
    public TimedEvent deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT,
        @Nullable JsonDeserializationContext context) throws JsonParseException {
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
      }
      throw new IllegalStateException();
    }
  }

  static class EnumSetDeserializer implements JsonDeserializer<Set<Enum<?>>>,
      JsonSerializer<Set<Enum<?>>> {
    @Override
    public Set<Enum<?>> deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT,
        @Nullable JsonDeserializationContext context) throws JsonParseException {
      final Set<Enum<?>> eventTypes = newLinkedHashSet();
      final List<String> list = context
          .deserialize(json, new TypeToken<List<String>>() {}.getType());
      for (final String s : list) {
        eventTypes.add(PDPScenarioEvent.valueOf(s));
      }
      return eventTypes;
    }

    @Override
    public JsonElement serialize(@Nullable Set<Enum<?>> src,
        @Nullable Type typeOfSrc,
        @Nullable JsonSerializationContext context) {
      final List<String> list = newArrayList();
      for (final Enum<?> e : src) {
        list.add(e.name());
      }
      return context.serialize(src, new TypeToken<List<String>>() {}.getType());
    }
  }

  static class UnitSerializer implements JsonSerializer<Unit<?>>,
      JsonDeserializer<Unit<?>> {

    @Override
    public Unit<?> deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT,
        @Nullable JsonDeserializationContext context) throws JsonParseException {
      return Unit.valueOf(json.getAsString());
    }

    @Override
    public JsonElement serialize(@Nullable Unit<?> src,
        @Nullable Type typeOfSrc,
        @Nullable JsonSerializationContext context) {
      return context.serialize(src.toString());
    }

  }

  static class ProblemClassSerializer implements
      JsonSerializer<ProblemClass>,
      JsonDeserializer<ProblemClass> {

    @SuppressWarnings("unchecked")
    @Override
    public ProblemClass deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT,
        @Nullable JsonDeserializationContext context) throws JsonParseException {
      Class<?> type;
      try {
        type = Class.forName(json.getAsJsonObject().get("type")
            .getAsString());
      } catch (final ClassNotFoundException e) {
        throw new IllegalArgumentException();
      }
      if (type.isEnum()) {
        return (ProblemClass) Enum.valueOf(type.asSubclass(Enum.class), json
            .getAsJsonObject()
            .get("value").getAsJsonObject()
            .get("name").getAsString());
      }
      else {
        throw new IllegalArgumentException("Non-enum type is not supported: "
            + type);
      }
    }

    @Override
    public JsonElement serialize(@Nullable ProblemClass src,
        @Nullable Type typeOfSrc,
        @Nullable JsonSerializationContext context) {
      final JsonObject obj = new JsonObject();
      obj.addProperty("type", src.getClass().getName());

      if (src instanceof Enum<?>) {
        obj.add("value", context.serialize(src, Enum.class));
        return obj;
      }
      else {
        throw new IllegalArgumentException(
            "Currently only enums are supported as ProblemClass instances.");
      }
    }
  }

}
