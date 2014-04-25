package rinde.sim.examples.core.comm;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

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
