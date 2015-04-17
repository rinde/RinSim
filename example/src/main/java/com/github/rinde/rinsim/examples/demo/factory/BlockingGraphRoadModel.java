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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Suppliers;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Rinde van Lon
 *
 */
public class BlockingGraphRoadModel extends GraphRoadModel {

  Set<Point> blockedNodes;
  Multimap<MovingRoadUser, Point> vehicleBlocks;

  BlockingGraphRoadModel(Graph<? extends ConnectionData> pGraph, Builder b) {
    super(pGraph, b);
    blockedNodes = newLinkedHashSet();
    vehicleBlocks = LinkedHashMultimap.create();
  }

  @Override
  protected MoveProgress doFollowPath(MovingRoadUser object, Queue<Point> path,
    TimeLapse time) {
    blockedNodes.removeAll(vehicleBlocks.get(object));
    vehicleBlocks.removeAll(object);

    final List<Point> inputP = newArrayList(path);
    int index = -1;
    for (int i = 0; i < inputP.size(); i++) {
      if (blockedNodes.contains(inputP.get(i))) {
        index = i;
        break;
      }
    }
    final MoveProgress mp;
    if (index >= 0) {
      final Queue<Point> newPath = index == -1 ? new LinkedList<Point>()
        : newLinkedList(inputP.subList(0, index));
      final int originalSize = newPath.size();
      mp = super.doFollowPath(object, newPath, time);
      for (int i = 0; i < originalSize - newPath.size(); i++) {
        path.remove();
      }
      time.consumeAll();
    } else {
      mp = super.doFollowPath(object, path, time);
    }
    final Loc newLoc = objLocs.get(object);
    if (newLoc.isOnConnection()) {
      final Connection<?> conn = newLoc.conn.get();
      blockedNodes.add(conn.to());
      vehicleBlocks.put(object, conn.to());
    } else {
      blockedNodes.add(getPosition(object));
      vehicleBlocks.put(object, getPosition(object));
    }
    return mp;
  }

  static Builder blockingBuilder(Graph<?> g) {
    return new Builder(g);
  }

  static class Builder extends
    RoadModelBuilders.AbstractGraphRMB<BlockingGraphRoadModel, Builder> {

    Builder(Graph<?> g) {
      super(Suppliers.ofInstance(g));
      setProvidingTypes(RoadModel.class, GraphRoadModel.class,
        BlockingGraphRoadModel.class);
    }

    @Override
    public BlockingGraphRoadModel build(DependencyProvider dependencyProvider) {
      return new BlockingGraphRoadModel(getGraph(), this);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }
}
