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
package com.github.rinde.rinsim.examples.core.comm;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;

class ExamplePackage implements RoadUser {
  private final String name;
  private final Point location;

  ExamplePackage(String pName, Point pLocation) {
    name = pName;
    location = pLocation;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    model.addObjectAt(this, location);
  }

  Point getLocation() {
    return location;
  }

  String getName() {
    return name;
  }
}
