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
import static java.util.Objects.hash;

import java.util.Objects;

import javax.annotation.Nullable;
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
    return new RoadModelBuilders.PlaneRMB();
  }

  /**
   * Construct a new {@link RoadModelBuilders.StaticGraphRMB} for creating a
   * {@link GraphRoadModel}.
   * @param graph The graph which will be used as road structure.
   * @return A new {@link RoadModelBuilders.StaticGraphRMB}.
   */
  public static RoadModelBuilders.StaticGraphRMB staticGraph(Graph<?> graph) {
    return new RoadModelBuilders.StaticGraphRMB(Suppliers.ofInstance(graph));
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
    return new RoadModelBuilders.StaticGraphRMB(graphSupplier);
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
  @SuppressWarnings("unchecked")
  public static RoadModelBuilders.DynamicGraphRMB dynamicGraph(
    Supplier<? extends ListenableGraph<?>> g) {
    return new RoadModelBuilders.DynamicGraphRMB(
      (Supplier<ListenableGraph<?>>) g);
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

    private Unit<Length> distanceUnit;
    private Unit<Velocity> speedUnit;

    /**
     * Create instance.
     */
    protected AbstractRMB() {
      distanceUnit = SI.KILOMETER;
      speedUnit = NonSI.KILOMETERS_PER_HOUR;
    }

    /**
     * Should return the builder itself.
     * @return This.
     */
    protected abstract S self();

    /**
     * Sets the distance unit to for all dimensions. The default is
     * {@link SI#KILOMETER}.
     * @param unit The distance unit to set.
     * @return This, as per the builder pattern.
     */
    public S setDistanceUnit(Unit<Length> unit) {
      distanceUnit = unit;
      return self();
    }

    /**
     * Sets the speed unit to use for all speeds. The default is
     * {@link NonSI#KILOMETERS_PER_HOUR}.
     * @param unit The speed unit to set
     * @return This, as per the builder pattern.
     */
    public S setSpeedUnit(Unit<Velocity> unit) {
      speedUnit = unit;
      return self();
    }

    /**
     * @return the distanceUnit
     */
    public Unit<Length> getDistanceUnit() {
      return distanceUnit;
    }

    /**
     * @return the speedUnit
     */
    public Unit<Velocity> getSpeedUnit() {
      return speedUnit;
    }
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

    final Supplier<G> graphSupplier;

    /**
     * Create a new instance.
     * @param g The graph which will be used as road structure.
     */
    @SuppressWarnings("unchecked")
    protected AbstractGraphRMB(Supplier<? extends G> g) {
      graphSupplier = (Supplier<G>) g;
      setProvidingTypes(RoadModel.class, GraphRoadModel.class);
    }

    @Override
    public int hashCode() {
      return hash(graphSupplier, getDistanceUnit(), getSpeedUnit());
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (other == null || other.getClass() != getClass()) {
        return false;
      }
      final AbstractGraphRMB<?, ?, ?> o = (AbstractGraphRMB<?, ?, ?>) other;
      return Objects.equals(graphSupplier, o.graphSupplier)
        && Objects.equals(getDistanceUnit(), o.getDistanceUnit())
        && Objects.equals(getSpeedUnit(), o.getSpeedUnit());
    }

    /**
     * @return the graph
     */
    public G getGraph() {
      return graphSupplier.get();
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
    extends AbstractGraphRMB<T, S, ListenableGraph<?>> {

    /**
     * Create a new instance.
     * @param supplier Supplier of the graph that will be used as road
     *          structure.
     */
    protected AbstractDynamicGraphRMB(Supplier<ListenableGraph<?>> supplier) {
      super(supplier);
    }
  }

  /**
   * A builder for {@link PlaneRoadModel}. Instances can be obtained via
   * {@link #plane()}.
   * @author Rinde van Lon
   */
  public static final class PlaneRMB extends
    AbstractRMB<PlaneRoadModel, PlaneRMB> {
    static final double DEFAULT_MAX_SPEED = 50d;
    static final Point DEFAULT_MIN_POINT = new Point(0, 0);
    static final Point DEFAULT_MAX_POINT = new Point(10, 10);

    Point min;
    Point max;
    double maxSpeed;

    PlaneRMB() {
      setProvidingTypes(RoadModel.class, PlaneRoadModel.class);
      min = DEFAULT_MIN_POINT;
      max = DEFAULT_MAX_POINT;
      maxSpeed = DEFAULT_MAX_SPEED;
    }

    /**
     * Sets the min point that defines the left top corner of the plane. The
     * default is <code>(0,0)</code>.
     * @param minPoint The min point to set.
     * @return This, as per the builder pattern.
     */
    public PlaneRMB setMinPoint(Point minPoint) {
      min = minPoint;
      return self();
    }

    /**
     * Sets the max point that defines the right bottom corner of the plane. The
     * default is <code>(10,10)</code>.
     * @param maxPoint The max point to set.
     * @return This, as per the builder pattern.
     */
    public PlaneRMB setMaxPoint(Point maxPoint) {
      max = maxPoint;
      return self();
    }

    /**
     * Sets the maximum speed to use for all vehicles in the model. The default
     * is <code>50</code>.
     * @param speed The max speed to set.
     * @return This, as per the builder pattern.
     */
    public PlaneRMB setMaxSpeed(double speed) {
      checkArgument(speed > 0d,
        "Max speed must be strictly positive but is %s.",
        speed);
      maxSpeed = speed;
      return self();
    }

    @Override
    public PlaneRoadModel build(DependencyProvider dependencyProvider) {
      checkArgument(
        min.x < max.x && min.y < max.y,
        "Min should have coordinates smaller than max, found min %s and max %s.",
        min, max);
      return new PlaneRoadModel(this);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      final PlaneRMB o = (PlaneRMB) other;
      return Objects.equals(min, o.min)
        && Objects.equals(max, o.max)
        && Objects.equals(getDistanceUnit(), o.getDistanceUnit())
        && Objects.equals(getSpeedUnit(), o.getSpeedUnit())
        && Objects.equals(maxSpeed, o.maxSpeed);
    }

    @Override
    public int hashCode() {
      return Objects
        .hash(min, max, getDistanceUnit(), getSpeedUnit(), maxSpeed);
    }

    @Override
    protected PlaneRMB self() {
      return this;
    }
  }

  /**
   * A builder for creating {@link GraphRoadModel} instances. Instances can be
   * obtained via {@link RoadModelBuilders#staticGraph(Graph)}.
   * @author Rinde van Lon
   */
  public static final class StaticGraphRMB extends
    AbstractGraphRMB<GraphRoadModel, StaticGraphRMB, Graph<?>> {
    StaticGraphRMB(Supplier<? extends Graph<?>> g) {
      super(g);
    }

    @Override
    public GraphRoadModel build(DependencyProvider dependencyProvider) {
      return new GraphRoadModel(getGraph(), this);
    }

    /**
     * When this is called it will return a builder that creates
     * {@link CachedGraphRoadModel} instead.
     * @return A new {@link CachedGraphRMB} instance.
     */
    public CachedGraphRMB useCache() {
      return new CachedGraphRMB(graphSupplier)
        .setDistanceUnit(getDistanceUnit())
        .setSpeedUnit(getSpeedUnit());
    }

    @Override
    protected StaticGraphRMB self() {
      return this;
    }
  }

  /**
   * A builder for constructing {@link DynamicGraphRoadModel} instances. Use
   * {@link RoadModelBuilders#dynamicGraph(ListenableGraph)} for obtaining
   * builder instances.
   * @author Rinde van Lon
   */
  public static final class DynamicGraphRMB extends
    AbstractDynamicGraphRMB<DynamicGraphRoadModel, DynamicGraphRMB> {

    DynamicGraphRMB(Supplier<ListenableGraph<?>> g) {
      super(g);
    }

    /**
     * Will return a new builder that constructs {@link CollisionGraphRoadModel}
     * instances instead of {@link DynamicGraphRoadModel} instances. Note that
     * all connections in the specified graph must have length
     * <code>2 * vehicleLength</code>, where vehicle length can be specified in
     * {@link CollisionGraphRMB#setVehicleLength(double)}.
     * @return A new {@link CollisionGraphRMB} instance.
     */
    public CollisionGraphRMB avoidCollisions() {
      return new CollisionGraphRMB(
        graphSupplier)
        .setDistanceUnit(getDistanceUnit())
        .setSpeedUnit(getSpeedUnit());
    }

    @Override
    public DynamicGraphRoadModel build(DependencyProvider dependencyProvider) {
      return new DynamicGraphRoadModel(getGraph(), this);
    }

    @Override
    protected DynamicGraphRMB self() {
      return this;
    }
  }

  /**
   * Builder for {@link CachedGraphRoadModel} instances.
   * @author Rinde van Lon
   */
  public static final class CachedGraphRMB extends
    AbstractGraphRMB<CachedGraphRoadModel, CachedGraphRMB, Graph<?>> {

    CachedGraphRMB(Supplier<? extends Graph<?>> g) {
      super(g);
    }

    @Override
    public CachedGraphRoadModel build(DependencyProvider dependencyProvider) {
      return new CachedGraphRoadModel(getGraph(), this);
    }

    @Override
    protected CachedGraphRMB self() {
      return this;
    }
  }

  /**
   * A builder for constructing {@link CollisionGraphRoadModel} instances.
   * @author Rinde van Lon
   */
  public static final class CollisionGraphRMB extends
    AbstractDynamicGraphRMB<CollisionGraphRoadModel, CollisionGraphRMB> {

    /**
     * The default vehicle length: <code>2</code>.
     */
    public static final double DEFAULT_VEHICLE_LENGTH = 2;

    /**
     * The default minimum distance: <code>.25</code>.
     */
    public static final double DEFAULT_MIN_DISTANCE = .25;

    double vehicleLength;
    double minDistance;

    CollisionGraphRMB(Supplier<ListenableGraph<?>> g) {
      super(g);
      vehicleLength = DEFAULT_VEHICLE_LENGTH;
      minDistance = DEFAULT_MIN_DISTANCE;
    }

    /**
     * Sets the length of each vehicle added to the
     * {@link CollisionGraphRoadModel} that will be constructed by this builder.
     * The vehicle length must be a strictly positive number. The default value
     * is {@link #DEFAULT_VEHICLE_LENGTH}.
     * @param length A length expressed in the unit set by
     *          {@link #setDistanceUnit(Unit)}.
     * @return This, as per the builder pattern.
     */
    public CollisionGraphRMB setVehicleLength(double length) {
      checkArgument(length > 0d,
        "Only positive vehicle lengths are allowed, found %s.", length);
      checkArgument(Doubles.isFinite(length),
        "%s is not a valid vehicle length.", length);
      vehicleLength = length;
      return this;
    }

    /**
     * Sets the minimum required distance between two vehicles. The minimum
     * distance must be a positive number &le; to 2 * vehicle length. The
     * default value is {@link #DEFAULT_MIN_DISTANCE}.
     * @param dist A distance expressed in the unit set by
     *          {@link #setDistanceUnit(Unit)}.
     * @return This, as per the builder pattern.
     */
    public CollisionGraphRMB setMinDistance(double dist) {
      checkArgument(dist >= 0d);
      minDistance = dist;
      return this;
    }

    @Override
    public CollisionGraphRoadModel build(DependencyProvider dependencyProvider) {
      final double minConnectionLength = vehicleLength;
      checkArgument(
        minDistance <= minConnectionLength,
        "Min distance must be smaller than 2 * vehicle length (%s), but is %s.",
        vehicleLength, minDistance);
      final ListenableGraph<?> graph = getGraph();

      for (final Connection<?> conn : graph.getConnections()) {
        CollisionGraphRoadModel
          .checkConnectionLength(minConnectionLength, conn);
      }
      return new CollisionGraphRoadModel(graph, minConnectionLength, this);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (other == null || other.getClass() != getClass()) {
        return false;
      }
      final CollisionGraphRMB o = (CollisionGraphRMB) other;
      return Objects.equals(minDistance, o.minDistance)
        && Objects.equals(vehicleLength, o.vehicleLength)
        && super.equals(other);
    }

    @Override
    public int hashCode() {
      return hash(minDistance, vehicleLength, getGraph(), getDistanceUnit(),
        getSpeedUnit());
    }

    @Override
    protected CollisionGraphRMB self() {
      return this;
    }
  }
}
