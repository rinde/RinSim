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
package com.github.rinde.rinsim.scenario;

import com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent;
import com.github.rinde.rinsim.geom.Point;

/**
 * Event indicating that a depot can be created.
 * @author Rinde van Lon 
 */
public class AddDepotEvent extends TimedEvent {

  /**
   * The position where the depot is to be added.
   */
  public final Point position;

  /**
   * Create a new instance.
   * @param t The time at which the event is to be dispatched.
   * @param pPosition {@link #position}
   */
  public AddDepotEvent(long t, Point pPosition) {
    super(PDPScenarioEvent.ADD_DEPOT, t);
    position = pPosition;
  }

}
