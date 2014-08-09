package com.github.rinde.rinsim.examples.core.comm;

import com.github.rinde.rinsim.core.graph.Point;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;

class ExamplePackage implements RoadUser {
  private final String name;
  private final Point location;

  ExamplePackage(String pName, Point pLocation) {
    name = pName;
    location = pLocation;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    model.addObjectAt(this, location);
  }

  Point getLocation() {
    return location;
  }

  String getName() {
    return name;
  }
}
