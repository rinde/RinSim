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
package com.github.rinde.rinsim.examples.uav;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionPlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.google.common.collect.ImmutableList;

/** @author Hoang Tung Dinh */
public final class UavExample {

  static final int SPEED_UP = 8;
  static final Point MIN_POINT = new Point(0, 0);
  static final Point MAX_POINT = new Point(6000, 6000);
  static final double MAX_SPEED = 1000d;

  private UavExample() {}

  public static void main(String[] args) {

    final Simulator sim =
      Simulator.builder()
        .addModel(TimeModel.builder().withTickLength(100))
        .addModel(
          RoadModelBuilders.plane()
            .withCollisionAvoidance()
            .withObjectRadius(300)
            .withMinPoint(MIN_POINT)
            .withMaxPoint(MAX_POINT)
            .withDistanceUnit(SI.METER)
            .withSpeedUnit(SI.METERS_PER_SECOND)
            .withMaxSpeed(MAX_SPEED))
        .addModel(
          View.builder()
            .with(PlaneRoadModelRenderer.builder())
            .with(UavRenderer.builder())
            .withAutoPlay()
            .withSpeedUp(SPEED_UP))
        .build();

    final ImmutableList<Point> initialPositions =
      ImmutableList.of(
        new Point(0, 0), new Point(0, 6000), new Point(6000, 0),
        new Point(6000, 6000));

    final CollisionPlaneRoadModel model =
      sim.getModelProvider().getModel(CollisionPlaneRoadModel.class);

    sim.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse timeLapse) {
        final List<RoadUser> list = model.getObjects().asList();

        for (int i = 0; i < list.size(); i++) {
          for (int j = i + 1; j < list.size(); j++) {

            final Point p1 = model.getPosition(list.get(i));
            final Point p2 = model.getPosition(list.get(j));
            final double dist = Point.distance(p1, p2);
            checkState(dist > 599, "%s is too close to %s, %s",
              list.get(i), list.get(j), dist);
          }
        }

      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    final RandomGenerator rng = new MersenneTwister(123);
    int counter = 0;
    for (final Point initPos : initialPositions) {
      sim.register(new UavAgent(rng, initPos, Integer.toString(counter++)));
    }
    sim.start();
  }
}
