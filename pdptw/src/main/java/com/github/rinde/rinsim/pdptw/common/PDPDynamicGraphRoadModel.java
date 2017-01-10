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
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

/**
 * Wraps {@link DynamicGraphRoadModel} instances similar to {@link PDPRoadModel}
 * , adds support for {@link DynamicGraphRoadModel} specific methods.
 * @author Rinde van Lon
 */
public class PDPDynamicGraphRoadModel
    extends PDPGraphRoadModel
    implements DynamicGraphRoadModel {

  private final DynamicGraphRoadModelImpl dgrm;

  PDPDynamicGraphRoadModel(DynamicGraphRoadModelImpl rm, boolean diversion) {
    super(rm, diversion);
    dgrm = rm;
  }

  @Override
  public boolean hasRoadUserOn(Point from, Point to) {
    return dgrm.hasRoadUserOn(from, to);
  }

  @Override
  public ImmutableSet<RoadUser> getRoadUsersOn(Point from, Point to) {
    return dgrm.getRoadUsersOn(from, to);
  }

  @Override
  public ImmutableSet<RoadUser> getRoadUsersOnNode(Point node) {
    return dgrm.getRoadUsersOnNode(node);
  }

  @Override
  public ListenableGraph<?> getGraph() {
    return dgrm.getGraph();
  }

  /**
   * Create a new builder for {@link PDPDynamicGraphRoadModel}.
   * @param delegateModelBuilder The {@link DynamicGraphRoadModel} builder to
   *          wrap.
   * @return A new {@link Builder} instance.
   */
  public static Builder builderForDynamicGraphRm(
      ModelBuilder<? extends DynamicGraphRoadModel, ? extends RoadUser> delegateModelBuilder) {
    return Builder.create(delegateModelBuilder, false);
  }

  /**
   * Builder for {@link PDPDynamicGraphRoadModel}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends AbstractBuilder<PDPDynamicGraphRoadModel, DynamicGraphRoadModel> {

    private static final long serialVersionUID = 6896293955429749530L;

    Builder() {
      setProvidingTypes(RoadModel.class, PDPRoadModel.class,
        GraphRoadModel.class, DynamicGraphRoadModel.class);
    }

    @Override
    public abstract ModelBuilder<DynamicGraphRoadModel, RoadUser> getDelegateModelBuilder();

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
    public PDPDynamicGraphRoadModel build(
        DependencyProvider dependencyProvider) {
      return new PDPDynamicGraphRoadModel(
        (DynamicGraphRoadModelImpl) getDelegateModelBuilder()
          .build(dependencyProvider),
        getAllowVehicleDiversion());
    }

    @SuppressWarnings("unchecked")
    static Builder create(
        ModelBuilder<? extends DynamicGraphRoadModel, ? extends RoadUser> delegateModelBuilder,
        boolean allowDiversion) {
      return new AutoValue_PDPDynamicGraphRoadModel_Builder(allowDiversion,
        (ModelBuilder<DynamicGraphRoadModel, RoadUser>) delegateModelBuilder);
    }
  }
}
