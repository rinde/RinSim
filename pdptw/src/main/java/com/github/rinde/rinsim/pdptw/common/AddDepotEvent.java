/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.auto.value.AutoValue;

/**
 * Event indicating that a depot can be created.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class AddDepotEvent implements TimedEvent {

  AddDepotEvent() {}

  /**
   * @return The position where the depot is to be added.
   */
  public abstract Point getPosition();

  /**
   * Create a new {@link AddDepotEvent} instance.
   * @param time The time at which the event is to be dispatched.
   * @param position {@link #getPosition()}
   * @return A new instance.
   */
  public static AddDepotEvent create(long time, Point position) {
    return new AutoValue_AddDepotEvent(time, position);
  }

  /**
   * Default {@link TimedEventHandler} that creates a {@link Depot} for every
   * {@link AddDepotEvent} that is received.
   * @return The default handler.
   */
  public static TimedEventHandler<AddDepotEvent> defaultHandler() {
    return Handler.INSTANCE;
  }

  enum Handler implements TimedEventHandler<AddDepotEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(AddDepotEvent event, SimulatorAPI sim) {
        sim.register(new Depot(event.getPosition()));
      }

      @Override
      public String toString() {
        return AddDepotEvent.class.getSimpleName() + ".defaultHandler()";
      }
    };
  }
}
