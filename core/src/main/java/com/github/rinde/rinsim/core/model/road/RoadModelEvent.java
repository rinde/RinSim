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

import com.github.rinde.rinsim.core.model.road.GenericRoadModel.RoadEventType;
import com.github.rinde.rinsim.event.Event;
import com.google.common.base.MoreObjects;

/**
 * Event representing a change in a {@link RoadModel}. See {@link RoadEventType}
 * for a list of event types.
 * @author Rinde van Lon
 */
public class RoadModelEvent extends Event {

  /**
   * The {@link RoadModel} that dispatched this event.
   */
  public final RoadModel roadModel;

  /**
   * The {@link RoadUser} that is involved in the event.
   */
  public final RoadUser roadUser;

  RoadModelEvent(Enum<?> type, RoadModel rm, RoadUser ru) {
    super(type, rm);
    roadModel = rm;
    roadUser = ru;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper("RoadModelEvent")
        .add("roadModel", roadModel)
        .add("roadUser", roadUser)
        .toString();
  }
}
