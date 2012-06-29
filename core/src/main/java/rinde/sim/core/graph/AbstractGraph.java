/**
 * 
 */
package rinde.sim.core.graph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

/**
 * Abstract graph implementation providing basic implementations of several
 * graph functions.
 * @param <E> The type of {@link ConnectionData} that is used at the
 *            {@link Connection}s.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class AbstractGraph<E extends ConnectionData> implements Graph<E> {

	/**
	 * Create a new empty graph.
	 */
	public AbstractGraph() {
		super();
	}

	@Override
	public double connectionLength(Point from, Point to) {
		checkArgument(hasConnection(from, to), "Can not get connection length from a non-existing connection.");
		E connData = connectionData(from, to);
		return !isEmptyConnectionData(connData) ? connData.getLength() : Point.distance(from, to);
	}

	/**
	 * Determines whether a connection data is 'empty'. Default only
	 * <code>null</code> is considered as an empty connection data. This can be
	 * overriden to include a specific instance of connection data to be the
	 * 'empty' instance.
	 * @param connData The connection data to check.
	 * @return <code>true</code> if the specified connection data is considered
	 *         empty, <code>false</code> otherwise.
	 */
	protected boolean isEmptyConnectionData(E connData) {
		return connData == null;
	}

	@Override
	public void addConnection(Point from, Point to) {
		addConnection(from, to, null);
	}

	@Override
	public void addConnection(Connection<E> c) {
		if (c == null) {
			return;
		}
		addConnection(c.from, c.to, c.getData());
	}

	@Override
	public void addConnections(Collection<Connection<E>> connections) {
		for (Connection<E> connection : connections) {
			addConnection(connection);
		}
	}

	@Override
	public void merge(Graph<E> other) {
		addConnections(other.getConnections());
	}

	@Override
	public void addConnection(Point from, Point to, E connData) {
		checkArgument(!from.equals(to), "A connection cannot be circular: " + from + " -> " + to);
		checkArgument(!hasConnection(from, to), "Connection already exists: " + from + " -> " + to);
		doAddConnection(from, to, connData);
	}

	/**
	 * Must be overriden by implementors. It should add a connection between
	 * from and to. It can be assumed that the connection does not yet exist and
	 * that it is not circular.
	 * @param from Starting point of the connection.
	 * @param to End point of the connection.
	 * @param connData The data to be associated to the connection.
	 */
	protected abstract void doAddConnection(Point from, Point to, E connData);

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean equals(Object other) {
		return other instanceof Graph ? equals((Graph) other) : false;
	}

	@Override
	public boolean equals(Graph<? extends E> other) {
		return Graphs.equals(this, other);
	}

	@Override
	public Point getRandomNode(RandomGenerator generator) {
		checkState(!isEmpty(), "Can not find a random node in an empty graph.");
		Set<Point> nodes = getNodes();
		int idx = generator.nextInt(nodes.size());
		int i = 0;
		for (Point point : nodes) {
			if (idx == i++) {
				return point;
			}
		}
		return null; // should not happen
	}

	@Override
	public Connection<E> getConnection(Point from, Point to) {
		if (!hasConnection(from, to)) {
			throw new IllegalArgumentException(from + " -> " + to + " is not a connection.");
		}
		return new Connection<E>(from, to, connectionData(from, to));
	}

}