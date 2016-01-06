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

import java.io.Serializable;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import com.google.auto.value.AutoValue;
import com.google.common.primitives.Chars;

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
   * Default {@link TimedEventHandler} that creates a {@link Parcel} for every
   * {@link AddParcelEvent} that is received.
   * @return The default handler.
   */
  public static TimedEventHandler<AddParcelEvent> defaultHandler() {
    return Handler.INSTANCE;
  }

  /**
   * {@link TimedEventHandler} that creates {@link Parcel}s with an overridden
   * toString implementation. The first 26 parcels are named
   * <code>A,B,C..Y,Z</code>, parcel 27 to 702 are named
   * <code>AA,AB..YZ,ZZ</code>. If more than 702 parcels are created the
   * {@link TimedEventHandler} will throw an {@link IllegalStateException}. This
   * handler should only be used for debugging purposes and is not thread safe.
   * @return A newly constructed handler.
   */
  public static TimedEventHandler<AddParcelEvent> namedHandler() {
    return new NamedParcelCreator();
  }

  enum Handler implements TimedEventHandler<AddParcelEvent> {
    INSTANCE {
      @Override
      public void handleTimedEvent(AddParcelEvent event, SimulatorAPI sim) {
        sim.register(Parcel.builder(event.getParcelDTO()).build());
      }

      @Override
      public String toString() {
        return AddParcelEvent.class.getSimpleName() + ".defaultHandler()";
      }
    };
  }

  static class NamedParcelCreator
      implements TimedEventHandler<AddParcelEvent>, Serializable {
    private static final long serialVersionUID = 3888253170041895475L;
    long counter;

    NamedParcelCreator() {}

    @Override
    public void handleTimedEvent(AddParcelEvent event, SimulatorAPI simulator) {
      final String name;
      if (counter >= 26) {
        if (counter >= 702) {
          throw new IllegalStateException(
              "Too many parcels, this handler is meant for debuggin and should "
                  + "not be used in production.");
        }

        final char first =
          (char) ('A' + (int) Math.floor(counter / 26) - 1);
        final char second = (char) ('A' + counter % 26);
        name = Chars.join("", first, second);
      } else {
        name = Character.toString((char) (counter + 'A'));
      }
      counter++;
      simulator.register(
        Parcel.builder(event.getParcelDTO())
            .toString(name)
            .build());
    }

    @Override
    public String toString() {
      return AddParcelEvent.class.getSimpleName() + ".namedHandler()";
    }
  }
}
