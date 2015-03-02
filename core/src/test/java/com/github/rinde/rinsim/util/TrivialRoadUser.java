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
package com.github.rinde.rinsim.util;

import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;

/**
 * 
 * Ignores the model registration.
 * @author Bartosz Michalik 
 * 
 */
public class TrivialRoadUser implements MovingRoadUser {

  private RoadModel model;

  public RoadModel getRoadModel() {
    return model;
  }

  @Override
  public void initRoadUser(RoadModel m) {
    model = m;
  }

  @Override
  public double getSpeed() {
    return 1d;
  }
}
