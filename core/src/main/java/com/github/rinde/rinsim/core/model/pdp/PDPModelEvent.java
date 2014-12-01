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
package com.github.rinde.rinsim.core.model.pdp;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.event.Event;

/**
 * Event object that is dispatched by the {@link DefaultPDPModel}.
 * @author Rinde van Lon 
 */
public class PDPModelEvent extends Event {

  /**
   * The {@link DefaultPDPModel} that dispatched this event.
   */
  public final PDPModel pdpModel;

  /**
   * The time at which the event was dispatched.
   */
  public final long time;

  /**
   * The {@link Parcel} that was involved in the event, or <code>null</code> if
   * there was no {@link Parcel} involved in the event.
   */
  @Nullable
  public final Parcel parcel;

  /**
   * The {@link Vehicle} that was involved in the event, or <code>null</code> if
   * there was no {@link Vehicle} involved in the event.
   */
  @Nullable
  public final Vehicle vehicle;

  PDPModelEvent(PDPModelEventType type, PDPModel model, long t,
      @Nullable Parcel p, @Nullable Vehicle v) {
    super(type, model);
    pdpModel = model;
    time = t;
    parcel = p;
    vehicle = v;
  }
}
