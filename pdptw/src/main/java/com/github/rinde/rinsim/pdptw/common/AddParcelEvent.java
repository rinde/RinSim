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
package com.github.rinde.rinsim.pdptw.common;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.pdptw.DefaultParcel;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.auto.value.AutoValue;

/**
 * Event indicating that a parcel can be created.
 * @author Rinde van Lon
 */
@AutoValue
public abstract class AddParcelEvent implements TimedEvent {

  AddParcelEvent() {}

  /**
   * @return The data which should be used to instantiate a new parcel.
   */
  public abstract ParcelDTO getParcelDTO();

  /**
   * Creates a new {@link AddParcelEvent}.
   * @param dto The {@link ParcelDTO} that describes the parcel.
   * @return A new instance.
   */
  public static AddParcelEvent create(ParcelDTO dto) {
    return new AutoValue_AddParcelEvent(dto.getOrderAnnounceTime(), dto);
  }

  /**
   * Default {@link TimedEventHandler} that creates a {@link DefaultParcel} for
   * every {@link AddParcelEvent} that is received.
   * @return The default handler.
   */
  public static TimedEventHandler<AddParcelEvent> defaultHandler() {
    return Handler.INSTANCE;
  }

  enum Handler implements TimedEventHandler<AddParcelEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(AddParcelEvent event, SimulatorAPI sim) {
        sim.register(new DefaultParcel(event.getParcelDTO()));
      }

      @Override
      public String toString() {
        return AddParcelEvent.class.getSimpleName() + ".defaultHandler()";
      }
    };
  }
}
