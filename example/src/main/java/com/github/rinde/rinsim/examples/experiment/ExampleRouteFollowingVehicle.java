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
package com.github.rinde.rinsim.examples.experiment;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.pdp.PDPModelEvent;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.pdptw.common.RouteFollowingVehicle;

class ExampleRouteFollowingVehicle extends RouteFollowingVehicle {

  ExampleRouteFollowingVehicle(VehicleDTO dto,
      boolean allowDelayedRouteChanging) {
    super(dto, allowDelayedRouteChanging);
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    super.initRoadPDP(pRoadModel, pPdpModel);

    final RouteFollowingVehicle vehicle = this;
    getPDPModel().getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        if (!(e instanceof PDPModelEvent)) {
          return;
        }
        final List<Parcel> route = new ArrayList<>(vehicle.getRoute());
        final Parcel newlyAddedParcel = ((PDPModelEvent) e).parcel;
        route.add(newlyAddedParcel);
        route.add(newlyAddedParcel);
        vehicle.setRoute(route);
      }
    }, PDPModelEventType.NEW_PARCEL);
  }

  @Override
  public String toString() {
    return "Example car";
  }

}
