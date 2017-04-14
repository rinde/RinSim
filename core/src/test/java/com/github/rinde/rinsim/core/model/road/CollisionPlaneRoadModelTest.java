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

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.FakeDependencyProvider;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TrivialRoadUser;
import com.google.common.base.Optional;

public class CollisionPlaneRoadModelTest {

  final TrivialRoadUser ru1 = new TrivialRoadUser();
  final TrivialRoadUser ru2 = new TrivialRoadUser();
  final TrivialRoadUser ru3 = new TrivialRoadUser();
  final TrivialRoadUser ru4 = new TrivialRoadUser();
  final TrivialRoadUser ru5 = new TrivialRoadUser();

  private static final long tickLength = 250;
  CollisionPlaneRoadModel model;

  static TimeLapse tick() {
    return TimeLapseFactory.create(0, tickLength);
  }

  @Before
  public void setUp() {
    model = RoadModelBuilders.plane()
      .withCollisionAvoidance()
      .withMinPoint(new Point(0, 0))
      .withMaxPoint(new Point(100, 100))
      .withDistanceUnit(SI.METER)
      .withSpeedUnit(SI.METERS_PER_SECOND)
      .withMaxSpeed(1)
      .withObjectRadius(.5d)
      .build(FakeDependencyProvider.builder()
        .add(TimeModel.builder()
          .withTickLength(tickLength))
        .build());
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
    assertThat(Point.distance(model.getPosition(ru1), new Point(1.25, 1)))
      .isAtMost(PlaneRoadModel.DELTA);
    assertThat(mp.distance().doubleValue(SI.METER))
      .isWithin(PlaneRoadModel.DELTA).of(.25);

    final MoveProgress mp2 = model.moveTo(ru1, new Point(2, 1), tick());
    assertThat(Point.distance(model.getPosition(ru1), new Point(1.5, 1)))
      .isAtMost(PlaneRoadModel.DELTA);
    assertThat(mp2.distance().doubleValue(SI.METER))
      .isWithin(PlaneRoadModel.DELTA).of(.25);

    model.addObjectAt(ru3, new Point(3.5, 2));
    model.moveTo(ru3, new Point(2.5, 1), tick());

    assertThat(Point.distance(model.getPosition(ru3), model.getPosition(ru2)))
      .isGreaterThan(1d);

    model.moveTo(ru3, new Point(2.5, 1), tick());
    assertThat(Point.distance(model.getPosition(ru3), model.getPosition(ru2)))
      .isWithin(PlaneRoadModel.DELTA).of(1d);

    final MoveProgress mp3 = model.moveTo(ru3, new Point(2.5, 1), tick());
    assertThat(mp3.distance().doubleValue(SI.METER))
      .isWithin(PlaneRoadModel.DELTA).of(0d);

    assertThat(Point.distance(model.getPosition(ru3), model.getPosition(ru2)))
      .isWithin(PlaneRoadModel.DELTA).of(1d);

  }

  @Test
  public void testCorrectMovement() {
    final Simulator sim = Simulator.builder()
      .addModel(RoadModelBuilders.plane()
        .withCollisionAvoidance()
        .withObjectRadius(1000)
        .withMinPoint(new Point(0, 0))
        .withMaxPoint(new Point(6000, 6000))
        .withDistanceUnit(SI.METER)
        .withSpeedUnit(SI.METERS_PER_SECOND)
        .withMaxSpeed(1000d))
      .addModel(TimeModel.builder().withTickLength(500))
      .build();

    final UavAgent firstUav =
      new UavAgent(new Point(0, 0), new Point(3000, 3000));
    final UavAgent secondUav =
      new UavAgent(new Point(5000, 5000), new Point(3000, 3000));

    sim.register(firstUav);
    sim.register(secondUav);

    final CollisionPlaneRoadModel model = sim.getModelProvider()
      .getModel(CollisionPlaneRoadModel.class);

    final double firstUavMaxDistancePerTick =
      firstUav.getSpeed() * sim.getTimeStep() / 1.0e3;
    final double secondUavMaxDistancePerTick =
      secondUav.getSpeed() * sim.getTimeStep() / 1.0e3;

    Point secondUavPositionBeforeTick = model.getPosition(secondUav);
    Point firstUavPositionBeforeTick = model.getPosition(firstUav);

    final double tolerance = 1.0e-6;

    for (int i = 0; i < 340; i++) {
      sim.tick();
      final Point firstUavPositionAfterTick = model.getPosition(firstUav);
      final Point secondUavPositionAfterTick = model.getPosition(secondUav);

      assertThat(
        Point.distance(firstUavPositionBeforeTick, firstUavPositionAfterTick))
          .isLessThan(
            firstUavMaxDistancePerTick + tolerance);

      assertThat(
        Point.distance(secondUavPositionBeforeTick, secondUavPositionAfterTick))
          .isLessThan(
            secondUavMaxDistancePerTick + tolerance);

      firstUavPositionBeforeTick = firstUavPositionAfterTick;
      secondUavPositionBeforeTick = secondUavPositionAfterTick;
    }
  }

  static class UavAgent implements MovingRoadUser, TickListener {

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

}
