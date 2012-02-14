/**
 * 
 */
package rinde.sim.core.graph;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public interface Graph {

	public boolean containsNode(Point node);

	public Collection<Point> getOutgoingConnections(Point node);

	public Collection<Point> getIncomingConnections(Point node);

	public boolean hasConnection(Point from, Point to);

	public double connectionLength(Point from, Point to);

	public int getNumberOfConnections();

	public Collection<Entry<Point, Point>> getConnections();

	public int getNumberOfNodes();

	public Set<Point> getNodes();

	public void addConnection(Point from, Point to);

	public void addConnections(Collection<Entry<Point, Point>> connections);

	public void merge(Graph other);

	public boolean isEmpty();

	public void removeNode(Point node);

	public void removeConnection(Point from, Point to);

	public boolean equals(Graph other);

}
