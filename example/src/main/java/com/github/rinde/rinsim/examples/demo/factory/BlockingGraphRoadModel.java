package com.github.rinde.rinsim.examples.demo.factory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Rinde van Lon 
 * 
 */
public class BlockingGraphRoadModel extends GraphRoadModel {

  Set<Point> blockedNodes;
  Multimap<MovingRoadUser, Point> vehicleBlocks;

  BlockingGraphRoadModel(Graph<? extends ConnectionData> pGraph,
      Unit<Length> distanceUnit, Unit<Velocity> speedUnit) {
    super(pGraph, distanceUnit, speedUnit);
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
      final Connection<?> conn = newLoc.conn;
      checkNotNull(conn);
      blockedNodes.add(conn.to);
      vehicleBlocks.put(object, conn.to);
    } else {
      blockedNodes.add(getPosition(object));
      vehicleBlocks.put(object, getPosition(object));
    }
    return mp;
  }
}
