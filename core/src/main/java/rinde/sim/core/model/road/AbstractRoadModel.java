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
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.Point;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * TODO update this doc, it is outdated<br/>
 * The RoadModel is a model that manages a fleet of vehicles ({@link RoadUser}s)
 * on top of a {@link Graph}. Its responsibilities are:
 * <ul>
 * <li>adding and removing objects</li>
 * <li>moving objects around</li>
 * </ul>
 * On top of that the RoadModel provides several functions for retrieving
 * objects and finding the shortest path.
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> changes wrt.
 *         models infrastructure
 * @param <T> The type of object that is stored in the model
 */
public abstract class AbstractRoadModel<T> implements RoadModel {

	protected volatile Map<RoadUser, T> objLocs;

	/**
	 * Create a new instance using the specified graph.
	 */
	public AbstractRoadModel() {
		objLocs = createObjectToLocationMap();
	}

	protected Map<RoadUser, T> createObjectToLocationMap() {
		return Collections.synchronizedMap(new LinkedHashMap<RoadUser, T>());
	}

	protected abstract Point locObj2point(T locObj);

	protected abstract T point2LocObj(Point point);

	// TODO add documentation
	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#followPath(rinde.sim.core.model.MovingRoadUser
	 * , java.util.Queue, long)
	 */
	@Override
	public abstract PathProgress followPath(MovingRoadUser object, Queue<Point> path, TimeLapse time);

	// TODO add javadoc
	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#addObjectAt(rinde.sim.core.model.RoadUser,
	 * rinde.sim.core.graph.Point)
	 */
	@Override
	public void addObjectAt(RoadUser newObj, Point pos) {
		checkArgument(!objLocs.containsKey(newObj), "Object is already added");
		objLocs.put(newObj, point2LocObj(pos));
	}

	// TODO add javadoc
	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#addObjectAtSamePosition(rinde.sim.core
	 * .model.RoadUser, rinde.sim.core.model.RoadUser)
	 */
	@Override
	public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
		checkArgument(!objLocs.containsKey(newObj), "Object " + newObj + " is already added.");
		checkArgument(objLocs.containsKey(existingObj), "Object " + existingObj + " does not exist.");
		objLocs.put(newObj, objLocs.get(existingObj));
	}

	// TODO add javadoc
	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#removeObject(rinde.sim.core.model.RoadUser
	 * )
	 */
	@Override
	public void removeObject(RoadUser roadUser) {
		checkArgument(roadUser != null, "RoadUser can not be null");
		checkArgument(objLocs.containsKey(roadUser), "RoadUser does not exist.");
		objLocs.remove(roadUser);
	}

	// TODO add javadoc
	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.model.RoadModel#clear()
	 */
	@Override
	public void clear() {
		objLocs.clear();
	}

	// TODO add javadoc
	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#containsObject(rinde.sim.core.model.RoadUser
	 * )
	 */
	@Override
	public boolean containsObject(RoadUser obj) {
		checkArgument(obj != null, "obj can not be null");
		return objLocs.containsKey(obj);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#containsObjectAt(rinde.sim.core.model.
	 * RoadUser, rinde.sim.core.graph.Point)
	 */
	@Override
	public boolean containsObjectAt(RoadUser obj, Point p) {
		checkArgument(p != null, "point can not be null");
		if (containsObject(obj)) {
			return objLocs.get(obj).equals(p);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#equalPosition(rinde.sim.core.model.RoadUser
	 * , rinde.sim.core.model.RoadUser)
	 */
	@Override
	public boolean equalPosition(RoadUser obj1, RoadUser obj2) {
		return containsObject(obj1) && containsObject(obj2) && getPosition(obj1).equals(getPosition(obj2));
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.model.RoadModel#getObjectsAndPositions()
	 */
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

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#getPosition(rinde.sim.core.model.RoadUser)
	 */
	@Override
	public Point getPosition(RoadUser roadUser) {
		checkArgument(roadUser != null, "object can not be null");
		checkArgument(containsObject(roadUser), "RoadUser does not exist");
		return locObj2point(objLocs.get(roadUser));
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.model.RoadModel#getObjectPositions()
	 */
	@Override
	public Collection<Point> getObjectPositions() {
		return getObjectsAndPositions().values();
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.model.RoadModel#getObjects()
	 */
	@Override
	public Set<RoadUser> getObjects() {
		synchronized (objLocs) {
			Set<RoadUser> copy = new LinkedHashSet<RoadUser>();
			copy.addAll(objLocs.keySet());
			return copy;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#getObjects(com.google.common.base.Predicate
	 * )
	 */
	@Override
	public Set<RoadUser> getObjects(Predicate<RoadUser> predicate) {
		return Sets.filter(getObjects(), predicate);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#getObjectsAt(rinde.sim.core.model.RoadUser
	 * , java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <Y extends RoadUser> Set<Y> getObjectsAt(RoadUser roadUser, Class<Y> type) {
		checkArgument(roadUser != null, "roadUser can not be null");
		checkArgument(type != null, "type can not be null");
		Set<Y> result = new HashSet<Y>();
		for (RoadUser ru : getObjects(new SameLocationPredicate(roadUser, type, this))) {
			result.add((Y) ru);
		}
		return result;
	}

	private static class SameLocationPredicate implements Predicate<RoadUser> {
		private final RoadUser reference;
		private final RoadModel model;
		private final Class<?> type;

		public SameLocationPredicate(final RoadUser pReference, final Class<?> pType, final RoadModel pModel) {
			reference = pReference;
			type = pType;
			model = pModel;
		}

		@Override
		public boolean apply(RoadUser input) {
			return type.isInstance(input) && model.equalPosition(input, reference);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.model.RoadModel#getObjectsOfType(java.lang.Class)
	 */
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

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#getShortestPathTo(rinde.sim.core.model
	 * .RoadUser, rinde.sim.core.model.RoadUser)
	 */
	@Override
	public List<Point> getShortestPathTo(RoadUser fromObj, RoadUser toObj) {
		checkArgument(fromObj != null, "fromObj can not be null");
		checkArgument(objLocs.containsKey(toObj), " to object should be in RoadModel. " + toObj);
		return getShortestPathTo(fromObj, getPosition(toObj));
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#getShortestPathTo(rinde.sim.core.model
	 * .RoadUser, rinde.sim.core.graph.Point)
	 */
	@Override
	public List<Point> getShortestPathTo(RoadUser fromObj, Point to) {
		checkArgument(fromObj != null, "fromObj can not be null");
		checkArgument(objLocs.containsKey(fromObj), " from object should be in RoadModel. " + fromObj);
		return getShortestPathTo(getPosition(fromObj), to);
	}

	/**
	 * Searches for the shortest path between <code>from</code> and
	 * <code>to</code>.
	 * @param from The start position of the path.
	 * @param to The end position of the path.
	 * @return The shortest path between <code>from</code> and <code>to</code>
	 *         if it exists, <code>null</code> otherwise.
	 * @see Graphs#shortestPathEuclideanDistance(Graph, Point, Point)
	 */
	@Override
	public abstract List<Point> getShortestPathTo(Point from, Point to);

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#register(rinde.sim.core.model.RoadUser)
	 */
	@Override
	public boolean register(RoadUser roadUser) {
		if (roadUser == null) {
			throw new IllegalArgumentException("roadUser can not be null");
		}
		roadUser.initRoadUser(this);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.model.RoadModel#unregister(rinde.sim.core.model.RoadUser)
	 */
	@Override
	public boolean unregister(RoadUser roadUser) {
		checkArgument(roadUser != null, "RoadUser can not be null");
		if (containsObject(roadUser)) {
			removeObject(roadUser);
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.model.RoadModel#getSupportedType()
	 */
	@Override
	public Class<RoadUser> getSupportedType() {
		return RoadUser.class;
	}

}
