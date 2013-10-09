/**
 * 
 */
package rinde.sim.core.model.road;

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

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.event.EventAPI;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class ForwardingRoadModel implements RoadModel {

  /**
   * Needs to be overriden by subclasses.
   */
  protected ForwardingRoadModel() {}

  /**
   * @return The {@link AbstractRoadModel} (or subclass) to which all calls are
   *         delegated.
   */
  protected abstract AbstractRoadModel<?> delegate();

  @Override
  public boolean register(RoadUser element) {
    return delegate().register(this, element);
  }

  @Override
  public boolean unregister(RoadUser element) {
    return delegate().unregister(element);
  }

  @Override
  public Class<RoadUser> getSupportedType() {
    return delegate().getSupportedType();
  }

  @Override
  public MoveProgress moveTo(MovingRoadUser object, Point destination,
      TimeLapse time) {
    return delegate().moveTo(this, object, destination, time);
  }

  @Override
  public MoveProgress moveTo(MovingRoadUser object, RoadUser destination,
      TimeLapse time) {
    return delegate().moveTo(object, destination, time);
  }

  @Override
  public MoveProgress followPath(MovingRoadUser object, Queue<Point> path,
      TimeLapse time) {
    return delegate().followPath(this, object, path, time);
  }

  @Override
  public void addObjectAt(RoadUser newObj, Point pos) {
    delegate().addObjectAt(newObj, pos);
  }

  @Override
  public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
    delegate().addObjectAtSamePosition(newObj, existingObj);
  }

  @Override
  public void removeObject(RoadUser roadUser) {
    delegate().removeObject(roadUser);
  }

  @Override
  public void clear() {
    delegate().clear();
  }

  @Override
  public boolean containsObject(RoadUser obj) {
    return delegate().containsObject(obj);
  }

  @Override
  public boolean containsObjectAt(RoadUser obj, Point p) {
    return delegate().containsObjectAt(obj, p);
  }

  @Override
  public boolean equalPosition(RoadUser obj1, RoadUser obj2) {
    return delegate().equalPosition(obj1, obj2);
  }

  @Override
  public Map<RoadUser, Point> getObjectsAndPositions() {
    return delegate().getObjectsAndPositions();
  }

  @Override
  public Point getPosition(RoadUser roadUser) {
    return delegate().getPosition(roadUser);
  }

  @Nullable
  @Override
  public Point getDestination(MovingRoadUser roadUser) {
    return delegate().getDestination(roadUser);
  }

  @Override
  public Point getRandomPosition(RandomGenerator rnd) {
    return delegate().getRandomPosition(rnd);
  }

  @Override
  public Collection<Point> getObjectPositions() {
    return delegate().getObjectPositions();
  }

  @Override
  public Set<RoadUser> getObjects() {
    return delegate().getObjects();
  }

  @Override
  public Set<RoadUser> getObjects(Predicate<RoadUser> predicate) {
    return delegate().getObjects(predicate);
  }

  @Override
  public <Y extends RoadUser> Set<Y> getObjectsAt(RoadUser roadUser,
      Class<Y> type) {
    return delegate().getObjectsAt(roadUser, type);
  }

  @Override
  public <Y extends RoadUser> Set<Y> getObjectsOfType(Class<Y> type) {
    return delegate().getObjectsOfType(type);
  }

  @Override
  public List<Point> getShortestPathTo(RoadUser fromObj, RoadUser toObj) {
    return delegate().getShortestPathTo(fromObj, toObj);
  }

  @Override
  public List<Point> getShortestPathTo(RoadUser fromObj, Point to) {
    return delegate().getShortestPathTo(fromObj, to);
  }

  @Override
  public List<Point> getShortestPathTo(Point from, Point to) {
    return delegate().getShortestPathTo(from, to);
  }

  @Override
  public EventAPI getEventAPI() {
    return delegate().getEventAPI();
  }

  @Override
  public ImmutableList<Point> getBounds() {
    return delegate().getBounds();
  }

  @Override
  public Unit<Length> getDistanceUnit() {
    return delegate().getDistanceUnit();
  }

  @Override
  public Unit<Velocity> getSpeedUnit() {
    return delegate().getSpeedUnit();
  }
}
