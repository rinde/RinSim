/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;

/**
 * Wraps {@link GraphRoadModel} instances similar to {@link PDPRoadModel}, adds
 * support for {@link GraphRoadModel} specific methods.
 * @author Rinde van Lon
 */
public class PDPGraphRoadModel extends PDPRoadModel
    implements GraphRoadModel {

  private final GraphRoadModel grm;

  PDPGraphRoadModel(GraphRoadModelImpl rm, boolean diversion) {
    super(rm, diversion);
    grm = rm;
  }

  @Override
  public Graph<? extends ConnectionData> getGraph() {
    return grm.getGraph();
  }

  @Override
  public Optional<? extends Connection<?>> getConnection(RoadUser obj) {
    return grm.getConnection(obj);
  }

  /**
   * Create a new builder for {@link PDPGraphRoadModel}.
   * @param delegateModelBuilder The {@link GraphRoadModel} builder to wrap.
   * @return A new {@link Builder} instance.
   */
  public static Builder builderForGraphRm(
      ModelBuilder<? extends GraphRoadModel, ? extends RoadUser> delegateModelBuilder) {
    return Builder.create(delegateModelBuilder, false);
  }

  /**
   * Builder for {@link PDPGraphRoadModel}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends PDPRoadModel.AbstractBuilder<PDPGraphRoadModel, GraphRoadModel> {

    private static final long serialVersionUID = -5302130329728439504L;

    Builder() {
      setProvidingTypes(RoadModel.class, PDPRoadModel.class,
        GraphRoadModel.class);
    }

    @Override
    public abstract ModelBuilder<GraphRoadModel, RoadUser> getDelegateModelBuilder();

    @Override
    public Builder withAllowVehicleDiversion(boolean allowDiversion) {
      return create(getDelegateModelBuilder(), allowDiversion);
    }

    @Override
    public PDPGraphRoadModel build(DependencyProvider dependencyProvider) {
      return new PDPGraphRoadModel(
        (GraphRoadModelImpl) getDelegateModelBuilder()
          .build(dependencyProvider),
        getAllowVehicleDiversion());
    }

    @Override
    public String toString() {
      return Joiner.on("").join(
        PDPRoadModel.class.getSimpleName(),
        ".builderForGraphRm(", getDelegateModelBuilder(), ")");
    }

    @SuppressWarnings("unchecked")
    static Builder create(
        ModelBuilder<? extends GraphRoadModel, ? extends RoadUser> delegateModelBuilder,
        boolean allowDiversion) {
      return new AutoValue_PDPGraphRoadModel_Builder(allowDiversion,
        (ModelBuilder<GraphRoadModel, RoadUser>) delegateModelBuilder);
    }
  }
}
