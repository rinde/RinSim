/**
 * 
 */
package rinde.sim.core;

import java.util.Collection;
import java.util.List;

import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.Point;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class CachedRoadModel extends RoadModel {

	private Table<Point, Point, List<Point>> pathTable;

	private final Multimap<Class<?>, Object> classObjectMap;

	public CachedRoadModel(Graph graph) {
		super(graph);

		pathTable = HashBasedTable.create();
		classObjectMap = HashMultimap.create();
	}

	public void setPathCache(Table<Point, Point, List<Point>> pathTable) {
		this.pathTable = pathTable;
	}

	public Table<Point, Point, List<Point>> getPathCache() {
		return pathTable;
	}

	@Override
	public List<Point> getShortestPathTo(Object obj, Point dest) {
		assert objLocs.containsKey(obj);
		Point origin = getNode(obj);

		if (pathTable.contains(origin, dest)) {
			return pathTable.get(origin, dest);
		} else {
			List<Point> path = Graphs.shortestPathDistance(graph, origin, dest);
			pathTable.put(origin, dest, path);
			return path;
		}
	}

	@Override
	public void addObjectAt(Object newObj, Point pos) {
		super.addObjectAt(newObj, pos);
		classObjectMap.put(newObj.getClass(), newObj);
	}

	@Override
	public void addObjectAtSamePosition(Object newObj, Object existingObj) {
		super.addObjectAtSamePosition(newObj, existingObj);
		classObjectMap.put(newObj.getClass(), newObj);
	}

	@Override
	public void clear() {
		super.clear();
		classObjectMap.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Y> Collection<Y> getObjectsOfType(final Class<Y> type) {
		return (Collection<Y>) classObjectMap.get(type);
	}

	@Override
	public void removeObject(Object o) {
		super.removeObject(o);
		classObjectMap.remove(o.getClass(), o);
	}

}
