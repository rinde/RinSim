/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.util.ArrayList;
import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel.Loc;
import com.github.rinde.rinsim.geom.Point;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * Represents the distance traveled and time spent in
 * {@link RoadModel#followPath(MovingRoadUser, java.util.Queue, com.github.rinde.rinsim.core.TimeLapse)}
 * .
 * @author Bartosz Michalik
 * @author Rinde van Lon
 * @since 2.0
 */
@AutoValue
public abstract class MoveProgress {

  MoveProgress() {}

  /**
   * @return the distance traveled.
   */
  public abstract Measure<Double, Length> distance();

  /**
   * @return the time spend on traveling the distance.
   */
  public abstract Measure<Long, Duration> time();

  /**
   * @return the nodes that were traveled.
   */
  public abstract ImmutableList<Point> travelledNodes();

  static MoveProgress create(Measure<Double, Length> dist,
      Measure<Long, Duration> pTime, List<Point> pTravelledNodes) {
    checkArgument(dist.getValue() >= 0d,
        "Distance must be greater than or equal to 0.");
    checkArgument(pTime.getValue() >= 0L,
        "Time must be greather than or equal to 0.");
    return new AutoValue_MoveProgress(dist, pTime,
        ImmutableList.copyOf(pTravelledNodes));
  }

  /**
   * Construct a new {@link Builder}.
   * @param ru A reference to {@link RoadUnits}.
   * @param timeLapse The time lapse to use to obtain time related information.
   * @return A new instance.
   */
  public static Builder builder(RoadUnits ru, TimeLapse timeLapse) {
    return new Builder(ru, timeLapse);
  }

  /**
   * A {@link Builder} for constructing {@link MoveProgress} instances. Per
   * builder instance only one {@link MoveProgress} instance can be created.
   * @author Rinde van Lon
   */
  public static class Builder {
    private final RoadUnits unitConversion;
    private final List<Point> traveledNodes;
    private final TimeLapse time;

    private double travelDistance;
    private final long startTimeConsumed;
    private final boolean used = false;

    Builder(RoadUnits ru, TimeLapse timeLapse) {
      unitConversion = ru;
      time = timeLapse;
      startTimeConsumed = time.getTimeConsumed();
      travelDistance = 0;
      traveledNodes = new ArrayList<>();
    }

    /**
     * Attempts to add the point to the list of traveled nodes. It is added only
     * if it is a node in the graph, otherwise nothing happens.
     * @param node The node to add.
     * @return This, as per the builder pattern.
     */
    public Builder addNode(Point node) {
      if (!(node instanceof Loc) || !((Loc) node).isOnConnection()) {
        traveledNodes.add(node);
      }
      return this;
    }

    /**
     * Adds the distance as traveled distance.
     * @param dist The distance to add (must be positive).
     * @return This, as per the builder pattern.
     */
    public Builder addDistance(double dist) {
      checkArgument(dist >= 0d, "Only positive values are allowed, is %s.",
          dist);
      travelDistance += dist;
      return this;
    }

    /**
     * Constructs a new {@link MoveProgress} instance using all information that
     * was added via this builder. This method may be called only once per
     * builder.
     * @return A new {@link MoveProgress} instance.
     */
    public MoveProgress build() {
      verify(!used, "This method may be called only once.");
      final Measure<Double, Length> distTraveled = unitConversion
          .toExDistMeasure(travelDistance);
      final Measure<Long, Duration> timeConsumed = Measure.valueOf(
          time.getTimeConsumed() - startTimeConsumed, time.getTimeUnit());
      return create(distTraveled, timeConsumed, traveledNodes);
    }
  }
}
