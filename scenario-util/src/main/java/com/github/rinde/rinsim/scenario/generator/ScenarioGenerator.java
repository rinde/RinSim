/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.scenario.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.ForwardingRoadModel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.AbstractRMB;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.Scenario.AbstractBuilder;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.generator.Depots.DepotGenerator;
import com.github.rinde.rinsim.scenario.generator.DynamicSpeeds.DynamicSpeedGenerator;
import com.github.rinde.rinsim.scenario.generator.Parcels.ParcelGenerator;
import com.github.rinde.rinsim.scenario.generator.Vehicles.VehicleGenerator;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * A generator of {@link Scenario}s.
 * @author Rinde van Lon
 */
// TODO rename to Scenarios? or Generators?
public final class ScenarioGenerator {

  private static final Logger LOGGER =
    LoggerFactory.getLogger("ScenarioGenerator");

  // global properties
  final Builder builder;
  final ImmutableSet<ModelBuilder<?, ?>> modelBuilders;

  private final ParcelGenerator parcelGenerator;
  private final VehicleGenerator vehicleGenerator;
  private final DepotGenerator depotGenerator;
  private final ImmutableList<DynamicSpeedGenerator> dynamicSpeedGenerators;

  private final Unit<Velocity> speedUnit;
  private final Unit<Length> distanceUnit;
  private final Unit<Duration> timeUnit;

  ScenarioGenerator(Builder b) {
    builder = b;
    parcelGenerator = b.parcelGenerator;
    vehicleGenerator = b.vehicleGenerator;
    depotGenerator = b.depotGenerator;
    dynamicSpeedGenerators = ImmutableList.copyOf(b.dynamicSpeedGenerators);
    modelBuilders = ImmutableSet.copyOf(builder.modelSuppliers);

    final List<ModelBuilder<RoadModel, ?>> rmBuilders = findBuildersThatBuild(
      modelBuilders, RoadModel.class);
    checkArgument(rmBuilders.size() == 1,
      "Exactly one RoadModel builder must be supplied, found %s builders.",
      rmBuilders.size());
    final ModelBuilder<? extends RoadModel, ?> rmb = rmBuilders.get(0);
    final AbstractRMB<?, ?> roadBuilder;
    if (rmb instanceof ForwardingRoadModel.Builder) {
      final ModelBuilder<?, ?> delegate = ((ForwardingRoadModel.Builder<?>) rmb)
        .getDelegateModelBuilder();

      roadBuilder = (AbstractRMB<?, ?>) delegate;
    } else {
      roadBuilder = (AbstractRMB<?, ?>) rmb;
    }
    distanceUnit = roadBuilder.getDistanceUnit();
    speedUnit = roadBuilder.getSpeedUnit();

    final List<ModelBuilder<TimeModel, ?>> tmBuilders = findBuildersThatBuild(
      modelBuilders, TimeModel.class);
    checkArgument(tmBuilders.size() <= 1,
      "At most one TimeModel builder can be specified.");
    if (tmBuilders.isEmpty()) {
      timeUnit = TimeModel.AbstractBuilder.DEFAULT_TIME_UNIT;
    } else {
      timeUnit =
        ((TimeModel.AbstractBuilder<?>) tmBuilders.get(0)).getTimeUnit();
    }
  }

  @SuppressWarnings("unchecked")
  static <T extends Model<?>> List<ModelBuilder<T, ?>> findBuildersThatBuild(
      Iterable<? extends ModelBuilder<?, ?>> builders, Class<T> type) {
    final List<ModelBuilder<T, ?>> foundBuilders = new ArrayList<>();
    for (final ModelBuilder<?, ?> b : builders) {
      if (type.isAssignableFrom(b.getModelType())) {
        foundBuilders.add((ModelBuilder<T, ?>) b);
      }
    }
    return foundBuilders;
  }

  /**
   * @return The speed unit used in generated scenarios.
   */
  public Unit<Velocity> getSpeedUnit() {
    return speedUnit;
  }

  /**
   * @return The distance unit used in generated scenarios.
   */
  public Unit<Length> getDistanceUnit() {
    return distanceUnit;
  }

  /**
   * @return The time unit used in generated scenarios.
   */
  public Unit<Duration> getTimeUnit() {
    return timeUnit;
  }

  /**
   * @return The time window of generated scenarios.
   */
  public TimeWindow getTimeWindow() {
    return builder.getTimeWindow();
  }

  /**
   * @return The minimum position found in generated scenarios.
   */
  public Point getMin() {
    return parcelGenerator.getMin();
  }

  /**
   * @return The maximum position found in generated scenarios.
   */
  public Point getMax() {
    return parcelGenerator.getMax();
  }

  /**
   * @return The {@link ProblemClass} of the generated scenarios.
   */
  public ProblemClass getProblemClass() {
    return builder.problemClass;
  }

  /**
   * Generates a new {@link Scenario} instance.
   * @param rng The random number generator used for drawing random numbers.
   * @param id The id of this specific scenario.
   * @return A new instance.
   */
  // TODO change rng to seed?
  public Scenario generate(RandomGenerator rng, String id) {
    final ImmutableList.Builder<TimedEvent> b = ImmutableList.builder();

    LOGGER.info("Starting generation scenario");

    // depots
    LOGGER.info("- Generating Depots");
    final Iterable<? extends AddDepotEvent> depots = depotGenerator.generate(
      rng.nextLong(), parcelGenerator.getCenter());
    b.addAll(depots);

    // vehicles
    LOGGER.info("- Generating Vehicles");
    final ImmutableList<AddVehicleEvent> vehicles = vehicleGenerator.generate(
      rng.nextLong(), parcelGenerator.getCenter(),
      builder.getTimeWindow().end());
    b.addAll(vehicles);

    final TravelTimes tm = createTravelTimes(modelBuilders, getTimeUnit(),
      depots, vehicles);

    // parcels
    LOGGER.info("- Generating Parcels");
    b.addAll(parcelGenerator.generate(rng.nextLong(), tm,
      builder.getTimeWindow().end()));

    // dynamic speed events
    LOGGER.info("- Generating Dynamic Speed Events");
    for (final DynamicSpeedGenerator gen : dynamicSpeedGenerators) {
      b.addAll(gen.generate(rng.nextLong(), builder.getTimeWindow().end()));
    }
    // time out
    LOGGER.info("- Generating Time Out");
    b.add(TimeOutEvent.create(builder.getTimeWindow().end()));

    // create
    LOGGER.info("Building Scenario");
    return Scenario.builder(builder, builder.problemClass)
      .addModels(modelBuilders)
      .addEvents(b.build())
      .instanceId(id)
      .build();
  }

  /**
   * Create a {@link Builder} for constructing {@link ScenarioGenerator}s.
   * @param problemClass The {@link ProblemClass} of the scenarios that will be
   *          generated by the generator under construction.
   * @return The builder.
   */
  public static Builder builder(ProblemClass problemClass) {
    return new Builder(problemClass);
  }

  /**
   * Create a {@link Builder} for constructing {@link ScenarioGenerator}s.
   * @return The builder.
   */
  public static Builder builder() {
    return new Builder(Scenario.DEFAULT_PROBLEM_CLASS);
  }

  /**
   * Creates a {@link TravelTimes} instance based on the specified
   * {@link Scenario}.
   * @param s The scenario.
   * @return The travel times.
   */
  @SuppressWarnings("null")
  public static TravelTimes createTravelTimes(Scenario s) {
    final Iterable<AddDepotEvent> depots = FluentIterable.from(s.getEvents())
      .filter(AddDepotEvent.class);
    final Iterable<AddVehicleEvent> vehicles = FluentIterable.from(
      s.getEvents())
      .filter(AddVehicleEvent.class);

    final List<RoadModel> roadModels = newArrayList();

    Unit<Duration> timeUnit = TimeModel.AbstractBuilder.DEFAULT_TIME_UNIT;
    for (final ModelBuilder<?, ?> mb : s.getModelBuilders()) {
      if (RoadModel.class.isAssignableFrom(mb.getModelType())) {
        roadModels.add((RoadModel) mb.build(null));
      }
      if (TimeModel.class.isAssignableFrom(mb.getModelType())) {
        timeUnit = ((TimeModel.AbstractBuilder<?>) mb).getTimeUnit();
      }
    }
    checkArgument(roadModels.size() == 1);
    if (roadModels.get(0) instanceof GraphRoadModel) {
      return new GraphTravelTimes<>((GraphRoadModel) roadModels.get(0),
        timeUnit, depots,
        vehicles);
    }
    return new DefaultTravelTimes(roadModels.get(0), timeUnit, depots,
      vehicles);
  }

  static TravelTimes createTravelTimes(
      Iterable<? extends ModelBuilder<?, ?>> modelSuppliers,
      Unit<Duration> tu,
      Iterable<? extends AddDepotEvent> depots,
      Iterable<? extends AddVehicleEvent> vehicles) {
    final RoadModel rm = getRm(modelSuppliers);
    if (rm instanceof GraphRoadModel) {
      return new GraphTravelTimes<>((GraphRoadModel) rm, tu, depots,
        vehicles);
    }
    return new DefaultTravelTimes(rm, tu, depots, vehicles);
  }

  @SuppressWarnings("null")
  static RoadModel getRm(
      Iterable<? extends ModelBuilder<?, ?>> modelSuppliers) {
    for (final ModelBuilder<?, ?> sup : modelSuppliers) {
      if (RoadModel.class.isAssignableFrom(sup.getModelType())) {
        return (RoadModel) sup.build(null);
      }
    }
    throw new IllegalArgumentException("There is no RoadModel supplier in "
      + modelSuppliers + ".");
  }

  /**
   * Builder for creating {@link ScenarioGenerator} instances.
   * @author Rinde van Lon
   */
  public static class Builder extends AbstractBuilder<Builder> {
    static final ParcelGenerator DEFAULT_PARCEL_GENERATOR = Parcels.builder()
      .build();
    static final VehicleGenerator DEFAULT_VEHICLE_GENERATOR = Vehicles
      .builder().build();
    static final DepotGenerator DEFAULT_DEPOT_GENERATOR = Depots
      .singleCenteredDepot();
    static final List<DynamicSpeedGenerator> DEFAULT_DYNAMIC_SPEED_GENERATOR =
      DynamicSpeeds.zeroEvents();

    ParcelGenerator parcelGenerator;
    VehicleGenerator vehicleGenerator;
    DepotGenerator depotGenerator;
    List<DynamicSpeedGenerator> dynamicSpeedGenerators;
    final List<ModelBuilder<?, ?>> modelSuppliers;
    final ProblemClass problemClass;

    Builder(ProblemClass pc) {
      super(Optional.<AbstractBuilder<?>>absent());
      problemClass = pc;
      parcelGenerator = DEFAULT_PARCEL_GENERATOR;
      vehicleGenerator = DEFAULT_VEHICLE_GENERATOR;
      depotGenerator = DEFAULT_DEPOT_GENERATOR;
      dynamicSpeedGenerators = DEFAULT_DYNAMIC_SPEED_GENERATOR;
      modelSuppliers = newArrayList();
    }

    // copying constructor
    Builder(Builder b) {
      super(Optional.<AbstractBuilder<?>>of(b));
      problemClass = b.problemClass;
      parcelGenerator = b.parcelGenerator;
      vehicleGenerator = b.vehicleGenerator;
      depotGenerator = b.depotGenerator;
      dynamicSpeedGenerators = b.dynamicSpeedGenerators;
      modelSuppliers = newArrayList(b.modelSuppliers);
    }

    @Override
    protected Builder self() {
      return this;
    }

    /**
     * Set the {@link VehicleGenerator} to use for adding vehicles to the
     * scenario.
     * @param vg The vehicle generator.
     * @return This, as per the builder pattern.
     */
    public Builder vehicles(VehicleGenerator vg) {
      vehicleGenerator = vg;
      return this;
    }

    /**
     * Set the {@link ParcelGenerator} to use for adding parcels to the
     * scenario.
     * @param pg The parcel generator.
     * @return This, as per the builder pattern.
     */
    public Builder parcels(ParcelGenerator pg) {
      parcelGenerator = pg;
      return this;
    }

    /**
     * Set the {@link DepotGenerator} to use for adding depots to the scenario.
     * @param ds The depot generator.
     * @return This, as per the builder pattern.
     */
    public Builder depots(DepotGenerator ds) {
      depotGenerator = ds;
      return this;
    }

    /**
     * Set the {@link DynamicSpeedGenerator}s to use for adding dynamic changes
     * to maximum allowed to speed to the scenario.
     * @param dsg The generators.
     * @return This, as per the builder pattern.
     */
    public Builder dynamicSpeedGenerators(List<DynamicSpeedGenerator> dsg) {
      dynamicSpeedGenerators = dsg;
      return this;
    }

    /**
     * Add a builder of a {@link Model}. The provided model builder will use
     * default values provided by the {@link ScenarioGenerator} instance which
     * is currently being constructed.
     * @param modelBuilder The model builder to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModel(ModelBuilder<?, ?> modelBuilder) {
      modelSuppliers.add(modelBuilder);
      return this;
    }

    /**
     * @return Constructs a new {@link ScenarioGenerator} instance based on this
     *         builder.
     */
    public ScenarioGenerator build() {
      return new ScenarioGenerator(new Builder(this));
    }
  }

  /**
   * Implementations should provide information about travel times in a
   * scenario. The travel times are usually extracted from a {@link RoadModel}.
   * @author Rinde van Lon
   */
  public interface TravelTimes {
    /**
     * Computes the travel time between <code>from</code> and <code>to</code>
     * using the fastest available vehicle.
     * @param from The origin position.
     * @param to The destination position.
     * @return The expected travel time between the two positions.
     */
    long getShortestTravelTime(Point from, Point to);

    /**
     * Computes the travel time between <code>from</code> and the nearest depot
     * using the fastest available vehicle.
     * @param from The origin position.
     * @return The expected travel time between the two positions.
     */
    long getTravelTimeToNearestDepot(Point from);
  }

  static class DefaultTravelTimes implements TravelTimes {
    private final RoadModel roadModel;
    private final Measure<Double, Velocity> vehicleSpeed;
    private final Unit<Duration> timeUnit;
    private final ImmutableList<Point> depotLocations;

    DefaultTravelTimes(RoadModel rm, Unit<Duration> tu,
        Iterable<? extends AddDepotEvent> depots,
        Iterable<? extends AddVehicleEvent> vehicles) {
      roadModel = rm;

      double max = 0;
      for (final AddVehicleEvent ave : vehicles) {
        max = Math.max(max, ave.getVehicleDTO().getSpeed());
      }
      vehicleSpeed = Measure.valueOf(max, roadModel.getSpeedUnit());

      final ImmutableList.Builder<Point> depotBuilder = ImmutableList.builder();
      for (final AddDepotEvent ade : depots) {
        depotBuilder.add(ade.getPosition());
      }
      depotLocations = depotBuilder.build();

      timeUnit = tu;
    }

    @Override
    public long getShortestTravelTime(Point from, Point to) {
      final Iterator<Point> path = roadModel.getShortestPathTo(from, to)
        .iterator();

      long travelTime = 0L;
      Point prev = path.next();
      while (path.hasNext()) {
        final Point cur = path.next();
        final Measure<Double, Length> distance = Measure.valueOf(
          Point.distance(prev, cur), roadModel.getDistanceUnit());
        travelTime += RoadModels.computeTravelTime(vehicleSpeed, distance,
          timeUnit);
        prev = cur;
      }
      return travelTime;
    }

    @Override
    public long getTravelTimeToNearestDepot(Point from) {
      return getShortestTravelTime(from, findNearestDepot(from));
    }

    private Point findNearestDepot(Point from) {
      final Iterator<Point> it = depotLocations.iterator();
      Point nearestDepot = it.next();
      final double dist = Point.distance(from, nearestDepot);
      while (it.hasNext()) {
        final Point cur = it.next();
        final double d = Point.distance(from, cur);
        if (d < dist) {
          nearestDepot = cur;
        }
      }
      return nearestDepot;
    }
  }

  static class GraphTravelTimes<T extends ConnectionData>
      implements TravelTimes {
    private final GraphRoadModel roadModel;
    private final Measure<Double, Velocity> vehicleSpeed;
    private final Unit<Duration> timeUnit;
    private final ImmutableList<Point> depotLocations;

    GraphTravelTimes(GraphRoadModel rm, Unit<Duration> tu,
        Iterable<? extends AddDepotEvent> depots,
        Iterable<? extends AddVehicleEvent> vehicles) {
      roadModel = rm;

      double max = 0;
      for (final AddVehicleEvent ave : vehicles) {
        max = Math.max(max, ave.getVehicleDTO().getSpeed());
      }
      vehicleSpeed = Measure.valueOf(max, roadModel.getSpeedUnit());

      final ImmutableList.Builder<Point> depotBuilder = ImmutableList.builder();
      for (final AddDepotEvent ade : depots) {
        depotBuilder.add(ade.getPosition());
      }
      depotLocations = depotBuilder.build();

      timeUnit = tu;
    }

    GraphTravelTimes(GraphRoadModel rm, Unit<Duration> tu,
        Point centerMostPoint, Iterator<Vehicle> vehicles) {
      roadModel = rm;

      double max = 0;
      while (vehicles.hasNext()) {
        final Vehicle v = vehicles.next();
        max = Math.max(max, v.getDTO().getSpeed());
      }
      vehicleSpeed = Measure.valueOf(max, roadModel.getSpeedUnit());

      depotLocations = ImmutableList.of(centerMostPoint);

      this.timeUnit = tu;
    }

    @Override
    public long getShortestTravelTime(Point from, Point to) {
      final Iterator<Point> path = roadModel.getShortestPathTo(from, to)
        .iterator();

      long travelTime = 0L;
      Point prev = path.next();
      while (path.hasNext()) {
        final Point cur = path.next();
        @SuppressWarnings("unchecked")
        final Connection<T> conn =
          (Connection<T>) roadModel.getGraph().getConnection(prev, cur);

        final Measure<Double, Length> distance = Measure.valueOf(
          conn.getLength(), roadModel.getDistanceUnit());
        try {
          travelTime +=
            Math.min(RoadModels.computeTravelTime(vehicleSpeed, distance,
              timeUnit),
              RoadModels.computeTravelTime(
                Measure.valueOf(
                  ((MultiAttributeData) conn.data().get()).getMaxSpeed().get(),
                  roadModel.getSpeedUnit()),
                distance, timeUnit));
        } catch (final Exception e) {
          travelTime +=
            RoadModels.computeTravelTime(vehicleSpeed, distance,
              timeUnit);
        }
        prev = cur;
      }
      return travelTime;
      // TT := millis
      // conn.length := meter
      // speed := kmh
    }

    @Override
    public long getTravelTimeToNearestDepot(Point from) {
      return getShortestTravelTime(from, findNearestDepot(from));
    }

    private Point findNearestDepot(Point from) {
      final Iterator<Point> it = depotLocations.iterator();
      Point nearestDepot = it.next();
      final double dist =
        Graphs.pathLength(roadModel.getShortestPathTo(from, nearestDepot));
      while (it.hasNext()) {
        final Point cur = it.next();
        final double d =
          Graphs.pathLength(roadModel.getShortestPathTo(from, cur));
        if (d < dist) {
          nearestDepot = cur;
        }
      }
      return nearestDepot;
    }
  }
}
