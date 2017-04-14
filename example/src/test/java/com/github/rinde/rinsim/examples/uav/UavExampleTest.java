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

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionPlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import org.junit.Test;

import javax.measure.unit.SI;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Hoang Tung Dinh
 */
public class UavExampleTest {

  private static final Point MIN_POINT = new Point(0, 0);
  private static final Point MAX_POINT = new Point(6000, 6000);
  private static final double MAX_SPEED = 1000d;

  @Test
  public void testCorrectMovement() {
    final Simulator sim = Simulator.builder()
        .addModel(RoadModelBuilders.plane()
            .withCollisionAvoidance()
            .withObjectRadius(1000)
            .withMinPoint(MIN_POINT)
            .withMaxPoint(MAX_POINT)
            .withDistanceUnit(SI.METER)
            .withSpeedUnit(SI.METERS_PER_SECOND)
            .withMaxSpeed(MAX_SPEED))
        .addModel(TimeModel.builder().withTickLength(500))
        .build();

    final UavAgent firstUav = new UavAgent(new Point(0, 0), new Point(3000, 3000));
    final UavAgent secondUav = new UavAgent(new Point(5000, 5000), new Point(3000, 3000));

    sim.register(firstUav);
    sim.register(secondUav);

    final CollisionPlaneRoadModel model = sim.getModelProvider()
        .getModel(CollisionPlaneRoadModel.class);

    final double firstUavMaxDistancePerTick = firstUav.getSpeed() * sim.getTimeStep() / 1.0e3;
    final double secondUavMaxDistancePerTick = secondUav.getSpeed() * sim.getTimeStep() / 1.0e3;

    Point secondUavPositionBeforeTick = model.getPosition(secondUav);
    Point firstUavPositionBeforeTick = model.getPosition(firstUav);

    final double tolerance = 1.0e-6;

    for (int i = 0; i < 340; i++) {
      sim.tick();
      final Point firstUavPositionAfterTick = model.getPosition(firstUav);
      final Point secondUavPositionAfterTick = model.getPosition(secondUav);

      assertThat(Point.distance(firstUavPositionBeforeTick, firstUavPositionAfterTick)).isLessThan(
          firstUavMaxDistancePerTick + tolerance);

      assertThat(
          Point.distance(secondUavPositionBeforeTick, secondUavPositionAfterTick)).isLessThan(
          secondUavMaxDistancePerTick + tolerance);

      firstUavPositionBeforeTick = firstUavPositionAfterTick;
      secondUavPositionBeforeTick = secondUavPositionAfterTick;
    }
  }
}