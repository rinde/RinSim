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
package com.github.rinde.rinsim.core.model.road;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * A {@link RoadModel} which forwards all its method calls to another
 * {@link RoadModel}. Subclasses should override one or more methods to modify
 * the behavior of the backing model as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 * @author Rinde van Lon
 */
public class ForwardingRoadModel extends GenericRoadModel {
  /**
   * The {@link AbstractRoadModel} (or subclass) to which all calls are
   * delegated.
   */
  protected final GenericRoadModel delegate;

  /**
   * Initializes a new instance that delegates all calls to the specified
   * {@link GenericRoadModel}.
   * @param deleg The instance to which all calls are delegated.
   */
  protected ForwardingRoadModel(GenericRoadModel deleg) {
    delegate = deleg;
    delegate.setSelf(this);
  }

  @Override
  protected final void setSelf(GenericRoadModel rm) {
    super.setSelf(rm);
    delegate.setSelf(rm);
  }

  @Override
  public boolean doRegister(RoadUser element) {
    return delegate.register(element);
  }

  @Override
  public boolean unregister(RoadUser element) {
    return delegate.unregister(element);
  }

  @Override
  public MoveProgress moveTo(MovingRoadUser object, Point destination,
    TimeLapse time) {
    return delegate.moveTo(object, destination, time);
  }

  @Override
  public MoveProgress moveTo(MovingRoadUser object, RoadUser destination,
    TimeLapse time) {
    return delegate.moveTo(object, destination, time);
  }

  @Override
  public MoveProgress followPath(MovingRoadUser object, Queue<Point> path,
    TimeLapse time) {
    return delegate.followPath(object, path, time);
  }

  @Override
  public void addObjectAt(RoadUser newObj, Point pos) {
    delegate.addObjectAt(newObj, pos);
  }

  @Override
  public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
    delegate.addObjectAtSamePosition(newObj, existingObj);
  }

  @Override
  public void removeObject(RoadUser roadUser) {
    delegate.removeObject(roadUser);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public boolean containsObject(RoadUser obj) {
    return delegate.containsObject(obj);
  }

  @Override
  public boolean containsObjectAt(RoadUser obj, Point p) {
    return delegate.containsObjectAt(obj, p);
  }

  @Override
  public boolean equalPosition(RoadUser obj1, RoadUser obj2) {
    return delegate.equalPosition(obj1, obj2);
  }

  @Override
  public Map<RoadUser, Point> getObjectsAndPositions() {
    return delegate.getObjectsAndPositions();
  }

  @Override
  public Point getPosition(RoadUser roadUser) {
    return delegate.getPosition(roadUser);
  }

  @Nullable
  @Override
  public Point getDestination(MovingRoadUser roadUser) {
    return delegate.getDestination(roadUser);
  }

  @Override
  public Point getRandomPosition(RandomGenerator rnd) {
    return delegate.getRandomPosition(rnd);
  }

  @Override
  public Collection<Point> getObjectPositions() {
    return delegate.getObjectPositions();
  }

  @Override
  public Set<RoadUser> getObjects() {
    return delegate.getObjects();
  }

  @Override
  public Set<RoadUser> getObjects(Predicate<RoadUser> predicate) {
    return delegate.getObjects(predicate);
  }

  @Override
  public <Y extends RoadUser> Set<Y> getObjectsAt(RoadUser roadUser,
    Class<Y> type) {
    return delegate.getObjectsAt(roadUser, type);
  }

  @Override
  public <Y extends RoadUser> Set<Y> getObjectsOfType(Class<Y> type) {
    return delegate.getObjectsOfType(type);
  }

  @Override
  public List<Point> getShortestPathTo(RoadUser fromObj, RoadUser toObj) {
    return delegate.getShortestPathTo(fromObj, toObj);
  }

  @Override
  public List<Point> getShortestPathTo(RoadUser fromObj, Point to) {
    return delegate.getShortestPathTo(fromObj, to);
  }

  @Override
  public List<Point> getShortestPathTo(Point from, Point to) {
    return delegate.getShortestPathTo(from, to);
  }

  @Override
  public EventAPI getEventAPI() {
    return delegate.getEventAPI();
  }

  @Override
  public ImmutableList<Point> getBounds() {
    return delegate.getBounds();
  }

  @Override
  public Unit<Length> getDistanceUnit() {
    return delegate.getDistanceUnit();
  }

  @Override
  public Unit<Velocity> getSpeedUnit() {
    return delegate.getSpeedUnit();
  }

  @Override
  public <U> U get(Class<U> type) {
    return delegate.get(type);
  }

  /**
   * Abstract base builder for creating subclasses of
   * {@link ForwardingRoadModel}.
   * @param <T> The specific subtype of {@link ForwardingRoadModel} to
   *          construct.
   * @author Rinde van Lon
   */
  public abstract static class Builder<T extends ForwardingRoadModel>
    extends AbstractModelBuilder<T, RoadUser> implements Serializable {

    private static final long serialVersionUID = 1852539610753492228L;

    /**
     * @return The {@link ModelBuilder} that will be decorated by the
     *         {@link ForwardingRoadModel} constructed by this builder.
     */
    public abstract ModelBuilder<RoadModel, RoadUser> getDelegateModelBuilder();
  }
}
