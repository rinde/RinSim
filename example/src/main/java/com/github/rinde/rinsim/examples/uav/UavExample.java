package com.github.rinde.rinsim.examples.uav;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;

import javax.measure.unit.SI;

/** @author Hoang Tung Dinh */
public final class UavExample {

  static final int SPEED_UP = 8;
  static final Point MIN_POINT = new Point(0, 0);
  static final Point MAX_POINT = new Point(6000, 6000);
  static final double MAX_SPEED = 1000d;

  public static void main(String[] args) {

    final Simulator sim = Simulator.builder()
        .addModel(RoadModelBuilders.plane()
            .withCollisionAvoidance()
            .withObjectRadius(1000)
            .withMinPoint(MIN_POINT)
            .withMaxPoint(MAX_POINT)
            .withDistanceUnit(SI.METER)
            .withSpeedUnit(SI.METERS_PER_SECOND)
            .withMaxSpeed(MAX_SPEED))
        .addModel(View.builder()
            .with(PlaneRoadModelRenderer.builder())
            .with(UavRenderer.builder())
            .withSpeedUp(SPEED_UP))
        .build();

    sim.register(new UavAgent(new Point(0, 0), new Point(3000, 3000)));
    sim.register(new UavAgent(new Point(5000, 5000), new Point(3000, 3000)));
    sim.start();
  }
}
