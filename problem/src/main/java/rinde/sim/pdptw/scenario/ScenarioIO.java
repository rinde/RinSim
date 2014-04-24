package rinde.sim.pdptw.scenario;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.unit.Unit;
import javax.xml.bind.DatatypeConverter;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.model.pdp.TimeWindowPolicy;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.common.VehicleDTO;
import rinde.sim.pdptw.scenario.PDPScenario.DefaultScenario;
import rinde.sim.pdptw.scenario.PDPScenario.ProblemClass;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Provides utilities for reading and writing scenario files.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class ScenarioIO {
  private ScenarioIO() {}

  private final static Gson gson = initialize();

  private final static Gson initialize() {
    final Type enumSetType = new TypeToken<Set<Enum<?>>>() {}.getType();

    final GsonBuilder builder = new GsonBuilder();
    builder
        .registerTypeHierarchyAdapter(ProblemClass.class,
            new ProblemClassSerializer())
        .registerTypeAdapter(Point.class, new PointAdapter())
        .registerTypeAdapter(TimeWindow.class, new TimeWindowAdapter())
        .registerTypeHierarchyAdapter(TimedEvent.class,
            new TimedEventSerializer())
        .registerTypeAdapter(enumSetType, new EnumSetSerializer())
        .registerTypeAdapter(Unit.class, new UnitSerializer())
        .registerTypeAdapter(Measure.class, new MeasureSerializer())
        .registerTypeHierarchyAdapter(TimeWindowPolicy.class,
            new TimeWindowPolicySerializer())
        .registerTypeAdapter(Enum.class, new EnumSerializer())
        .registerTypeAdapter(Predicate.class, new PredicateSerializer())
        .registerTypeAdapter(ImmutableList.class, new ImmutableListSerializer());

    return builder.create();
  }

  public static void write(Scenario s, File to) throws IOException {
    Files.write(write(s), to, Charsets.UTF_8);
  }

  public static PDPScenario read(File file) throws IOException {
    return read(file, DefaultScenario.class);
  }

  public static <T> T read(File file, Class<T> type) throws IOException {
    return read(Files.toString(file, Charsets.UTF_8), type);
  }

  static String write(Scenario s) {
    return gson.toJson(s);
  }

  static PDPScenario read(String s) {
    return read(s, DefaultScenario.class);
  }

  static <T> T read(String s, Class<T> type) {
    return gson.fromJson(s, type);
  }

  static class PointAdapter extends TypeAdapter<Point> {
    @Nullable
    @Override
    public Point read(@Nullable JsonReader reader) throws IOException {
      checkNotNull(reader);
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
      checkNotNull(writer);
      if (value == null) {
        writer.nullValue();
        return;
      }
      final String xy = value.x + "," + value.y;
      writer.value(xy);
    }
  }

  static class TimeWindowAdapter extends TypeAdapter<TimeWindow> {
    @Nullable
    @Override
    public TimeWindow read(@Nullable JsonReader reader) throws IOException {
      checkNotNull(reader);
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
      checkNotNull(writer);
      if (value == null) {
        writer.nullValue();
        return;
      }
      final String xy = value.begin + "," + value.end;
      writer.value(xy);
    }
  }

  static class TimedEventSerializer implements JsonDeserializer<TimedEvent> {
    @Override
    public TimedEvent deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT,
        @Nullable JsonDeserializationContext context) throws JsonParseException {
      checkNotNull(json);
      checkNotNull(context);

      final JsonObject obj = json.getAsJsonObject();

      final long time = obj.get("time").getAsLong();
      final Enum<?> type = context
          .deserialize(obj.get("eventType"), Enum.class);

      checkArgument(type instanceof PDPScenarioEvent);
      final PDPScenarioEvent scenEvent = (PDPScenarioEvent) type;
      switch (scenEvent) {
      case ADD_DEPOT:
        return new AddDepotEvent(time, (Point) context.deserialize(
            obj.get("position"), Point.class));
      case ADD_VEHICLE:
        return new AddVehicleEvent(time, (VehicleDTO) context.deserialize(
            obj.get("vehicleDTO"), VehicleDTO.class));
      case ADD_PARCEL:
        return new AddParcelEvent((ParcelDTO) context.deserialize(
            obj.get("parcelDTO"), ParcelDTO.class));
      case TIME_OUT:
        return new TimedEvent(type, time);
      case REMOVE_DEPOT:
        // fall through
      case REMOVE_PARCEL:
        // fall through
      case REMOVE_VEHICLE:
        // fall through
      default:
        throw new IllegalArgumentException("Event not supported: " + scenEvent);
      }
    }
  }

  static class EnumSetSerializer implements JsonDeserializer<Set<Enum<?>>>,
      JsonSerializer<Set<Enum<?>>> {
    @Override
    public Set<Enum<?>> deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT,
        @Nullable JsonDeserializationContext context) throws JsonParseException {
      checkNotNull(context);
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
      checkNotNull(src);
      checkNotNull(context);

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
      checkNotNull(json);
      return Unit.valueOf(json.getAsString());
    }

    @Override
    public JsonElement serialize(@Nullable Unit<?> src,
        @Nullable Type typeOfSrc,
        @Nullable JsonSerializationContext context) {
      checkNotNull(src);
      checkNotNull(context);
      return context.serialize(src.toString());
    }
  }

  static class MeasureSerializer implements
      JsonSerializer<Measure<?, ?>>,
      JsonDeserializer<Measure<?, ?>> {

    private static final String UNIT = "unit";
    private static final String VALUE = "value";
    private static final String VALUE_TYPE = "value-type";

    @Override
    public Measure<?, ?> deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT,
        @Nullable JsonDeserializationContext context) throws JsonParseException {
      checkNotNull(json);
      checkNotNull(context);
      checkArgument(json.isJsonObject());
      final JsonObject obj = json.getAsJsonObject();
      final Unit<?> unit = context.deserialize(obj.get(UNIT), Unit.class);

      try {
        final Class<?> type = Class.forName(obj.get(VALUE_TYPE).getAsString());
        final Number value = context.deserialize(obj.get(VALUE), type);

        if (type.equals(Double.TYPE) || type.equals(Double.class)) {
          return Measure.valueOf(value.doubleValue(), unit);
        } else if (type.equals(Integer.TYPE) || type.equals(Integer.class)) {
          return Measure.valueOf(value.intValue(), unit);
        } else if (type.equals(Long.TYPE) || type.equals(Long.class)) {
          return Measure.valueOf(value.longValue(), unit);
        }
        throw new IllegalArgumentException(type + " is not supported");
      } catch (final ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
    }

    @Override
    public JsonElement serialize(Measure<?, ?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      final JsonObject obj = new JsonObject();
      obj.add(UNIT, context.serialize(src.getUnit(), Unit.class));
      obj.add(VALUE, context.serialize(src.getValue()));
      obj.addProperty(VALUE_TYPE, src.getValue().getClass().getName());
      return obj;
    }

  }

  static class EnumSerializer implements
      JsonSerializer<Enum<?>>,
      JsonDeserializer<Enum<?>> {

    @Override
    public JsonElement serialize(Enum<?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      final String className = src.getDeclaringClass().getName();
      final String valueName = src.name();

      final JsonObject obj = new JsonObject();
      obj.addProperty("class", className);
      obj.addProperty("value", valueName);
      return obj;
    }

    @Override
    public Enum<?> deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT, @Nullable JsonDeserializationContext context)
        throws JsonParseException {
      checkNotNull(json);
      final JsonObject obj = json.getAsJsonObject();

      try {
        @SuppressWarnings("rawtypes")
        final Class clazz = Class.forName(obj.get("class").getAsString());
        @SuppressWarnings("unchecked")
        final Enum<?> en = Enum.valueOf(clazz, obj.get("value").getAsString());
        return en;
      } catch (final ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  static class TimeWindowPolicySerializer implements
      JsonSerializer<TimeWindowPolicy>,
      JsonDeserializer<TimeWindowPolicy> {

    @Override
    public TimeWindowPolicy deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT,
        @Nullable JsonDeserializationContext context) throws JsonParseException {
      checkNotNull(json);
      checkNotNull(context);
      return context.deserialize(json, Enum.class);
    }

    @Override
    public JsonElement serialize(TimeWindowPolicy src, Type typeOfSrc,
        JsonSerializationContext context) {
      if (src instanceof Enum<?>) {
        return context.serialize(src, Enum.class);
      }
      throw new IllegalArgumentException(
          "Only Enum implementations of the TimeWindowPolicy interface are allowed.");
    }

  }

  static String serializeObject(Object obj) throws IOException {
    final ByteArrayOutputStream bo = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(bo);
    oos.writeObject(obj);
    oos.flush();
    oos.close();
    return DatatypeConverter.printBase64Binary(bo.toByteArray());
  }

  static Object deserializeObject(String serialForm) throws IOException,
      ClassNotFoundException {
    final byte[] bytes = DatatypeConverter.parseBase64Binary(serialForm);
    final ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    final ObjectInputStream ois = new ObjectInputStream(is);
    final Object predicate = ois.readObject();
    ois.close();
    return predicate;
  }

  static class PredicateSerializer implements
      JsonSerializer<Predicate<?>>,
      JsonDeserializer<Predicate<?>> {

    @Override
    public Predicate<?> deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT,
        @Nullable JsonDeserializationContext context) throws JsonParseException {
      checkNotNull(json);
      checkArgument(json.isJsonPrimitive());

      try {
        final Predicate<?> obj = (Predicate<?>) deserializeObject(json
            .getAsString());
        return obj;
      } catch (final IOException e) {
        throw new IllegalArgumentException(e);
      } catch (final ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }

    }

    @Override
    public JsonElement serialize(Predicate<?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      if (src instanceof Serializable) {
        try {
          return new JsonPrimitive(serializeObject(src));
        } catch (final IOException e) {
          throw new IllegalArgumentException(e);
        }
      }
      throw new IllegalArgumentException(
          "All predicates must be serializable, found: "
              + src.getClass().getName());
    }
  }

  static class ImmutableListSerializer implements
      JsonSerializer<ImmutableList<?>>,
      JsonDeserializer<ImmutableList<?>> {

    @Override
    public ImmutableList<?> deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT, @Nullable JsonDeserializationContext context)
        throws JsonParseException {
      checkNotNull(json);
      checkNotNull(context);
      final ImmutableList.Builder<Object> builder = ImmutableList.builder();
      final Iterator<JsonElement> it = json.getAsJsonArray().iterator();
      while (it.hasNext()) {
        final JsonObject obj = it.next().getAsJsonObject();
        final String clazz = obj.get("class").getAsString();
        Class<?> clz;
        try {
          clz = Class.forName(clazz);
        } catch (final ClassNotFoundException e) {
          throw new IllegalArgumentException(e);
        }
        builder.add(context.deserialize(obj.get("value"), clz));
      }
      return builder.build();
    }

    @Override
    public JsonElement serialize(ImmutableList<?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      final JsonArray arr = new JsonArray();
      for (final Object item : src) {
        final JsonObject obj = new JsonObject();
        obj.add("class", new JsonPrimitive(item.getClass().getName()));
        obj.add("value", context.serialize(item, item.getClass()));
        arr.add(obj);
      }
      return arr;
    }

  }

  static class ProblemClassSerializer implements
      JsonSerializer<ProblemClass>,
      JsonDeserializer<ProblemClass> {

    @Override
    public ProblemClass deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT,
        @Nullable JsonDeserializationContext context) throws JsonParseException {
      checkNotNull(json);
      checkNotNull(context);
      return context.deserialize(json, Enum.class);
    }

    @Override
    public JsonElement serialize(@Nullable ProblemClass src,
        @Nullable Type typeOfSrc,
        @Nullable JsonSerializationContext context) {
      checkNotNull(context);
      checkNotNull(src);
      if (src instanceof Enum<?>) {
        return context.serialize(src, Enum.class);
      } else {
        throw new IllegalArgumentException(
            "Currently only enums are supported as ProblemClass instances, found: "
                + src.getClass());
      }
    }
  }

}
