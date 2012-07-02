/**
 * 
 */
package rinde.sim.core.model.road;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.AbstractModel;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * A common space neutral implementation of {@link RoadModel}. It implements a
 * data structure for managing objects and locations and checks many
 * preconditions as defined in {@link RoadModel}.
 * @param <T> The type of the location representation that is used for storing
 *            object locations. This location representation should only be used
 *            internally in the model.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class AbstractRoadModel<T> extends AbstractModel<RoadUser>
        implements RoadModel {

    /**
     * A mapping of {@link RoadUser} to location.
     */
    protected volatile Map<RoadUser, T> objLocs;

    /**
     * Create a new instance using the specified graph.
     */
    public AbstractRoadModel() {
        super(RoadUser.class);
        objLocs = createObjectToLocationMap();
    }

    /**
     * Defines the specific {@link Map} instance used for storing object
     * locations.
     * @return The map instance.
     */
    protected Map<RoadUser, T> createObjectToLocationMap() {
        return Collections.synchronizedMap(new LinkedHashMap<RoadUser, T>());
    }

    /**
     * A function for converting the location representation to a {@link Point}.
     * @param locObj The location to be converted.
     * @return A {@link Point} indicating the position as represented by the
     *         specified location.
     */
    protected abstract Point locObj2point(T locObj);

    /**
     * A function for converting a {@link Point} to the location representation
     * of this model.
     * @param point The {@link Point} to be converted.
     * @return The location.
     */
    protected abstract T point2LocObj(Point point);

    @Override
    public PathProgress followPath(MovingRoadUser object, Queue<Point> path,
            TimeLapse time) {
        checkArgument(objLocs.containsKey(object), "object must have a location");
        checkArgument(path.peek() != null, "path can not be empty");
        checkArgument(time.hasTimeLeft(), "can not follow path when to time is left");
        return doFollowPath(object, path, time);
    }

    /**
     * Should be overriden by subclasses to define
     * {@link RoadModel#followPath(MovingRoadUser, Queue, TimeLapse)}.
     * @param object The object that is moved.
     * @param path The path that is followed.
     * @param time The time that is available for travel.
     * @return A {@link PathProgress} instance containing the actual travel
     *         details.
     */
    protected abstract PathProgress doFollowPath(MovingRoadUser object,
            Queue<Point> path, TimeLapse time);

    @Override
    public void addObjectAt(RoadUser newObj, Point pos) {
        checkArgument(!objLocs.containsKey(newObj), "Object is already added");
        objLocs.put(newObj, point2LocObj(pos));
    }

    @Override
    public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
        checkArgument(!objLocs.containsKey(newObj), "Object " + newObj
                + " is already added.");
        checkArgument(objLocs.containsKey(existingObj), "Object " + existingObj
                + " does not exist.");
        objLocs.put(newObj, objLocs.get(existingObj));
    }

    @Override
    public void removeObject(RoadUser roadUser) {
        checkArgument(roadUser != null, "RoadUser can not be null");
        checkArgument(objLocs.containsKey(roadUser), "RoadUser does not exist.");
        objLocs.remove(roadUser);
    }

    @Override
    public void clear() {
        objLocs.clear();
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
        }// it is save to release the lock now

        Map<RoadUser, Point> theMap = new LinkedHashMap<RoadUser, Point>();
        for (java.util.Map.Entry<RoadUser, T> entry : copiedMap.entrySet()) {
            theMap.put(entry.getKey(), locObj2point(entry.getValue()));
        }
        return theMap;
    }

    @Override
    public Point getPosition(RoadUser roadUser) {
        checkArgument(roadUser != null, "object can not be null");
        checkArgument(containsObject(roadUser), "RoadUser does not exist");
        return locObj2point(objLocs.get(roadUser));
    }

    @Override
    public Collection<Point> getObjectPositions() {
        return getObjectsAndPositions().values();
    }

    @Override
    public Set<RoadUser> getObjects() {
        synchronized (objLocs) {
            Set<RoadUser> copy = new LinkedHashSet<RoadUser>();
            copy.addAll(objLocs.keySet());
            return copy;
        }
    }

    @Override
    public Set<RoadUser> getObjects(Predicate<RoadUser> predicate) {
        return Sets.filter(getObjects(), predicate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Y extends RoadUser> Set<Y> getObjectsAt(RoadUser roadUser,
            Class<Y> type) {
        checkArgument(roadUser != null, "roadUser can not be null");
        checkArgument(type != null, "type can not be null");
        Set<Y> result = new HashSet<Y>();
        for (RoadUser ru : getObjects(new SameLocationPredicate(roadUser, type,
                this))) {
            result.add((Y) ru);
        }
        return result;
    }

    private static class SameLocationPredicate implements Predicate<RoadUser> {
        private final RoadUser reference;
        private final RoadModel model;
        private final Class<?> type;

        public SameLocationPredicate(final RoadUser pReference,
                final Class<?> pType, final RoadModel pModel) {
            reference = pReference;
            type = pType;
            model = pModel;
        }

        @Override
        public boolean apply(RoadUser input) {
            return type.isInstance(input)
                    && model.equalPosition(input, reference);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Y extends RoadUser> Set<Y> getObjectsOfType(final Class<Y> type) {
        if (type == null) {
            throw new IllegalArgumentException("type can not be null");
        }
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
        checkArgument(objLocs.containsKey(toObj), " to object should be in RoadModel. "
                + toObj);
        return getShortestPathTo(fromObj, getPosition(toObj));
    }

    @Override
    public List<Point> getShortestPathTo(RoadUser fromObj, Point to) {
        checkArgument(fromObj != null, "fromObj can not be null");
        checkArgument(objLocs.containsKey(fromObj), " from object should be in RoadModel. "
                + fromObj);
        return getShortestPathTo(getPosition(fromObj), to);
    }

    @Override
    public boolean register(RoadUser roadUser) {
        if (roadUser == null) {
            throw new IllegalArgumentException("roadUser can not be null");
        }
        roadUser.initRoadUser(this);
        return true;
    }

    @Override
    public boolean unregister(RoadUser roadUser) {
        checkArgument(roadUser != null, "RoadUser can not be null");
        if (containsObject(roadUser)) {
            removeObject(roadUser);
            return true;
        }
        return false;
    }
}
