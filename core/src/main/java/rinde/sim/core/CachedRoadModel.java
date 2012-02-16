/**
 * 
 */
package rinde.sim.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Point;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 * This class provides two additional features compared to {@link RoadModel}:
 * <ul>
 * <li>It provides a cache for finding paths. Every result of an invocation of
 * {@link #getShortestPathTo(Point, Point)},
 * {@link #getShortestPathTo(RoadUser, Point)} and
 * {@link #getShortestPathTo(RoadUser, RoadUser)} is stored in a table. In case
 * a call to one of these methods is done using the same arguments, the path is
 * loaded from the cache instead of recomputed. Additionally the cache can be
 * retrieved and set using the {@link #getPathCache()} and
 * {@link #setPathCache(Table)} methods respectively.</li>
 * 
 * <li>It manages an additional datastructure for storing all objects of the
 * same type. This means that {@link #getObjectsOfType(Class)} is now O(1)
 * instead of O(n).</li>
 * </ul>
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public class CachedRoadModel extends RoadModel {

	private Table<Point, Point, List<Point>> pathTable;
	private final Multimap<Class<?>, RoadUser> classObjectMap;

	public CachedRoadModel(Graph graph) {
		super(graph);

		pathTable = HashBasedTable.create();
		classObjectMap = LinkedHashMultimap.create();
	}

	/**
	 * Sets the path cache.
	 * @param pathTable The new path cache, this overrides any existing cache.
	 */
	public void setPathCache(Table<Point, Point, List<Point>> pathTable) {
		this.pathTable = pathTable;
	}

	/**
	 * @return The path cache currently set or null if there is no path cache.
	 */
	public Table<Point, Point, List<Point>> getPathCache() {
		return pathTable;
	}

	// overrides internal func to add caching
	@Override
	protected List<Point> doGetShortestPathTo(Point from, Point to) {
		if (pathTable.contains(from, to)) {
			return pathTable.get(from, to);
		} else {
			List<Point> path = super.doGetShortestPathTo(from, to);
			pathTable.put(from, to, path);
			return path;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addObjectAt(RoadUser newObj, Point pos) {
		super.addObjectAt(newObj, pos);
		classObjectMap.put(newObj.getClass(), newObj);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj) {
		super.addObjectAtSamePosition(newObj, existingObj);
		classObjectMap.put(newObj.getClass(), newObj);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		super.clear();
		classObjectMap.clear();
	}

	/**
	 * This method returns a set of {@link RoadUser} objects which exist in this
	 * model and are instances of the specified {@link Class}. The returned set
	 * is not a live view on the set, but a new created copy. The time
	 * complexity of this method is O(1).
	 * @param type The type of returned objects.
	 * @return A set of {@link RoadUser} objects.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <Y extends RoadUser> Set<Y> getObjectsOfType(final Class<Y> type) {
		Set<Y> set = new LinkedHashSet<Y>();
		set.addAll((Set<Y>) classObjectMap.get(type));
		return set;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeObject(RoadUser o) {
		super.removeObject(o);
		classObjectMap.remove(o.getClass(), o);
	}

}
