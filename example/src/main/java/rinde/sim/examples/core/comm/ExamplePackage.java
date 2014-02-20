package rinde.sim.examples.core.comm;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

public class ExamplePackage implements RoadUser {
  public final String name;
  private Point location;

  public ExamplePackage(String name, Point location) {
    this.name = name;
    this.location = location;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    model.addObjectAt(this, location);
  }

  public Point getLocation() {
    return location;
  }
}
