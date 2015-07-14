/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.scenario;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.unit.Unit;
import javax.xml.bind.DatatypeConverter;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Provides utilities for reading and writing scenario files.
 * @author Rinde van Lon
 */
public final class ScenarioIO {
  private static final Gson GSON = initialize();
  private static final String VALUE_SEPARATOR = ",";
  private static final String VALUE = "value";
  private static final String CLAZZ = "class";

  private ScenarioIO() {}

  private static Gson initialize() {

    final GsonBuilder builder = new GsonBuilder();
    builder
        .registerTypeAdapter(ProblemClass.class, new ProblemClassIO())
        .registerTypeHierarchyAdapter(TimeWindowPolicy.class,
            new TimeWindowHierarchyIO())
        .registerTypeAdapter(Scenario.class, new ScenarioObjIO())
        .registerTypeAdapter(ParcelDTO.class, new ParcelIO())
        .registerTypeAdapter(VehicleDTO.class, new VehicleIO())
        .registerTypeAdapter(Point.class, new PointIO())
        .registerTypeAdapter(TimeWindow.class, new TimeWindowIO())
        .registerTypeAdapter(Unit.class, new UnitIO())
        .registerTypeAdapter(Measure.class, new MeasureIO())
        .registerTypeHierarchyAdapter(Enum.class, new EnumIO())
        .registerTypeAdapter(StopCondition.class, new StopConditionIO())
        .registerTypeAdapter(Class.class, new ClassIO())
        .registerTypeAdapter(ImmutableList.class, new ImmutableListIO())
        .registerTypeAdapter(ImmutableSet.class, new ImmutableSetIO())
        .registerTypeAdapter(ModelBuilder.class, new ModelBuilderIO());

    return builder.create();
  }

  /**
   * Writes the specified {@link Scenario} to disk in the JSON format.
   * @param s The scenario.
   * @param to The file to write to.
   * @throws IOException In case anything went wrong during writing the
   *           scenario.
   */
  public static void write(Scenario s, Path to) throws IOException {
    Files.write(to, Splitter.on(System.lineSeparator()).split(write(s)),
        Charsets.UTF_8);
  }

  /**
   * Reads a {@link Scenario} from disk.
   * @param file The file to read from.
   * @return A {@link Scenario} instance.
   * @throws IOException When reading fails.
   */
  public static Scenario read(Path file) throws IOException {
    return read(file, AutoValue_Scenario.class);
  }

  /**
   * Reads a scenario from disk.
   * @param file The file to read from.
   * @param type The type of scenario to read.
   * @param <T> The scenario type.
   * @return A scenario of type T.
   * @throws IOException When reading fails.
   */
  public static <T> T read(Path file, Class<T> type) throws IOException {
    return read(
        Joiner.on(System.lineSeparator()).join(
            Files.readAllLines(file, Charsets.UTF_8)),
        type);
  }

  /**
   * Writes the specified {@link Scenario} in JSON format.
   * @param s The scenario.
   * @return The scenario as JSON.
   */
  public static String write(Scenario s) {
    return GSON.toJson(s);
  }

  /**
   * Reads a {@link Scenario} from string.
   * @param scenarioString The string to read.
   * @return A {@link Scenario} instance.
   */
  public static Scenario read(String scenarioString) {
    return read(scenarioString, AutoValue_Scenario.class);
  }

  /**
   * Reads a {@link Scenario} from string.
   * @param s The string to read.
   * @param type The type of scenario to convert to.
   * @param <T> The scenario type.
   * @return A {@link Scenario} instance.
   */
  public static <T> T read(String s, Class<T> type) {
    return verifyNotNull(GSON.fromJson(s, type), "This is a bug in ScenarioIO");
  }

  /**
   * @return A {@link Function} that converts (reads) {@link Path}s into
   *         {@link Scenario} instances.
   */
  public static Function<Path, Scenario> reader() {
    return new DefaultScenarioReader<>();
  }

  /**
   * Allows to adapt the default reader ({@link #reader()}) by converting all
   * read scenarios. {@link ScenarioConverters} contains functions that can be
   * used for this.
   * @param converter A converter that transforms a scenario.
   * @return A new reader function.
   */
  public static Function<Path, Scenario> readerAdapter(
      Function<Scenario, Scenario> converter) {
    return Functions.compose(converter, reader());
  }

  /**
   * Creates a {@link Function} that converts {@link Path}s into the specified
   * subclass of {@link Scenario}.
   * @param clz The class instance to indicate the type scenario.
   * @param <T> The type of scenario.
   * @return A new reader instance.
   */
  public static <T extends Scenario> Function<Path, T> reader(Class<T> clz) {
    return new DefaultScenarioReader<>(clz);
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

  private static final class DefaultScenarioReader<T extends Scenario>
      implements Function<Path, T> {
    final Optional<Class<T>> clazz;

    DefaultScenarioReader() {
      clazz = Optional.absent();
    }

    DefaultScenarioReader(Class<T> clz) {
      clazz = Optional.of(clz);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public T apply(@Nullable Path input) {
      final Path in = verifyNotNull(input);
      try {
        if (clazz.isPresent()) {
          return verifyNotNull(read(in, clazz.get()));
        }
        return verifyNotNull((T) read(in));
      } catch (final IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  abstract static class SafeNullIO<T> implements JsonSerializer<T>,
      JsonDeserializer<T> {

    @Override
    public final JsonElement serialize(@Nullable T src,
        @Nullable Type typeOfSrc,
        @Nullable JsonSerializationContext context) {
      return verifyNotNull(doSerialize(verifyNotNull(src),
          verifyNotNull(typeOfSrc),
          verifyNotNull(context)));
    }

    @Override
    public final T deserialize(@Nullable JsonElement json,
        @Nullable Type typeOfT,
        @Nullable JsonDeserializationContext context) {
      return verifyNotNull(doDeserialize(verifyNotNull(json),
          verifyNotNull(typeOfT),
          verifyNotNull(context)), "found a null: %s", typeOfT, json);
    }

    abstract JsonElement doSerialize(T src, Type typeOfSrc,
        JsonSerializationContext context);

    abstract T doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context);
  }

  static class ScenarioObjIO extends SafeNullIO<Scenario> {

    @Override
    JsonElement doSerialize(Scenario src, Type typeOfSrc,
        JsonSerializationContext context) {
      final JsonObject obj = new JsonObject();
      obj.add(CLAZZ, new JsonPrimitive(src.getClass().getName()));
      obj.add(VALUE, context.serialize(src, src.getClass()));
      return obj;
    }

    @Override
    Scenario doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      final JsonObject obj = json.getAsJsonObject();
      final Class<?> clazz = context.deserialize(obj.get(CLAZZ), Class.class);
      return context.deserialize(obj.get(VALUE), clazz);
    }

  }

  static class ModelBuilderIO extends SafeNullIO<ModelBuilder<?, ?>> {
    @Override
    JsonElement doSerialize(ModelBuilder<?, ?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      final JsonObject obj = new JsonObject();
      obj.add(CLAZZ, new JsonPrimitive(src.getClass().getName()));
      obj.add(VALUE, context.serialize(src, src.getClass()));
      return obj;
    }

    @Override
    ModelBuilder<?, ?> doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      final JsonObject obj = json.getAsJsonObject();
      final Class<?> clazz = context.deserialize(obj.get(CLAZZ), Class.class);
      return context.deserialize(obj.get(VALUE), clazz);
    }
  }

  static class StopConditionIO extends SafeNullIO<StopCondition> {
    @Override
    JsonElement doSerialize(StopCondition src, Type typeOfSrc,
        JsonSerializationContext context) {
      final JsonObject obj = new JsonObject();
      obj.add(CLAZZ, new JsonPrimitive(src.getClass().getName()));
      obj.add(VALUE, context.serialize(src, src.getClass()));
      return obj;
    }

    @Override
    StopCondition doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      final JsonObject obj = json.getAsJsonObject();
      final Class<?> clazz = context.deserialize(obj.get(CLAZZ), Class.class);
      return context.deserialize(obj.get(VALUE), clazz);
    }
  }

  static class TimeWindowHierarchyIO extends SafeNullIO<TimeWindowPolicy> {
    @Override
    public TimeWindowPolicy doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      return context.deserialize(json, Enum.class);
    }

    @Override
    public JsonElement doSerialize(TimeWindowPolicy src, Type typeOfSrc,
        JsonSerializationContext context) {
      if (src instanceof Enum<?>) {
        return context.serialize(src, Enum.class);
      }
      throw new IllegalArgumentException(
          "Only Enum implementations of the TimeWindowPolicy interface are "
              + "allowed.");
    }
  }

  static class ProblemClassIO extends SafeNullIO<ProblemClass> {
    @Override
    public ProblemClass doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      final JsonObject obj = json.getAsJsonObject();
      final Class<?> clazz = context.deserialize(obj.get(CLAZZ), Class.class);
      return context.deserialize(obj.get(VALUE), clazz);
    }

    @Override
    public JsonElement doSerialize(ProblemClass src, Type typeOfSrc,
        JsonSerializationContext context) {
      final JsonObject obj = new JsonObject();
      obj.add(CLAZZ, new JsonPrimitive(src.getClass().getName()));
      obj.add(VALUE, context.serialize(src, src.getClass()));
      return obj;
    }
  }

  static class PointIO extends TypeAdapter<Point> {
    @Nullable
    @Override
    public Point read(@Nullable JsonReader reader) throws IOException {
      if (reader == null) {
        return null;
      }
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
    public void write(@Nullable JsonWriter writer, @Nullable Point value)
        throws IOException {
      if (writer == null) {
        return;
      }
      if (value == null) {
        writer.nullValue();
        return;
      }
      final String xy = value.x + VALUE_SEPARATOR + value.y;
      writer.value(xy);
    }
  }

  static class TimeWindowIO extends TypeAdapter<TimeWindow> {
    @Nullable
    @Override
    public TimeWindow read(@Nullable JsonReader reader) throws IOException {
      if (reader == null) {
        return null;
      }
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
    public void write(@Nullable JsonWriter writer, @Nullable TimeWindow value)
        throws IOException {
      if (writer == null) {
        return;
      }
      if (value == null) {
        writer.nullValue();
        return;
      }
      final String xy = value.begin + VALUE_SEPARATOR + value.end;
      writer.value(xy);
    }
  }

  static class UnitIO extends SafeNullIO<Unit<?>> {
    @Override
    public Unit<?> doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      return Unit.valueOf(json.getAsString());
    }

    @Override
    public JsonElement doSerialize(Unit<?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      return context.serialize(src.toString());
    }
  }

  static class MeasureIO extends SafeNullIO<Measure<?, ?>> {
    private static final String UNIT = "unit";
    private static final String VALUE_TYPE = "value-type";

    @Override
    public Measure<?, ?> doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
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
    public JsonElement doSerialize(Measure<?, ?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      final JsonObject obj = new JsonObject();
      obj.add(UNIT, context.serialize(src.getUnit(), Unit.class));
      obj.add(VALUE, context.serialize(src.getValue()));
      obj.addProperty(VALUE_TYPE, src.getValue().getClass().getName());
      return obj;
    }

  }

  static class EnumIO extends SafeNullIO<Enum<?>> {
    @Override
    public JsonElement doSerialize(Enum<?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      final String className = src.getDeclaringClass().getName();
      final String valueName = src.name();

      final JsonObject obj = new JsonObject();
      obj.addProperty(CLAZZ, className);
      obj.addProperty(VALUE, valueName);
      return obj;
    }

    @Override
    public Enum<?> doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      if (json.isJsonPrimitive()) {
        checkArgument(typeOfT instanceof Class<?>);
        return getEnum(((Class<?>) typeOfT).getName(), json.getAsString());
      }
      final JsonObject obj = json.getAsJsonObject();
      return getEnum(obj.get(CLAZZ).getAsString(), obj
          .get(VALUE).getAsString());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Enum<?> getEnum(String enumName, String value) {
      try {
        return Enum.valueOf((Class<Enum>) Class.forName(enumName), value);
      } catch (final ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  static class PredicateIO extends SafeNullIO<Predicate<?>> {
    @Override
    public Predicate<?> doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
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
    public JsonElement doSerialize(Predicate<?> src, Type typeOfSrc,
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

  static class ClassIO extends SafeNullIO<Class<?>> {
    @Override
    public Class<?> doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      checkArgument(json.isJsonPrimitive());
      try {
        return Class.forName(json.getAsString());
      } catch (final ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
    }

    @Override
    public JsonElement doSerialize(Class<?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      return new JsonPrimitive(src.getName());
    }
  }

  static class ParcelIO extends SafeNullIO<ParcelDTO> {
    @Override
    public ParcelDTO doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      final Iterator<JsonElement> it = json.getAsJsonArray().iterator();
      final Point p1 = context.deserialize(it.next(), Point.class);
      final Point p2 = context.deserialize(it.next(), Point.class);
      return Parcel
          .builder(p1, p2)
          .pickupTimeWindow(
              (TimeWindow) context.deserialize(it.next(), TimeWindow.class))
          .deliveryTimeWindow(
              (TimeWindow) context.deserialize(it.next(), TimeWindow.class))
          .neededCapacity(it.next().getAsDouble())
          .orderAnnounceTime(it.next().getAsLong())
          .pickupDuration(it.next().getAsLong())
          .deliveryDuration(it.next().getAsLong())
          .buildDTO();
    }

    @Override
    JsonElement doSerialize(ParcelDTO src, Type typeOfSrc,
        JsonSerializationContext context) {
      final JsonArray arr = new JsonArray();
      arr.add(context.serialize(src.getPickupLocation()));
      arr.add(context.serialize(src.getDeliveryLocation()));
      arr.add(context.serialize(src.getPickupTimeWindow()));
      arr.add(context.serialize(src.getDeliveryTimeWindow()));
      arr.add(context.serialize(src.getNeededCapacity()));
      arr.add(context.serialize(src.getOrderAnnounceTime()));
      arr.add(context.serialize(src.getPickupDuration()));
      arr.add(context.serialize(src.getDeliveryDuration()));
      return arr;
    }
  }

  static class VehicleIO extends SafeNullIO<VehicleDTO> {
    @Override
    public VehicleDTO doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      final Iterator<JsonElement> it = json.getAsJsonArray().iterator();
      return VehicleDTO
          .builder()
          .availabilityTimeWindow(
              (TimeWindow) context.deserialize(it.next(), TimeWindow.class))
          .capacity(it.next().getAsInt())
          .speed(it.next().getAsDouble())
          .startPosition((Point) context.deserialize(it.next(), Point.class))
          .build();
    }

    @Override
    JsonElement doSerialize(VehicleDTO src, Type typeOfSrc,
        JsonSerializationContext context) {
      final JsonArray arr = new JsonArray();
      arr.add(context.serialize(src.getAvailabilityTimeWindow()));
      arr.add(context.serialize(src.getCapacity()));
      arr.add(context.serialize(src.getSpeed()));
      arr.add(context.serialize(src.getStartPosition()));
      return arr;
    }
  }

  static class ImmutableListIO extends SafeNullIO<ImmutableList<?>> {
    @Override
    public ImmutableList<?> doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      final ImmutableList.Builder<Object> builder = ImmutableList.builder();
      final Iterator<JsonElement> it = json.getAsJsonArray().iterator();
      while (it.hasNext()) {
        final JsonObject obj = it.next().getAsJsonObject();
        final String clazz = obj.get(CLAZZ).getAsString();
        Class<?> clz;
        try {
          clz = Class.forName(clazz);
        } catch (final ClassNotFoundException e) {
          throw new IllegalArgumentException(e);
        }
        builder.add(context.deserialize(obj.get(VALUE), clz));
      }
      return builder.build();
    }

    @Override
    public JsonElement doSerialize(ImmutableList<?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      final JsonArray arr = new JsonArray();
      for (final Object item : src) {
        final JsonObject obj = new JsonObject();
        obj.add(CLAZZ, new JsonPrimitive(item.getClass().getName()));
        obj.add(VALUE, context.serialize(item, item.getClass()));
        arr.add(obj);
      }
      return arr;
    }
  }

  static class ImmutableSetIO extends SafeNullIO<ImmutableSet<?>> {
    @Override
    public ImmutableSet<?> doDeserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) {
      final ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
      final Iterator<JsonElement> it = json.getAsJsonArray().iterator();
      while (it.hasNext()) {
        final JsonObject obj = it.next().getAsJsonObject();
        final String clazz = obj.get(CLAZZ).getAsString();
        Class<?> clz;
        try {
          clz = Class.forName(clazz);
        } catch (final ClassNotFoundException e) {
          throw new IllegalArgumentException(e);
        }
        builder.add(context.deserialize(obj.get(VALUE), clz));
      }
      return builder.build();
    }

    @Override
    public JsonElement doSerialize(ImmutableSet<?> src, Type typeOfSrc,
        JsonSerializationContext context) {
      final JsonArray arr = new JsonArray();
      for (final Object item : src) {
        final JsonObject obj = new JsonObject();
        obj.add(CLAZZ, new JsonPrimitive(item.getClass().getName()));
        obj.add(VALUE, context.serialize(item, item.getClass()));
        arr.add(obj);
      }
      return arr;
    }
  }

}
