/**
 * 
 */
package rinde.sim.core.model.road;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;

import com.google.common.base.Predicate;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 *         TODO add interface comment
 */
public interface RoadModel extends Model<RoadUser> {

	// TODO add documentation
	// no guarantee about speed of objects
	// talk about queue and time
	// talk about pathprogress
	PathProgress followPath(MovingRoadUser object, Queue<Point> path, TimeLapse time);

	/**
	 * Adds a new object to the model at the specified position.
	 * @param newObj The object to be added to the model. It can not be an
	 *            already added object.
	 * @param pos The position on which the object is to be added. This must be
	 *            a node which already exists in the model.
	 */
	void addObjectAt(RoadUser newObj, Point pos);

	/**
	 * Adds an object at the same position as the existing object.
	 * @param newObj The new object to be added to the model. It can not be an
	 *            already added object.
	 * @param existingObj The existing object which location is used for the
	 *            target of the <code>newObj</code>. This object
	 *            <strong>must</strong> already exist in the model.
	 */
	void addObjectAtSamePosition(RoadUser newObj, RoadUser existingObj);

	/**
	 * Removes the specified {@link RoadUser} from this model.
	 * @param roadUser the object to be removed.
	 */
	void removeObject(RoadUser roadUser);

	/**
	 * Removes all objects on this RoadStructure instance.
	 */
	void clear();

	/**
	 * Checks if the specified {@link RoadUser} exists in the model.
	 * @param obj The {@link RoadUser} to check for existence, may not be
	 *            <code>null</code>.
	 * @return <code>true</code> if <code>obj</code> exists in the model,
	 *         <code>false</code> otherwise.
	 */
	boolean containsObject(RoadUser obj);

	/**
	 * Checks if the specified {@link RoadUser} exists at the specified
	 * position.
	 * @param obj The {@link RoadUser} to check.
	 * @param p The position to check.
	 * @return <code>true</code> if <code>obj</code> exists at position
	 *         <code>p</code>, <code>false</code> otherwise.
	 */
	boolean containsObjectAt(RoadUser obj, Point p);

	/**
	 * Checks if the positions of the <code>obj1</code> and <code>obj2</code>
	 * are equal.
	 * @param obj1 A {@link RoadUser}.
	 * @param obj2 A {@link RoadUser}.
	 * @return <code>true</code> if the positions are equal, <code>false</code>
	 *         otherwise.
	 */
	boolean equalPosition(RoadUser obj1, RoadUser obj2);

	/**
	 * This method returns a mapping of {@link RoadUser} to {@link Point}
	 * objects which exist in this model. The returned map is not a live view on
	 * this model, but a new created copy. TODO add test for this live view case
	 * @return A map of {@link RoadUser} to {@link Point} objects.
	 */
	Map<RoadUser, Point> getObjectsAndPositions();

	/**
	 * Method to retrieve the location of an object.
	 * @param roadUser The object for which the position is examined.
	 * @return The position (as a {@link Point} object) for the specified
	 *         <code>obj</code> object.
	 */
	Point getPosition(RoadUser roadUser);

	// TODO add documentation
	Point getRandomPosition(RandomGenerator rnd);

	/**
	 * This method returns a collection of {@link Point} objects which are the
	 * positions of the objects that exist in this model. The returned
	 * collection is not a live view on the set, but a new created copy.
	 * @return The collection of {@link Point} objects.
	 */
	Collection<Point> getObjectPositions();

	/**
	 * This method returns the set of {@link RoadUser} objects which exist in
	 * this model. The returned set is not a live view on the set, but a new
	 * created copy.
	 * @return The set of {@link RoadUser} objects.
	 */
	Set<RoadUser> getObjects();

	/**
	 * This method returns a set of {@link RoadUser} objects which exist in this
	 * model and satisfy the given {@link Predicate}. The returned set is not a
	 * live view on this model, but a new created copy.
	 * @param predicate The predicate that decides which objects to return.
	 * @return A set of {@link RoadUser} objects.
	 */
	Set<RoadUser> getObjects(Predicate<RoadUser> predicate);

	/**
	 * Returns all objects of the given type located in the same position as the
	 * given {@link RoadUser}.
	 * @param roadUser The object which location is checked for other objects.
	 * @param type The type of the objects to be returned.
	 * @return A set of objects of type <code>type</code>.
	 */
	<Y extends RoadUser> Set<Y> getObjectsAt(RoadUser roadUser, Class<Y> type);

	/**
	 * This method returns a set of {@link RoadUser} objects which exist in this
	 * model and are instances of the specified {@link Class}. The returned set
	 * is not a live view on the set, but a new created copy.
	 * @param type The type of returned objects.
	 * @return A set of {@link RoadUser} objects.
	 */
	<Y extends RoadUser> Set<Y> getObjectsOfType(final Class<Y> type);

	/**
	 * Convenience method for {@link #getShortestPathTo(Point, Point)}
	 * @param fromObj The object which is used as the path origin
	 * @param toObj The object which is used as the path destination
	 * @return The shortest path from 'fromObj' to 'toObj'.
	 */
	List<Point> getShortestPathTo(RoadUser fromObj, RoadUser toObj);

	/**
	 * Convenience method for {@link #getShortestPathTo(Point, Point)}
	 * @param fromObj The object which is used as the path origin
	 * @param to The path destination
	 * @return The shortest path from 'fromObj' to 'to'
	 */
	List<Point> getShortestPathTo(RoadUser fromObj, Point to);

	// TODO add documentation
	List<Point> getShortestPathTo(Point from, Point to);

}