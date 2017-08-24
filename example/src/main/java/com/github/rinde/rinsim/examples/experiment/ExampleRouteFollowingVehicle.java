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
