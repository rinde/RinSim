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
public class ForwardingCollisionGraphRoadModel<T extends CollisionGraphRoadModelImpl>
    extends ForwardingDynamicGraphRoadModel<T>
    implements CollisionGraphRoadModel {

  /**
   * Initializes a new instance that delegates all calls to the specified
   * {@link CollisionGraphRoadModelImpl}.
   * @param deleg The instance to which all calls are delegated.
   */
  protected ForwardingCollisionGraphRoadModel(T deleg) {
    super(deleg);
  }

  @Override
  public boolean isOccupied(Point node) {
    return delegate().isOccupied(node);
  }

  @Override
  public boolean isOccupiedBy(Point node, MovingRoadUser user) {
    return delegate().isOccupiedBy(node, user);
  }

  @Override
  public ImmutableSet<Point> getOccupiedNodes() {
    return delegate().getOccupiedNodes();
  }

  @Override
  public double getVehicleLength() {
    return delegate().getVehicleLength();
  }

  @Override
  public double getMinDistance() {
    return delegate().getMinDistance();
  }

  @Override
  public double getMinConnLength() {
    return delegate().getMinConnLength();
  }

}
