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
/** 
 * 
 */
package com.github.rinde.rinsim.core.model.road;

/**
 * A RoadUser is an object living on the {@link RoadModel}.
 * @author Rinde van Lon
 */
public interface RoadUser {
  /**
   * This is called when an road user can initialize itself.
   * @param model The model on which this RoadUser is registered.
   */
  void initRoadUser(RoadModel model);
}
