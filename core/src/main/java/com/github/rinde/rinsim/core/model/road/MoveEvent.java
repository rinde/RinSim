/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.github.rinde.rinsim.core.model.road.AbstractRoadModel.RoadEventType;
import com.github.rinde.rinsim.event.Event;

/**
 * Event representing a move of a {@link MovingRoadUser}.
 * @author Rinde van Lon 
 */
public class MoveEvent extends Event {

  /**
   * The {@link RoadModel} that dispatched this event.
   */
  public final RoadModel roadModel;

  /**
   * The {@link MovingRoadUser} that moved.
   */
  public final MovingRoadUser roadUser;

  /**
   * Object containing the distance, time and path of this move.
   */
  public final MoveProgress pathProgress;

  MoveEvent(RoadModel rm, MovingRoadUser ru, MoveProgress pp) {
    super(RoadEventType.MOVE, rm);
    roadModel = rm;
    roadUser = ru;
    pathProgress = pp;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
