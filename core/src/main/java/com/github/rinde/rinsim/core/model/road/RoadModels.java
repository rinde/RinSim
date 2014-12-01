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

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * Provides several queries for finding {@link RoadUser}s in {@link RoadModel}s.
 * 
 * @author Rinde van Lon
 */
public final class RoadModels {

  private RoadModels() {}

  /**
   * Convenience method for
   * {@link Graphs#findClosestObject(Point, Collection, Function)}.
   * @param pos The {@link Point} which is used as reference.
   * @param rm The {@link RoadModel} which is searched.
   * @param objects The {@link Collection} which is searched, each object must
   *          exist in <code>rm</code>.
   * @param <T> The type of the returned object.
   * @return The closest object in <code>rm</code> to <code>pos</code> which
   *         satisfies the <code>predicate</code>.
   * @see Graphs#findClosestObject(Point, Collection, Function)
   */
  @Nullable
  public static <T extends RoadUser> T findClosestObject(Point pos,
      RoadModel rm, Collection<T> objects) {
    return Graphs.findClosestObject(pos, objects,
        new RoadModels.RoadUserToPositionFunction<T>(rm));
  }

  /**
   * Convenience method for {@link Graphs#findClosestObject}.
   * @param pos The {@link Point} which is used as reference.
   * @param rm The {@link RoadModel} which is searched.
   * @param predicate A {@link Predicate} indicating which objects are included
   *          in the search.
   * @return The closest object in <code>rm</code> to <code>pos</code> which
   *         satisfies the <code>predicate</code>.
   * @see Graphs#findClosestObject
   */
  @Nullable
  public static RoadUser findClosestObject(Point pos, RoadModel rm,
      Predicate<RoadUser> predicate) {
    final Collection<RoadUser> filtered = Collections2.filter(rm.getObjects(),
        predicate);
    return findClosestObject(pos, rm, filtered);
  }

  /**
   * Convenience method for {@link Graphs#findClosestObject}.
   * @param pos The {@link Point} which is used as reference.
   * @param rm The {@link RoadModel} which is searched.
   * @param type The type of object that is searched.
   * @param <T> The type of the returned object.
   * @return The closest object in <code>rm</code> to <code>pos</code> of type
   *         <code>type</code>.
   * @see Graphs#findClosestObject
   */
  @Nullable
  public static <T extends RoadUser> T findClosestObject(Point pos,
      RoadModel rm, final Class<T> type) {
    return findClosestObject(pos, rm, rm.getObjectsOfType(type));
  }

  /**
   * Convenience method for {@link Graphs#findClosestObject}.
   * @param pos The {@link Point} which is used as reference.
   * @param rm The {@link RoadModel} which is searched.
   * @return The closest object in <code>rm</code> to <code>pos</code>.
   * @see Graphs#findClosestObject
   */
  @Nullable
  public static RoadUser findClosestObject(Point pos, RoadModel rm) {
    return findClosestObject(pos, rm, RoadUser.class);
  }

  /**
   * Returns a list of objects from {@link RoadModel} <code>rm</code> ordered by
   * its distance to position <code>pos</code>.
   * @param pos The {@link Point} which is used as a reference point.
   * @param rm The {@link RoadModel} instance in which the closest objects are
   *          searched.
   * @return A list of objects that are closest to <code>pos</code>. The list is
   *         ordered such that the closest object appears first. An empty list
   *         is returned when <code>objects</code> is empty.
   */
  public static List<RoadUser> findClosestObjects(Point pos, RoadModel rm) {
    return RoadModels.findClosestObjects(pos, rm, RoadUser.class,
        Integer.MAX_VALUE);
  }

  /**
   * Searches the closest <code>n</code> objects to position <code>pos</code> in
   * {@link RoadModel} <code>rm</code>.
   * @param pos The {@link Point} which is used as a reference point.
   * @param rm The {@link RoadModel} instance in which the closest objects are
   *          searched.
   * @param n The maximum number of objects to return where n must be &ge; 0.
   * @return A list of objects that are closest to <code>pos</code>. The list is
   *         ordered such that the closest object appears first. An empty list
   *         is returned when <code>objects</code> is empty.
   */
  public static List<RoadUser> findClosestObjects(Point pos, RoadModel rm, int n) {
    return RoadModels.findClosestObjects(pos, rm, RoadUser.class, n);
  }

  /**
   * Searches the closest <code>n</code> objects to position <code>pos</code> in
   * {@link RoadModel} <code>rm</code>. Only the objects that satisfy
   * <code>predicate</code> are included in the search.
   * @param pos The {@link Point} which is used as a reference point.
   * @param rm The {@link RoadModel} instance in which the closest objects are
   *          searched.
   * @param predicate Only objects that satisfy this predicate will be returned.
   * @param n The maximum number of objects to return where n must be &ge; 0.
   * @return A list of objects that are closest to <code>pos</code>. The list is
   *         ordered such that the closest object appears first. An empty list
   *         is returned when <code>objects</code> is empty.
   */
  public static List<RoadUser> findClosestObjects(Point pos, RoadModel rm,
      Predicate<RoadUser> predicate, int n) {
    final Collection<RoadUser> filtered = Collections2.filter(rm.getObjects(),
        predicate);
    return RoadModels.findClosestObjects(pos, rm, filtered, n);
  }

  /**
   * Searches the closest <code>n</code> objects to position <code>pos</code> in
   * {@link RoadModel} <code>rm</code>.
   * @param pos The {@link Point} which is used as a reference point.
   * @param rm The {@link RoadModel} instance in which the closest objects are
   *          searched.
   * @param type The type of objects which are included in the search.
   * @param n The maximum number of objects to return where n must be &ge; 0.
   * @param <T> The type of the objects in the returned collection.
   * @return A list of objects that are closest to <code>pos</code>. The list is
   *         ordered such that the closest object appears first. An empty list
   *         is returned when <code>objects</code> is empty.
   */
  public static <T extends RoadUser> List<T> findClosestObjects(Point pos,
      RoadModel rm, Class<T> type, int n) {
    return RoadModels.findClosestObjects(pos, rm, rm.getObjectsOfType(type), n);
  }

  /**
   * Searches the closest <code>n</code> objects to position <code>pos</code> in
   * collection <code>objects</code>.
   * @param pos The {@link Point} which is used as a reference point.
   * @param rm The {@link RoadModel} instance which is used to lookup the
   *          positions of the objects in <code>objects</code>.
   * @param objects The list of objects which is searched.
   * @param n The maximum number of objects to return where n must be &ge; 0.
   * @param <T> The type of the objects in the returned collection.
   * @return A list of objects that are closest to <code>pos</code>. The list is
   *         ordered such that the closest object appears first. An empty list
   *         is returned when <code>objects</code> is empty.
   */
  public static <T extends RoadUser> List<T> findClosestObjects(Point pos,
      RoadModel rm, Collection<T> objects, int n) {
    return Graphs.findClosestObjects(pos, objects,
        new RoadModels.RoadUserToPositionFunction<T>(rm), n);
  }

  /**
   * Returns all {@link RoadUser}s in <code>model</code> that are
   * <strong>within</strong> a bird-flight distance of <code>radius</code> to
   * <code>position</code>.
   * @param position The position which is used to measure distance.
   * @param model The {@link RoadModel} which contains the objects.
   * @param radius Objects with a distance smaller than <code>radius</code> to
   *          <code>position</code> are included.
   * @return A collection of {@link RoadUser}s.
   */
  public static Collection<RoadUser> findObjectsWithinRadius(
      final Point position, final RoadModel model, final double radius) {
    return RoadModels.findObjectsWithinRadius(position, model, radius,
        model.getObjects());
  }

  /**
   * Returns all {@link RoadUser}s of type <code> type</code> in
   * <code>model</code> that are <strong>within</strong> a bird-flight distance
   * of <code>radius</code> to <code>position</code>.
   * @param position The position which is used to measure distance.
   * @param model The {@link RoadModel} which contains the objects.
   * @param radius Objects with a distance smaller than <code>radius</code> to
   *          <code>position</code> are included.
   * @param type The {@link Class} of the required type.
   * @param <T> The type of the objects in the returned collection.
   * @return A collection of type <code>type</code>.
   */
  public static <T extends RoadUser> Collection<T> findObjectsWithinRadius(
      final Point position, final RoadModel model, final double radius,
      final Class<T> type) {
    return RoadModels.findObjectsWithinRadius(position, model, radius,
        model.getObjectsOfType(type));
  }

  /**
   * Finds all {@link RoadUser}s in the specified <code>objects</code>
   * collection that are <strong>within</strong> a bird-flight distance of
   * <code>radius</code> to <code>position</code>.
   * @param position The position which is used to measure distance.
   * @param model The {@link RoadModel} which contains the objects.
   * @param radius Objects with a distance smaller than <code>radius</code> to
   *          <code>position</code> are included.
   * @param objects The collection of objects which is searched through, note:
   *          all objects <b>must</b> exist in the {@link RoadModel}.
   * @param <T> The type of the objects in the returned collection.
   * @return A collection of {@link RoadUser}s.
   */
  public static <T extends RoadUser> Collection<T> findObjectsWithinRadius(
      final Point position, final RoadModel model, final double radius,
      Collection<T> objects) {
    return Collections2.filter(objects, new RoadModels.DistancePredicate(
        position, model, radius));
  }

  /**
   * Computes the duration which is required to travel the specified distance
   * with the given velocity. Note: although time is normally a long, we use
   * double here instead. Converting it to long in this method would introduce
   * rounding in a too early stage.
   * @param speed The travel speed.
   * @param distance The distance to travel.
   * @param outputTimeUnit The time unit to use for the output.
   * @return The time it takes to travel the specified distance with the
   *         specified speed.
   */
  public static double computeTravelTime(Measure<Double, Velocity> speed,
      Measure<Double, Length> distance, Unit<Duration> outputTimeUnit) {
    // meters
    return Measure.valueOf(distance.doubleValue(SI.METER)
        // divided by m/s
        / speed.doubleValue(SI.METERS_PER_SECOND),
        // gives seconds
        SI.SECOND)
        // convert to desired unit
        .doubleValue(outputTimeUnit);
  }

  static class RoadUserToPositionFunction<T extends RoadUser> implements
      Function<T, Point> {
    private final RoadModel rm;

    RoadUserToPositionFunction(RoadModel roadModel) {
      rm = roadModel;
    }

    @Override
    public Point apply(T input) {
      return rm.getPosition(input);
    }
  }

  static class DistancePredicate implements Predicate<RoadUser> {
    private final Point position;
    private final RoadModel model;
    private final double radius;

    DistancePredicate(final Point p, final RoadModel m, final double r) {
      position = p;
      model = m;
      radius = r;
    }

    @Override
    public boolean apply(RoadUser input) {
      return Point.distance(model.getPosition(input), position) < radius;
    }
  }
}
