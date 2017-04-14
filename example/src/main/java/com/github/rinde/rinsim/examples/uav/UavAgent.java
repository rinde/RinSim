package com.github.rinde.rinsim.examples.uav;

import com.github.rinde.rinsim.core.model.road.CollisionPlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/** @author Hoang Tung Dinh */
public final class UavAgent implements MovingRoadUser, TickListener {

  private Optional<CollisionPlaneRoadModel> roadModel;
  private final Point initialPosition;
  private final Point destination;

  UavAgent(Point initialPosition, Point destination) {
    this.initialPosition = initialPosition;
    this.destination = destination;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    roadModel = Optional.of((CollisionPlaneRoadModel) model);
    roadModel.get().addObjectAt(this, initialPosition);
  }

  @Override
  public double getSpeed() {
    return 5;
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    roadModel.get().moveTo(this, destination, timeLapse);
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}
}
