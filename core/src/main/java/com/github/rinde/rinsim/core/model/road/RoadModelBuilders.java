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
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.primitives.Doubles;

/**
 * @author Rinde van Lon
 *
 */
public final class RoadModelBuilders {
  RoadModelBuilders() {}

  /**
   * @return A new {@link RoadModelBuilders.PlaneBuilder} for creating a
   *         {@link PlaneRoadModel}.
   */
  public static RoadModelBuilders.PlaneBuilder plane() {
    return new RoadModelBuilders.PlaneBuilder();
  }

  /**
   * Construct a new {@link RoadModelBuilders.GraphBuilder} for creating a
   * {@link GraphRoadModel}.
   * @param graph The graph which will be used as road structure.
   * @return A new {@link RoadModelBuilders.GraphBuilder}.
   */
  public static RoadModelBuilders.GraphBuilder staticGraph(Graph<?> graph) {
    return new RoadModelBuilders.GraphBuilder(Suppliers.ofInstance(graph));
  }

  /**
   * Construct a new {@link RoadModelBuilders.GraphBuilder} for creating a
   * {@link GraphRoadModel}.
   * @param graphSupplier The supplier that creates a graph that will be used as
   *          road structure.
   * @return A new {@link RoadModelBuilders.GraphBuilder}.
   */
  public static RoadModelBuilders.GraphBuilder staticGraph(
    Supplier<? extends Graph<?>> graphSupplier) {
    return new RoadModelBuilders.GraphBuilder(graphSupplier);
  }

  /**
   * Create a {@link RoadModelBuilders.DynamicBuilder} for constructing
   * {@link DynamicGraphRoadModel} instances.
   * @param g A {@link ListenableGraph}.
   * @return A new {@link RoadModelBuilders.DynamicBuilder} instance.
   */
  public static RoadModelBuilders.DynamicBuilder dynamicGraph(
    ListenableGraph<?> g) {
    return new RoadModelBuilders.DynamicBuilder(Suppliers.ofInstance(g));
  }

  /**
   * Create a {@link RoadModelBuilders.DynamicBuilder} for constructing
   * {@link DynamicGraphRoadModel} instances.
   * @param g A supplier of {@link ListenableGraph}.
   * @return A new {@link RoadModelBuilders.DynamicBuilder} instance.
   */
  public static RoadModelBuilders.DynamicBuilder dynamicGraph(
    Supplier<? extends ListenableGraph<?>> g) {
    return new RoadModelBuilders.DynamicBuilder(g);
  }

  /**
   * A builder for {@link PlaneRoadModel}. Instances can be obtained via
   * {@link #plane()}.
   * @author Rinde van Lon
   */
  public static class PlaneBuilder extends
    AbstractRoadModelBuilder<PlaneRoadModel, PlaneBuilder> {
    static final double DEFAULT_MAX_SPEED = 50d;
    static final Point DEFAULT_MIN_POINT = new Point(0, 0);
    static final Point DEFAULT_MAX_POINT = new Point(10, 10);

    Point min;
    Point max;
    double maxSpeed;

    PlaneBuilder() {
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
    public PlaneBuilder setMinPoint(Point minPoint) {
      min = minPoint;
      return self();
    }

    /**
     * Sets the max point that defines the right bottom corner of the plane. The
     * default is <code>(10,10)</code>.
     * @param maxPoint The max point to set.
     * @return This, as per the builder pattern.
     */
    public PlaneBuilder setMaxPoint(Point maxPoint) {
      max = maxPoint;
      return self();
    }

    /**
     * Sets the maximum speed to use for all vehicles in the model. The default
     * is <code>50</code>.
     * @param speed The max speed to set.
     * @return This, as per the builder pattern.
     */
    public PlaneBuilder setMaxSpeed(double speed) {
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
      final PlaneBuilder o = (PlaneBuilder) other;
      return Objects.equals(min, o.min)
        && Objects.equals(max, o.max)
        && Objects.equals(getDistanceUnit(), o.getDistanceUnit())
        && Objects.equals(getSpeedUnit(), o.getSpeedUnit())
        && Objects.equals(maxSpeed, o.maxSpeed)
        && AbstractModelBuilder.equal(this, o);
    }

    @Override
    public int hashCode() {
      return Objects
        .hash(min, max, getDistanceUnit(), getSpeedUnit(), maxSpeed);
    }

    @Override
    protected PlaneBuilder self() {
      return this;
    }
  }

  /**
   * Abstract builder for constructing subclasses of {@link GraphRoadModel}.
   * @param <T> The type of the model that the builder is constructing.
   * @param <S> The builder type itself, necessary to make a inheritance-based
   *          builder.
   * @author Rinde van Lon
   */
  public static abstract class AbstractGraphBuilder<T extends GraphRoadModel, S>
    extends AbstractRoadModelBuilder<T, S> {

    final Supplier<Graph<?>> graphSupplier;

    /**
     * Create a new instance.
     * @param g The graph which will be used as road structure.
     */
    @SuppressWarnings("unchecked")
    protected AbstractGraphBuilder(Supplier<? extends Graph<?>> g) {
      graphSupplier = (Supplier<Graph<?>>) g;
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
      final AbstractGraphBuilder<?, ?> o = (AbstractGraphBuilder<?, ?>) other;
      return Objects.equals(graphSupplier, o.graphSupplier)
        && Objects.equals(getDistanceUnit(), o.getDistanceUnit())
        && Objects.equals(getSpeedUnit(), o.getSpeedUnit());
    }

    /**
     * @return the graph
     */
    public Graph<?> getGraph() {
      return graphSupplier.get();
    }
  }

  /**
   * A builder for creating {@link GraphRoadModel} instances. Instances can be
   * obtained via {@link RoadModelBuilders#staticGraph(Graph)}.
   * @author Rinde van Lon
   */
  public static class GraphBuilder extends
    AbstractGraphBuilder<GraphRoadModel, GraphBuilder> {
    GraphBuilder(Supplier<? extends Graph<?>> g) {
      super(g);
    }

    @Override
    public GraphRoadModel build(DependencyProvider dependencyProvider) {
      return new GraphRoadModel(getGraph(), this);
    }

    /**
     * When this is called it will return a builder that creates
     * {@link CachedGraphRoadModel} instead.
     * @return A new {@link CachedBuilder} instance.
     */
    public CachedBuilder useCache() {
      return new CachedBuilder(graphSupplier)
        .setDistanceUnit(getDistanceUnit())
        .setSpeedUnit(getSpeedUnit());
    }

    @Override
    protected GraphBuilder self() {
      return this;
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
  public static abstract class AbstractDynamicBuilder<T extends DynamicGraphRoadModel, S>
    extends AbstractGraphBuilder<T, S> {

    /**
     * Create a new instance.
     * @param supplier Supplier of the graph that will be used as road
     *          structure.
     */
    protected AbstractDynamicBuilder(
      Supplier<? extends ListenableGraph<?>> supplier) {
      super(supplier);
    }

    @Override
    public ListenableGraph<?> getGraph() {
      return (ListenableGraph<?>) super.getGraph();
    }
  }

  /**
   * A builder for constructing {@link DynamicGraphRoadModel} instances. Use
   * {@link RoadModelBuilders#dynamicGraph(ListenableGraph)} for obtaining
   * builder instances.
   * @author Rinde van Lon
   */
  public static class DynamicBuilder extends
    AbstractDynamicBuilder<DynamicGraphRoadModel, DynamicBuilder> {

    DynamicBuilder(Supplier<? extends ListenableGraph<?>> g) {
      super(g);
    }

    /**
     * Will return a new builder that constructs {@link CollisionGraphRoadModel}
     * instances instead of {@link DynamicGraphRoadModel} instances. Note that
     * all connections in the specified graph must have length
     * <code>2 * vehicleLength</code>, where vehicle length can be specified in
     * {@link CollisionBuilder#setVehicleLength(double)}.
     * @return A new {@link CollisionBuilder} instance.
     */
    public CollisionBuilder avoidCollisions() {
      return new CollisionBuilder(getGraph())
        .setDistanceUnit(getDistanceUnit())
        .setSpeedUnit(getSpeedUnit());
    }

    @Override
    public DynamicGraphRoadModel build(DependencyProvider dependencyProvider) {
      return new DynamicGraphRoadModel(getGraph(), this);
    }

    @Override
    protected DynamicBuilder self() {
      return this;
    }
  }

  /**
   * Builder for {@link CachedGraphRoadModel} instances.
   * @author Rinde van Lon
   */
  public static class CachedBuilder extends
    AbstractGraphBuilder<CachedGraphRoadModel, CachedBuilder> {

    CachedBuilder(Supplier<? extends Graph<?>> g) {
      super(g);
    }

    @Override
    public CachedGraphRoadModel build(DependencyProvider dependencyProvider) {
      return new CachedGraphRoadModel(getGraph(), this);
    }

    @Override
    protected CachedBuilder self() {
      return this;
    }
  }

  /**
   * A builder for constructing {@link CollisionGraphRoadModel} instances.
   * @author Rinde van Lon
   */
  public static final class CollisionBuilder extends
    AbstractDynamicBuilder<CollisionGraphRoadModel, CollisionBuilder> {

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

    CollisionBuilder(ListenableGraph<?> g) {
      super(Suppliers.ofInstance(g));
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
    public CollisionBuilder setVehicleLength(double length) {
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
    public CollisionBuilder setMinDistance(double dist) {
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
      final CollisionBuilder o = (CollisionBuilder) other;
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
    protected CollisionBuilder self() {
      return this;
    }
  }

}
