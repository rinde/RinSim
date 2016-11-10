/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.google.common.base.Optional;

/**
 * A {@link GraphRoadModel} which forwards all its method calls to another
 * {@link GraphRoadModel}. Subclasses should override one or more methods to
 * modify the behavior of the backing model as desired per the
 * <a href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator
 * pattern</a>.
 * @author Rinde van Lon
 */
public class ForwardingGraphRoadModel<T extends GraphRoadModelImpl>
    extends ForwardingRoadModel<T>
    implements GraphRoadModel {

  /**
   * Initializes a new instance that delegates all calls to the specified
   * {@link GenericRoadModel}.
   * @param deleg The instance to which all calls are delegated.
   */
  protected ForwardingGraphRoadModel(T deleg) {
    super(deleg);
  }

  @Override
  public Graph<? extends ConnectionData> getGraph() {
    return delegate().getGraph();
  }

  @Override
  public Optional<? extends Connection<?>> getConnection(RoadUser obj) {
    return delegate().getConnection(obj);
  }
}
