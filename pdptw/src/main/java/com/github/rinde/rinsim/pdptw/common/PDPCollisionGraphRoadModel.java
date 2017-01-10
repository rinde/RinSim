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
package com.github.rinde.rinsim.pdptw.common;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

/**
 * Wraps {@link CollisionGraphRoadModel} instances similar to
 * {@link PDPRoadModel}, adds support for {@link CollisionGraphRoadModel}
 * specific methods.
 * @author Rinde van Lon
 */
public class PDPCollisionGraphRoadModel
    extends PDPDynamicGraphRoadModel
    implements CollisionGraphRoadModel {

  private final CollisionGraphRoadModelImpl cgrm;

  PDPCollisionGraphRoadModel(CollisionGraphRoadModelImpl rm,
      boolean diversion) {
    super(rm, diversion);
    cgrm = rm;
  }

  @Override
  public boolean isOccupied(Point node) {
    return cgrm.isOccupied(node);
  }

  @Override
  public boolean isOccupiedBy(Point node, MovingRoadUser user) {
    return cgrm.isOccupiedBy(node, user);
  }

  @Override
  public ImmutableSet<Point> getOccupiedNodes() {
    return cgrm.getOccupiedNodes();
  }

  @Override
  public double getVehicleLength() {
    return cgrm.getVehicleLength();
  }

  @Override
  public double getMinDistance() {
    return cgrm.getMinDistance();
  }

  @Override
  public double getMinConnLength() {
    return cgrm.getMinConnLength();
  }

  /**
   * Create a new builder for {@link PDPCollisionGraphRoadModel}.
   * @param delegateModelBuilder The {@link CollisionGraphRoadModel} builder to
   *          wrap.
   * @return A new {@link Builder} instance.
   */
  public static Builder builderForCollisionGraphRm(
      ModelBuilder<? extends CollisionGraphRoadModel, ? extends RoadUser> delegateModelBuilder) {
    return Builder.create(delegateModelBuilder, false);
  }

  /**
   * Builder for {@link PDPCollisionGraphRoadModel}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends
      AbstractBuilder<PDPCollisionGraphRoadModel, CollisionGraphRoadModel> {

    private static final long serialVersionUID = 6896293955429749530L;

    Builder() {
      setProvidingTypes(RoadModel.class, PDPRoadModel.class,
        GraphRoadModel.class, DynamicGraphRoadModel.class,
        CollisionGraphRoadModel.class);
    }

    @Override
    public abstract ModelBuilder<CollisionGraphRoadModel, RoadUser> getDelegateModelBuilder();

    @Override
    public Builder withAllowVehicleDiversion(boolean allowDiversion) {
      return create(getDelegateModelBuilder(), allowDiversion);
    }

    @Override
    public String toString() {
      return Joiner.on("").join(
        PDPRoadModel.class.getSimpleName(),
        ".builderForGraphRm(", getDelegateModelBuilder(), ")");
    }

    @Override
    public PDPCollisionGraphRoadModel build(
        DependencyProvider dependencyProvider) {
      return new PDPCollisionGraphRoadModel(
        (CollisionGraphRoadModelImpl) getDelegateModelBuilder()
          .build(dependencyProvider),
        getAllowVehicleDiversion());
    }

    @SuppressWarnings("unchecked")
    static Builder create(
        ModelBuilder<? extends CollisionGraphRoadModel, ? extends RoadUser> delegateModelBuilder,
        boolean allowDiversion) {
      return new AutoValue_PDPCollisionGraphRoadModel_Builder(allowDiversion,
        (ModelBuilder<CollisionGraphRoadModel, RoadUser>) delegateModelBuilder);
    }
  }

}
