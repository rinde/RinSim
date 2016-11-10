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

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableSet;

/**
 *
 * @author Rinde van Lon
 */
public class ForwardingDynamicGraphRoadModel<T extends DynamicGraphRoadModelImpl>
    extends ForwardingGraphRoadModel<T>
    implements DynamicGraphRoadModel {

  /**
   * Initializes a new instance that delegates all calls to the specified
   * {@link DynamicGraphRoadModelImpl}.
   * @param deleg The instance to which all calls are delegated.
   */
  protected ForwardingDynamicGraphRoadModel(T deleg) {
    super(deleg);
  }

  @Override
  public boolean hasRoadUserOn(Point from, Point to) {
    return delegate().hasRoadUserOn(from, to);
  }

  @Override
  public ImmutableSet<RoadUser> getRoadUsersOn(Point from, Point to) {
    return delegate().getRoadUsersOn(from, to);
  }

  @Override
  public ImmutableSet<RoadUser> getRoadUsersOnNode(Point node) {
    return delegate().getRoadUsersOnNode(node);
  }
}
