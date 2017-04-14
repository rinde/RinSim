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
package com.github.rinde.rinsim.core.model.road;

import static com.google.common.truth.Truth.assertThat;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.FakeDependencyProvider;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TrivialRoadUser;

public class CollisionPlaneRoadModelTest {

  final TrivialRoadUser ru1 = new TrivialRoadUser();
  final TrivialRoadUser ru2 = new TrivialRoadUser();
  final TrivialRoadUser ru3 = new TrivialRoadUser();
  final TrivialRoadUser ru4 = new TrivialRoadUser();
  final TrivialRoadUser ru5 = new TrivialRoadUser();

  CollisionPlaneRoadModel model;

  static TimeLapse tick() {
    return TimeLapseFactory.create(0, 1000);
  }

  @Before
  public void setUp() {
    model = RoadModelBuilders.plane()
      .withCollisionAvoidance()
      .withMinPoint(new Point(0, 0))
      .withMaxPoint(new Point(100, 100))
      .withDistanceUnit(SI.METER)
      .withSpeedUnit(SI.METERS_PER_SECOND)
      .withMaxSpeed(2)
      .withObjectRadius(.5d)
      .build(FakeDependencyProvider.builder()
        .add(TimeModel.builder().withTickLength(1000)).build());
  }

  @Test
  public void addObjectAtOccupiedPosition() {
    boolean fail = false;
    model.addObjectAt(ru1, new Point(1, 1));
    try {
      model.addObjectAt(ru2, new Point(1, 1));
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage())
        .contains("Cannot add an object on an occupied position:");
    }
    assertThat(fail).isTrue();
  }

  @Test
  public void moveToOccupiedPosition() {
    model.addObjectAt(ru1, new Point(1, 1));
    model.addObjectAt(ru2, new Point(2.5, 1));

    final MoveProgress mp = model.moveTo(ru1, new Point(2, 1), tick());

    assertThat(Point.distance(model.getPosition(ru1), new Point(1.5, 1)))
      .isAtMost(PlaneRoadModel.DELTA);
    assertThat(mp.distance().doubleValue(SI.METER))
      .isWithin(PlaneRoadModel.DELTA).of(.5);
  }

}
