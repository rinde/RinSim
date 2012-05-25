/**
 * 
 */
package rinde.sim.core.graph;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.random.RandomGenerator;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> - added edge data
 *         handling
 * @param <E> The type of {@link EdgeData} that is used in the edges.
 * @since 1.0
 */
public interface Graph<E extends EdgeData> {

	public boolean containsNode(Point node);

	public Collection<Point> getOutgoingConnections(Point node);

	public Collection<Point> getIncomingConnections(Point node);

	public boolean hasConnection(Point from, Point to);

	public Connection<E> getConnection(Point from, Point to);

	/**
	 * Get the data associated with connection.
	 * @param from Start of connection
	 * @param to End of connection
	 * @return connection data or <code>null</code> if there is no data or
	 *         connection does not exists.
	 */
	public E connectionData(Point from, Point to);

	public double connectionLength(Point from, Point to);

	public int getNumberOfConnections();

	public List<Connection<E>> getConnections();

	public int getNumberOfNodes();

	public Set<Point> getNodes();

	/**
	 * Add connection to the graph
	 * @param from starting node
	 * @param to end node
	 * @param edgeData data associated with the edge
	 */
	public void addConnection(Point from, Point to, E edgeData);

	public void addConnection(Point from, Point to);

	/**
	 * Add connection to the graph.
	 * @param connection the connection to add.
	 */
	public void addConnection(Connection<E> connection);

	/**
	 * Set connection data
	 * @param from Start point of connection
	 * @param to End point of connection
	 * @param edgeData The edge data used for the connection
	 * @return old edge data or <code>null</null> if there was no edge
	 * @precondition connection from -> to exists
	 * @throws IllegalArgumentException when the connection between nodes do not
	 *             exists
	 */
	public E setEdgeData(Point from, Point to, E edgeData);

	public void addConnections(Collection<Connection<E>> connections);

	public void merge(Graph<E> other);

	public boolean isEmpty();

	public void removeNode(Point node);

	public void removeConnection(Point from, Point to);

	public boolean equals(Graph<? extends E> other);

	/**
	 * Get a random node in graph.
	 * @param generator used to generate the random point.
	 * @return random {@link Point}
	 * @throws NullPointerException should be thrown when parameter is
	 *             <code>null</code>
	 */
	public Point getRandomNode(RandomGenerator generator);

}