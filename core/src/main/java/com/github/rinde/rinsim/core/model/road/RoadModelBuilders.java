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
package com.github.rinde.rinsim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.primitives.Doubles;

/**
 * This class is the main entry point to obtain builders for creating
 * {@link RoadModel}s. There are three kinds of road model implementations:
 * <ul>
 * <li>plane based, see {@link #plane()}</li>
 * <li>static graph based, see {@link #staticGraph(Graph)}</li>
 * <li>dynamic graph based, see {@link #dynamicGraph(ListenableGraph)}</li>
 * </ul>
 * @author Rinde van Lon
 */
public final class RoadModelBuilders {
  RoadModelBuilders() {}

  /**
   * @return A new {@link RoadModelBuilders.PlaneRMB} for creating a
   *         {@link PlaneRoadModel}.
   */
  public static RoadModelBuilders.PlaneRMB plane() {
    return PlaneRMB.create();
  }

  /**
   * Construct a new {@link RoadModelBuilders.StaticGraphRMB} for creating a
   * {@link GraphRoadModel}.
   * @param graph The graph which will be used as road structure.
   * @return A new {@link RoadModelBuilders.StaticGraphRMB}.
   */
  public static RoadModelBuilders.StaticGraphRMB staticGraph(Graph<?> graph) {
    return StaticGraphRMB.create(Suppliers.ofInstance(graph));
  }

  /**
   * Construct a new {@link RoadModelBuilders.StaticGraphRMB} for creating a
   * {@link GraphRoadModel}.
   * @param graphSupplier The supplier that creates a graph that will be used as
   *          road structure.
   * @return A new {@link RoadModelBuilders.StaticGraphRMB}.
   */
  public static RoadModelBuilders.StaticGraphRMB staticGraph(
    Supplier<? extends Graph<?>> graphSupplier) {
    return StaticGraphRMB.create(graphSupplier);
  }

  /**
   * Create a {@link RoadModelBuilders.DynamicGraphRMB} for constructing
   * {@link DynamicGraphRoadModel} instances.
   * @param g A {@link ListenableGraph}.
   * @return A new {@link RoadModelBuilders.DynamicGraphRMB} instance.
   */
  public static RoadModelBuilders.DynamicGraphRMB dynamicGraph(
    ListenableGraph<?> g) {
    return dynamicGraph(Suppliers.<ListenableGraph<?>> ofInstance(g));
  }

  /**
   * Create a {@link RoadModelBuilders.DynamicGraphRMB} for constructing
   * {@link DynamicGraphRoadModel} instances.
   * @param g A supplier of {@link ListenableGraph}.
   * @return A new {@link RoadModelBuilders.DynamicGraphRMB} instance.
   */
  public static RoadModelBuilders.DynamicGraphRMB dynamicGraph(
    Supplier<? extends ListenableGraph<?>> g) {
    return DynamicGraphRMB.create(g);
  }

  /**
   * Abstract builder for constructing subclasses of {@link RoadModel}.
   *
   * @param <T> The type of the model that the builder is constructing.
   * @param <S> The builder type itself, necessary to make an inheritance-based
   *          builder.
   * @author Rinde van Lon
   */
  public abstract static class AbstractRMB<T extends RoadModel, S> extends
    AbstractModelBuilder<T, RoadUser> {

    /**
     * The default distance unit: {@link SI#KILOMETER}.
     */
    protected static final Unit<Length> DEFAULT_DISTANCE_UNIT = SI.KILOMETER;

    /**
     * The default speed unit: {@link NonSI#KILOMETERS_PER_HOUR}.
     */
    protected static final Unit<Velocity> DEFAULT_SPEED_UNIT = NonSI.KILOMETERS_PER_HOUR;

    /**
     * @return the distanceUnit
     */
    public abstract Unit<Length> getDistanceUnit();

    /**
     * @return the speedUnit
     */
    public abstract Unit<Velocity> getSpeedUnit();

    /**
     * Returns a new copy of this builder with the specified distance unit used
     * for all distances. The default is {@link SI#KILOMETER}.
     * @param unit The distance unit to set.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public abstract S withDistanceUnit(Unit<Length> unit);

    /**
     * Returns a new copy of this builder with the specified speed unit to use
     * for all speeds. The default is {@link NonSI#KILOMETERS_PER_HOUR}.
     * @param unit The speed unit to set
     * @return A new builder instance.
     */
    @CheckReturnValue
    public abstract S withSpeedUnit(Unit<Velocity> unit);
  }

  /**
   * Abstract builder for constructing subclasses of {@link GraphRoadModel}.
   * @param <T> The type of the model that the builder is constructing.
   * @param <S> The builder type itself, necessary to make a inheritance-based
   *          builder.
   * @param <G> The type of the graph.
   * @author Rinde van Lon
   */
  public abstract static class AbstractGraphRMB<T extends GraphRoadModel, S, G extends Graph<?>>
    extends AbstractRMB<T, S> {

    /**
     * Create a new instance.
     */
    protected AbstractGraphRMB() {
      setProvidingTypes(RoadModel.class, GraphRoadModel.class);
    }

    /**
     * @return The graph supplier.
     */
    protected abstract Supplier<G> getGraphSupplier();

    /**
     * @return the graph
     */
    public G getGraph() {
      return getGraphSupplier().get();
    }
  }

  /**
   * Abstract builder for constructing subclasses of
   * {@link DynamicGraphRoadModel}.
   * @param <T> The type of the model that the builder is constructing.
   * @param <S> The builder type itself, necessary to make a inheritance-based
   *          builder.
   * @author Rinde van Lon
   */
  public abstract static class AbstractDynamicGraphRMB<T extends DynamicGraphRoadModel, S>
    extends AbstractGraphRMB<T, S, ListenableGraph<?>> {}

  /**
   * A builder for {@link PlaneRoadModel}. Instances can be obtained via
   * {@link #plane()}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class PlaneRMB extends
    AbstractRMB<PlaneRoadModel, PlaneRMB> implements Serializable {
    private static final long serialVersionUID = 8160700332762443917L;
    static final double DEFAULT_MAX_SPEED = 50d;
    static final Point DEFAULT_MIN_POINT = new Point(0, 0);
    static final Point DEFAULT_MAX_POINT = new Point(10, 10);

    PlaneRMB() {
      setProvidingTypes(RoadModel.class, PlaneRoadModel.class);
    }

    abstract Point getMin();

    abstract Point getMax();

    abstract double getMaxSpeed();

    /**
     * Returns a copy of this builder with the specified min point. The min
     * point defines the left top corner of the plane. The default is
     * <code>(0,0)</code>.
     * @param minPoint The min point to set.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public PlaneRMB withMinPoint(Point minPoint) {
      return create(getDistanceUnit(), getSpeedUnit(), minPoint, getMax(),
        getMaxSpeed());
    }

    /**
     * Returns a copy of this builder with the specified max point. The max
     * point defines the right bottom corner of the plane. The default is
     * <code>(10,10)</code>.
     * @param maxPoint The max point to set.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public PlaneRMB withMaxPoint(Point maxPoint) {
      return create(getDistanceUnit(), getSpeedUnit(), getMin(), maxPoint,
        getMaxSpeed());
    }

    /**
     * Returns a copy of this builder with the specified maximum speed. The
     * maximum speed will be used for all vehicles in the model. The default is
     * <code>50</code>.
     * @param maxSpeed The max speed to set.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public PlaneRMB withMaxSpeed(double maxSpeed) {
      checkArgument(maxSpeed > 0d,
        "Max speed must be strictly positive but is %s.",
        maxSpeed);
      return create(getDistanceUnit(), getSpeedUnit(), getMin(), getMax(),
        maxSpeed);
    }

    @Override
    public PlaneRMB withDistanceUnit(Unit<Length> unit) {
      return create(unit, getSpeedUnit(), getMin(), getMax(), getMaxSpeed());
    }

    @Override
    public PlaneRMB withSpeedUnit(Unit<Velocity> unit) {
      return create(getDistanceUnit(), unit, getMin(), getMax(), getMaxSpeed());
    }

    @Override
    public PlaneRoadModel build(DependencyProvider dependencyProvider) {
      checkArgument(
        getMin().x < getMax().x && getMin().y < getMax().y,
        "Min should have coordinates smaller than max, found min %s and max %s.",
        getMin(), getMax());
      return new PlaneRoadModel(this);
    }

    static PlaneRMB create() {
      return create(DEFAULT_DISTANCE_UNIT, DEFAULT_SPEED_UNIT,
        DEFAULT_MIN_POINT, DEFAULT_MAX_POINT, DEFAULT_MAX_SPEED);
    }

    static PlaneRMB create(Unit<Length> distanceUnit, Unit<Velocity> speedUnit,
      Point min, Point max, double maxSpeed) {
      return new AutoValue_RoadModelBuilders_PlaneRMB(distanceUnit, speedUnit,
        min, max, maxSpeed);
    }
  }

  /**
   * A builder for creating {@link GraphRoadModel} instances. Instances can be
   * obtained via {@link RoadModelBuilders#staticGraph(Graph)}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class StaticGraphRMB extends
    AbstractGraphRMB<GraphRoadModel, StaticGraphRMB, Graph<?>> implements
    Serializable {
    private static final long serialVersionUID = 1206566008918936928L;

    StaticGraphRMB() {
      setProvidingTypes(RoadModel.class, GraphRoadModel.class);
    }

    @Override
    protected abstract Supplier<Graph<?>> getGraphSupplier();

    @Override
    public StaticGraphRMB withDistanceUnit(Unit<Length> unit) {
      return create(unit, getSpeedUnit(), getGraphSupplier());
    }

    @Override
    public StaticGraphRMB withSpeedUnit(Unit<Velocity> unit) {
      return create(getDistanceUnit(), unit, getGraphSupplier());
    }

    /**
     * When this is called it will return a builder that creates
     * {@link CachedGraphRoadModel} instead.
     * @return A new {@link CachedGraphRMB} instance.
     */
    @CheckReturnValue
    public CachedGraphRMB withCache() {
      return CachedGraphRMB.create(getDistanceUnit(), getSpeedUnit(),
        getGraphSupplier());
    }

    @Override
    public GraphRoadModel build(DependencyProvider dependencyProvider) {
      return new GraphRoadModel(getGraph(), this);
    }

    static StaticGraphRMB create(Supplier<? extends Graph<?>> graph) {
      return create(DEFAULT_DISTANCE_UNIT, DEFAULT_SPEED_UNIT, graph);
    }

    @SuppressWarnings("unchecked")
    static StaticGraphRMB create(Unit<Length> distanceUnit,
      Unit<Velocity> speedUnit, Supplier<? extends Graph<?>> graph) {
      return new AutoValue_RoadModelBuilders_StaticGraphRMB(distanceUnit,
        speedUnit, (Supplier<Graph<?>>) graph);
    }
  }

  /**
   * A builder for constructing {@link DynamicGraphRoadModel} instances. Use
   * {@link RoadModelBuilders#dynamicGraph(ListenableGraph)} for obtaining
   * builder instances.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class DynamicGraphRMB extends
    AbstractDynamicGraphRMB<DynamicGraphRoadModel, DynamicGraphRMB> implements
    Serializable {

    private static final long serialVersionUID = 7269626100558413212L;

    @Override
    protected abstract Supplier<ListenableGraph<?>> getGraphSupplier();

    /**
     * Will return a new builder that constructs {@link CollisionGraphRoadModel}
     * instances instead of {@link DynamicGraphRoadModel} instances. Note that
     * all connections in the specified graph must have length
     * <code>2 * vehicleLength</code>, where vehicle length can be specified in
     * {@link CollisionGraphRMB#withVehicleLength(double)}.
     * @return A new {@link CollisionGraphRMB} instance.
     */
    @CheckReturnValue
    public CollisionGraphRMB withCollisionAvoidance() {
      return CollisionGraphRMB.create(this);
    }

    @Override
    public DynamicGraphRMB withDistanceUnit(Unit<Length> unit) {
      return create(unit, getSpeedUnit(), getGraphSupplier());
    }

    @Override
    public DynamicGraphRMB withSpeedUnit(Unit<Velocity> unit) {
      return create(getDistanceUnit(), unit, getGraphSupplier());
    }

    @Override
    public DynamicGraphRoadModel build(DependencyProvider dependencyProvider) {
      return new DynamicGraphRoadModel(getGraph(), this);
    }

    static DynamicGraphRMB create(
      Supplier<? extends ListenableGraph<?>> graphSupplier) {
      return create(DEFAULT_DISTANCE_UNIT, DEFAULT_SPEED_UNIT, graphSupplier);
    }

    @SuppressWarnings("unchecked")
    static DynamicGraphRMB create(Unit<Length> distanceUnit,
      Unit<Velocity> speedUnit,
      Supplier<? extends ListenableGraph<?>> graphSupplier) {
      return new AutoValue_RoadModelBuilders_DynamicGraphRMB(distanceUnit,
        speedUnit, (Supplier<ListenableGraph<?>>) graphSupplier);
    }
  }

  /**
   * Builder for {@link CachedGraphRoadModel} instances.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class CachedGraphRMB extends
    AbstractGraphRMB<CachedGraphRoadModel, CachedGraphRMB, Graph<?>> implements
    Serializable {

    private static final long serialVersionUID = -7837221650923727573L;

    @Override
    protected abstract Supplier<Graph<?>> getGraphSupplier();

    @Override
    public CachedGraphRoadModel build(DependencyProvider dependencyProvider) {
      return new CachedGraphRoadModel(getGraph(), this);
    }

    @Override
    public CachedGraphRMB withDistanceUnit(Unit<Length> unit) {
      return create(unit, getSpeedUnit(), getGraphSupplier());
    }

    @Override
    public CachedGraphRMB withSpeedUnit(Unit<Velocity> unit) {
      return create(getDistanceUnit(), unit, getGraphSupplier());
    }

    @SuppressWarnings("unchecked")
    static CachedGraphRMB create(Unit<Length> distanceUnit,
      Unit<Velocity> speedUnit, Supplier<? extends Graph<?>> graph) {
      return new AutoValue_RoadModelBuilders_CachedGraphRMB(distanceUnit,
        speedUnit, (Supplier<Graph<?>>) graph);
    }
  }

  /**
   * A builder for constructing {@link CollisionGraphRoadModel} instances.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class CollisionGraphRMB extends
    AbstractDynamicGraphRMB<CollisionGraphRoadModel, CollisionGraphRMB>
    implements Serializable {

    private static final long serialVersionUID = -5076770082090735004L;

    /**
     * The default vehicle length: <code>2</code>.
     */
    public static final double DEFAULT_VEHICLE_LENGTH = 2;

    /**
     * The default minimum distance: <code>.25</code>.
     */
    public static final double DEFAULT_MIN_DISTANCE = .25;

    @Override
    protected abstract Supplier<ListenableGraph<?>> getGraphSupplier();

    abstract double getVehicleLength();

    abstract double getMinDistance();

    CollisionGraphRMB() {
      setProvidingTypes(RoadModel.class, GraphRoadModel.class,
        DynamicGraphRoadModel.class, CollisionGraphRoadModel.class);
    }

    /**
     * Returns a copy of this builder with the specified vehicle length. The
     * vehicle length defines the length of each vehicle added to the
     * {@link CollisionGraphRoadModel} that will be constructed by this builder.
     * The vehicle length must be a strictly positive number. The default value
     * is {@link #DEFAULT_VEHICLE_LENGTH}.
     * @param length A length expressed in the unit set by
     *          {@link #withDistanceUnit(Unit)}.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public CollisionGraphRMB withVehicleLength(double length) {
      checkArgument(length > 0d,
        "Only positive vehicle lengths are allowed, found %s.", length);
      checkArgument(Doubles.isFinite(length),
        "%s is not a valid vehicle length.", length);
      return create(getDistanceUnit(), getSpeedUnit(), getGraphSupplier(),
        length, getMinDistance());
    }

    /**
     * Returns a copy of this builder with the specified min distance. The min
     * distance defines the minimum required distance between two vehicles. The
     * minimum distance must be a positive number &le; to 2 * vehicle length.
     * The default value is {@link #DEFAULT_MIN_DISTANCE}.
     * @param dist A distance expressed in the unit set by
     *          {@link #withDistanceUnit(Unit)}.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public CollisionGraphRMB withMinDistance(double dist) {
      checkArgument(dist >= 0d);
      return create(getDistanceUnit(), getSpeedUnit(), getGraphSupplier(),
        getVehicleLength(), dist);
    }

    @Override
    public CollisionGraphRMB withDistanceUnit(Unit<Length> unit) {
      return create(unit, getSpeedUnit(), getGraphSupplier(),
        getVehicleLength(), getMinDistance());
    }

    @Override
    public CollisionGraphRMB withSpeedUnit(Unit<Velocity> unit) {
      return create(getDistanceUnit(), unit, getGraphSupplier(),
        getVehicleLength(), getMinDistance());
    }

    @Override
    public CollisionGraphRoadModel build(DependencyProvider dependencyProvider) {
      final double minConnectionLength = getVehicleLength();
      checkArgument(
        getMinDistance() <= minConnectionLength,
        "Min distance must be smaller than 2 * vehicle length (%s), but is %s.",
        getVehicleLength(), getMinDistance());
      final ListenableGraph<?> graph = getGraph();

      for (final Connection<?> conn : graph.getConnections()) {
        CollisionGraphRoadModel
          .checkConnectionLength(minConnectionLength, conn);
      }
      return new CollisionGraphRoadModel(graph, minConnectionLength, this);
    }

    static CollisionGraphRMB create(DynamicGraphRMB builder) {
      return create(builder.getDistanceUnit(), builder.getSpeedUnit(),
        builder.getGraphSupplier(), DEFAULT_VEHICLE_LENGTH,
        DEFAULT_MIN_DISTANCE);
    }

    static CollisionGraphRMB create(Unit<Length> distanceUnit,
      Unit<Velocity> speedUnit,
      Supplier<ListenableGraph<?>> graphSupplier,
      double vehicleLength,
      double minDistance) {
      return new AutoValue_RoadModelBuilders_CollisionGraphRMB(distanceUnit,
        speedUnit, graphSupplier, vehicleLength, minDistance);
    }
  }
}
