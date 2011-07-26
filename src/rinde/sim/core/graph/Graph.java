/**
 * 
 */
package rinde.sim.core.graph;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import rinde.sim.core.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public interface Graph {

	public boolean containsNode(Point node);

	public Collection<Point> getConnectedNodes(Point node);

	public boolean hasConnection(Point from, Point to);

	public int getNumberOfConnections();

	public Collection<Entry<Point, Point>> getConnections();

	public int getNumberOfNodes();

	public List<Point> getNodes();

	public void addConnection(Point from, Point to);

	public void merge(Graph other);

}
