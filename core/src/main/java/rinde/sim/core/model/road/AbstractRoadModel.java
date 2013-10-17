/**
 * 
 */
package rinde.sim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.event.EventAPI;
import rinde.sim.event.EventDispatcher;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * A common space neutral implementation of {@link RoadModel}. It implements a
 * data structure for managing objects and locations and checks many
 * preconditions as defined in {@link RoadModel}.
 * @param <T> The type of the location representation that is used for storing
 *          object locations. This location representation should only be used
 *          internally in the model.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class AbstractRoadModel<T> extends GenericRoadModel {

  /**
   * The unit that is used to represent time internally.
   */
  protected static final Unit<Duration> INTERNAL_TIME_UNIT = SI.SECOND;

  /**
   * The unit that is used to represent distance internally.
   */
  protected static final Unit<Length> INTERNAL_DIST_UNIT = SI.METER;

  /**
   * The unit that is used to represent speed internally.
   */
  protected static final Unit<Velocity> INTERNAL_SPEED_UNIT = SI.METERS_PER_SECOND;

  // TODO event dispatching has to be tested
  /**
   * The {@link EventDispatcher} that dispatches all event for this model.
   */
  protected final EventDispatcher eventDispatcher;

  /**
   * The unit that is used to represent distance externally.
   */
  protected final Unit<Length> externalDistanceUnit;

  /**
   * The unit that is used to represent speed externally.
   */
  protected final Unit<Velocity> externalSpeedUnit;

  /**
   * Converter that converts distances in {@link #INTERNAL_DIST_UNIT} to
   * distances in {@link #externalDistanceUnit}.
   */
  protected final UnitConverter toExternalDistConv;

  /**
   * Converter that converts distances in {@link #externalDistanceUnit} to
   * {@link #INTERNAL_DIST_UNIT}.
   */
  protected final UnitConverter toInternalDistConv;

  /**
   * Converter that converts speed in {@link #INTERNAL_SPEED_UNIT} to speed in
   * {@link #externalSpeedUnit}.
   */
  protected final UnitConverter toExternalSpeedConv;

  /**
   * Converter that converts speed in {@link #externalSpeedUnit} to speed in
   * {@link #INTERNAL_SPEED_UNIT}.
   */
  protected final UnitConverter toInternalSpeedConv;

  /**
   * The types of events this model can dispatch.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public enum RoadEventType {
    /**
     * Indicates that a {@link MovingRoadUser} has moved.
     */
    MOVE
  }

  /**
   * A mapping of {@link RoadUser} to location.
   */
  protected volatile Map<RoadUser, T> objLocs;

  /**
   * A mapping of {@link MovingRoadUser}s to {@link DestinationPath}s.
   */
  protected Map<MovingRoadUser, DestinationPath> objDestinations;

  /**
   * Create model using {@link SI#KILOMETER} and
   * {@link NonSI#KILOMETERS_PER_HOUR}.
   */
  protected AbstractRoadModel() {
    this(SI.KILOMETER, NonSI.KILOMETERS_PER_HOUR);
  }

  /**
   * Create a new instance.
   * @param distanceUnit The distance unit used to interpret all supplied
   *          distances.
   * @param speedUnit The speed unit used to interpret all supplied speeds.
   */
  protected AbstractRoadModel(Unit<Length> distanceUnit,
      Unit<Velocity> speedUnit) {
    externalDistanceUnit = distanceUnit;
    externalSpeedUnit = speedUnit;
    toExternalDistConv = INTERNAL_DIST_UNIT
        .getConverterTo(externalDistanceUnit);
    toInternalDistConv = externalDistanceUnit
        .getConverterTo(INTERNAL_DIST_UNIT);
    toExternalSpeedConv = INTERNAL_SPEED_UNIT.getConverterTo(externalSpeedUnit);
    toInternalSpeedConv = externalSpeedUnit.getConverterTo(INTERNAL_SPEED_UNIT);

    objLocs = Collections.synchronizedMap(new LinkedHashMap<RoadUser, T>());
    objDestinations = newLinkedHashMap();
    eventDispatcher = new EventDispatcher(RoadEventType.MOVE);
  }

  /**
   * A function for converting the location representation to a {@link Point}.
   * @param locObj The location to be converted.
   * @return A {@link Point} indicating the position as represented by the
   *         specified location.
   */
  protected abstract Point locObj2point(T locObj);

  /**
   * A function for converting a {@link Point} to the location representation of
   * this model.
   * @param point The {@link Point} to be converted.
   * @return The location.
   */
  protected abstract T point2LocObj(Point point);

  @Override
  public MoveProgress followPath(MovingRoadUser object, Queue<Point> path,
      TimeLapse time) {
    checkArgument(objLocs.containsKey(object), "object must have a location");
    checkArgument(path.peek() != null, "path can not be empty");
    checkArgument(time.hasTimeLeft(),
        "can not follow path when to time is left");
    final Point dest = newArrayList(path).get(path.size() - 1);
    objDestinations.put(object, new DestinationPath(dest, path));
    final MoveProgress mp = doFollowPath(object, path, time);
    eventDispatcher.dispatchEvent(new MoveEvent(self, object, mp));
    return mp;
  }

  @Override
  public MoveProgress moveTo(MovingRoadUser object, Point destination,
      TimeLapse time) {
    Queue<Point> path;
    if (objDestinations.containsKey(object)
        && objDestinations.get(object).destination.equals(destination)) {
      // is valid move? -> assume it is
      path = objDestinations.get(object).path;
    } else {
      path = new LinkedList<Point>(getShortestPathTo(object, destination));
      objDestinations.put(object, new DestinationPath(destination, path));
    }
    final MoveProgress mp = doFollowPath(object, path, time);
    eventDispatcher.dispatchEvent(new MoveEvent(self, object, mp));
    return mp;
  }

  @Override
  public MoveProgress moveTo(MovingRoadUser object, RoadUser destination,
      TimeLapse time) {
    return moveTo(object, getPosition(destination), time);
  }

  /**
   * Should be overridden by subclasses to define actual
   * {@link RoadModel#followPath(MovingRoadUser, Queue, TimeLapse)} behavior.
   * @param object The object that is moved.
   * @param path The path that is followed.
   * @param time The time that is available for travel.
   * @return A {@link MoveProgress} instance containing the actual travel
   *         details.
   */
  protected abstract MoveProgress doFollowPath(MovingRoadUser object,
      Queue<Point> path, TimeLapse time);

  @Override
  @Nullable
  public Point getDestination(MovingRoadUser object) {
    if (objDestinations.containsKey(object)) {
      return objDestinations.get(object).destination;
    }
    return null;
  }

  @Override
  public void addObjectAt(RoadUser newObj, Point pos) {
    checkArgument(!objLocs.containsKey(newObj), "Object is already added: %s.",
        newObj);
    objLocs.put(newObj, point2LocObj(pos));
  }

  @Override
  public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
    checkArgument(!objLocs.containsKey(newObj), "Object %s is already added.",
        newObj);
    checkArgument(objLocs.containsKey(existingObj),
        "Object %s does not exist.", existingObj);
    objLocs.put(newObj, objLocs.get(existingObj));
  }

  @Override
  public void removeObject(RoadUser roadUser) {
    checkArgument(roadUser != null, "RoadUser can not be null");
    checkArgument(objLocs.containsKey(roadUser),
        "RoadUser: %s does not exist.", roadUser);
    objLocs.remove(roadUser);
    objDestinations.remove(roadUser);
  }

  @Override
  public void clear() {
    objLocs.clear();
    objDestinations.clear();
  }

  @Override
  public boolean containsObject(RoadUser obj) {
    checkArgument(obj != null, "obj can not be null");
    return objLocs.containsKey(obj);
  }

  @Override
  public boolean containsObjectAt(RoadUser obj, Point p) {
    checkArgument(p != null, "point can not be null");
    if (containsObject(obj)) {
      return objLocs.get(obj).equals(p);
    }
    return false;
  }

  @Override
  public boolean equalPosition(RoadUser obj1, RoadUser obj2) {
    return containsObject(obj1) && containsObject(obj2)
        && getPosition(obj1).equals(getPosition(obj2));
  }

  @Override
  public Map<RoadUser, Point> getObjectsAndPositions() {
    Map<RoadUser, T> copiedMap;
    synchronized (objLocs) {
      copiedMap = new LinkedHashMap<RoadUser, T>();
      copiedMap.putAll(objLocs);
    } // it is save to release the lock now

    final Map<RoadUser, Point> theMap = new LinkedHashMap<RoadUser, Point>();
    for (final java.util.Map.Entry<RoadUser, T> entry : copiedMap.entrySet()) {
      theMap.put(entry.getKey(), locObj2point(entry.getValue()));
    }
    return theMap;
  }

  @Override
  public Point getPosition(RoadUser roadUser) {
    checkArgument(containsObject(roadUser), "RoadUser does not exist: %s ",
        roadUser);
    return locObj2point(objLocs.get(roadUser));
  }

  @Override
  public Collection<Point> getObjectPositions() {
    return getObjectsAndPositions().values();
  }

  @Override
  public Set<RoadUser> getObjects() {
    synchronized (objLocs) {
      final Set<RoadUser> copy = new LinkedHashSet<RoadUser>();
      copy.addAll(objLocs.keySet());
      return copy;
    }
  }

  @Override
  public Set<RoadUser> getObjects(Predicate<RoadUser> predicate) {
    return Sets.filter(getObjects(), predicate);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <Y extends RoadUser> Set<Y> getObjectsAt(RoadUser roadUser,
      Class<Y> type) {
    final Set<Y> result = new HashSet<Y>();
    for (final RoadUser ru : getObjects(new SameLocationPredicate(roadUser,
        type, self))) {
      result.add((Y) ru);
    }
    return result;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <Y extends RoadUser> Set<Y> getObjectsOfType(final Class<Y> type) {
    return (Set<Y>) getObjects(new Predicate<RoadUser>() {
      @Override
      public boolean apply(RoadUser input) {
        return type.isInstance(input);
      }
    });
  }

  @Override
  public List<Point> getShortestPathTo(RoadUser fromObj, RoadUser toObj) {
    checkArgument(fromObj != null, "fromObj can not be null");
    checkArgument(objLocs.containsKey(toObj),
        " to object should be in RoadModel. %s", toObj);
    return getShortestPathTo(fromObj, getPosition(toObj));
  }

  @Override
  public List<Point> getShortestPathTo(RoadUser fromObj, Point to) {
    checkArgument(objLocs.containsKey(fromObj),
        " from object should be in RoadModel. %s", fromObj);
    return getShortestPathTo(getPosition(fromObj), to);
  }

  @Override
  public boolean doRegister(RoadUser roadUser) {
    LOGGER.info("register {}", roadUser);
    roadUser.initRoadUser(self);
    return true;
  }

  @Override
  public boolean unregister(RoadUser roadUser) {
    final boolean contains = containsObject(roadUser);
    LOGGER.info("unregister {} succes: {}", roadUser, contains);
    if (contains) {
      removeObject(roadUser);
      return true;
    }
    return false;
  }

  @Override
  public final EventAPI getEventAPI() {
    return eventDispatcher.getPublicEventAPI();
  }

  @Override
  public Unit<Length> getDistanceUnit() {
    return externalDistanceUnit;
  }

  @Override
  public Unit<Velocity> getSpeedUnit() {
    return externalSpeedUnit;
  }

  private static class SameLocationPredicate implements Predicate<RoadUser> {
    private final RoadUser reference;
    private final RoadModel model;
    private final Class<?> type;

    SameLocationPredicate(final RoadUser pReference, final Class<?> pType,
        final RoadModel pModel) {
      reference = pReference;
      type = pType;
      model = pModel;
    }

    @Override
    public boolean apply(RoadUser input) {
      return type.isInstance(input) && model.equalPosition(input, reference);
    }
  }

  /**
   * Simple class for storing destinations and paths leading to them.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  protected class DestinationPath {
    /**
     * The destination of the path.
     */
    public final Point destination;
    /**
     * The path leading to the destination.
     */
    public final Queue<Point> path;

    DestinationPath(Point dest, Queue<Point> p) {
      destination = dest;
      path = p;
    }
  }
}
