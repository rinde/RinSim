/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.examples.demo.factory;

import static com.google.common.base.Verify.verify;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.AbstractModel;
import com.github.rinde.rinsim.core.ModelProvider;
import com.github.rinde.rinsim.core.ModelReceiver;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.SimulatorUser;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.pdp.PDPModelEvent;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

class AgvModel extends AbstractModel<AGV> implements TickListener,
  ModelReceiver, SimulatorUser, Listener {

  Optional<RoadModel> rm;
  Optional<SimulatorAPI> simulator;
  final RandomGenerator rng;
  Set<Point> occupiedPositions;
  ImmutableList<ImmutableList<Point>> points;
  int currentBox;

  List<BoxHandle> boxes;
  final List<Point> border;

  AgvModel(RandomGenerator r, ImmutableList<ImmutableList<Point>> ps,
    ImmutableList<Point> b) {
    rm = Optional.absent();
    rng = r;
    occupiedPositions = newLinkedHashSet();
    points = ps;
    currentBox = 0;
    boxes = newArrayList();
    border = b;
  }

  @Override
  public void tick(TimeLapse timeLapse) {}

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  public void registerModelProvider(ModelProvider mp) {
    rm = Optional.fromNullable(mp.tryGetModel(RoadModel.class));
    Optional
      .fromNullable(mp.tryGetModel(PDPModel.class))
      .get()
      .getEventAPI()
      .addListener(this, PDPModelEventType.END_DELIVERY,
        PDPModelEventType.END_PICKUP);
  }

  @Override
  public void setSimulator(SimulatorAPI api) {
    simulator = Optional.of(api);
  }

  void init() {
    if (simulator.isPresent() && rm.isPresent()) {
      int max = 0;
      for (final List<Point> ps : points) {
        max = Math.max(max, ps.size());
      }
      final int num = max;
      for (int i = 0; i < num; i++) {
        final long duration = DoubleMath.roundToLong(
          FactoryExample.SERVICE_DURATION / 2d
            + rng.nextDouble() * FactoryExample.SERVICE_DURATION,
          RoundingMode.CEILING);

        final Point rnd = rndBorder();
        Point dest;
        if (i >= points.get(0).size()) {
          dest = rndBorder();
        } else {
          dest = points.get(0).get(i);
          occupiedPositions.add(dest);
        }

        final BoxHandle bh = new BoxHandle(i);
        final Box b = new Box(rnd, dest, duration, bh);
        bh.box = b;

        boxes.add(bh);
        simulator.get().register(boxes.get(boxes.size() - 1).box);
      }
    }
  }

  @Override
  public boolean register(AGV element) {
    element.registerAgvModel(this);
    return true;
  }

  @Override
  public boolean unregister(AGV element) {
    return false;
  }

  @Override
  public void handleEvent(Event e) {
    // FIXME
    // if (e.getEventType() == SimulatorEventType.CONFIGURED) {
    // init();
    // } else {
    verify(e instanceof PDPModelEvent);
    final PDPModelEvent event = (PDPModelEvent) e;
    if (e.getEventType() == PDPModelEventType.END_PICKUP) {
      occupiedPositions.remove(((Box) event.parcel).origin);
    }
    if (e.getEventType() == PDPModelEventType.END_DELIVERY) {
      final long duration = DoubleMath.roundToLong(
        FactoryExample.SERVICE_DURATION / 2d
          + rng.nextDouble() * FactoryExample.SERVICE_DURATION,
        RoundingMode.CEILING);
      simulator.get().unregister(event.parcel);

      final BoxHandle bh = ((Box) event.parcel).boxHandle;
      bh.wordIndex = (bh.wordIndex + 1) % points.size();

      Point dest;
      if (bh.index >= points.get(bh.wordIndex).size()) {
        dest = rndBorder();
      } else {
        dest = points.get(bh.wordIndex).get(bh.index);
        occupiedPositions.add(dest);
      }

      final Box b = new Box(event.parcel.getDestination(), dest, duration, bh);
      bh.box = b;

      simulator.get().register(b);
    }
    // }
  }

  Point rndBorder() {
    return border.get(rng.nextInt(border.size()));
  }

  Point rnd() {
    Point p;
    do {
      p = rm.get().getRandomPosition(rng);
    } while (occupiedPositions.contains(p));
    occupiedPositions.add(p);
    return p;
  }

  Box nextDestination() {
    final Box b = boxes.get(currentBox % boxes.size()).box;
    currentBox++;
    return b;
  }

  static class BoxHandle {
    int wordIndex;
    final int index;
    Box box;

    BoxHandle(int i) {
      index = i;
      wordIndex = 0;
    }
  }
}
