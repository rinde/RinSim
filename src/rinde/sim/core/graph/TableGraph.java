/**
 * 
 */
package rinde.sim.core.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class TableGraph implements Graph {

	private final Table<Point, Point, ConnectionAttributes> data;

	public TableGraph() {
		data = HashBasedTable.create();
	}

	/**
	 * @see rinde.sim.core.RoadModel#getNodes()
	 */
	@Override
	public Set<Point> getNodes() {
		return Collections.unmodifiableSet(new HashSet<Point>(data.rowKeySet()));
	}

	@Override
	public boolean hasConnection(Point from, Point to) {
		return data.contains(from, to);
	}

	@Override
	public int getNumberOfNodes() {
		return data.rowKeySet().size();
	}

	@Override
	public int getNumberOfConnections() {
		return data.size();
	}

	@Override
	public boolean containsNode(Point node) {
		return data.containsRow(node);
	}

	@Override
	public Collection<Point> getOutgoingConnections(Point node) {
		return data.row(node).keySet();
	}

	@Override
	public Collection<Point> getIncomingConnections(Point node) {
		return data.column(node).keySet();
	}

	@Override
	public void removeNode(Point node) {
		data.row(node).clear();
		data.column(node).clear();
	}

	@Override
	public void removeConnection(Point from, Point to) {
		if (hasConnection(from, to)) {
			data.remove(from, to);
		} else {
			throw new IllegalArgumentException("Can not remove non-existing connection: " + from + " -> " + to);
		}

	}

	@Override
	public void addConnection(Point from, Point to) {

		addConnection(from, to, -1);
	}

	public void addConnection(Point from, Point to, double length) {
		addConnection(from, to, new ConnectionAttributes(length));
	}

	private void addConnection(Point from, Point to, ConnectionAttributes attributes) {
		if (from.equals(to)) {
			throw new IllegalArgumentException("A connection cannot be circular: " + from + " -> " + to);
		}
		data.put(from, to, attributes);
	}

	public static TableGraph create() {
		return new TableGraph();
	}

	@Override
	public Collection<Entry<Point, Point>> getConnections() {
		Collection<Entry<Point, Point>> connections = new ArrayList<Entry<Point, Point>>();
		for (Cell<Point, Point, ConnectionAttributes> cell : data.cellSet()) {
			connections.add(new Connection(cell.getRowKey(), cell.getColumnKey(), cell.getValue()));
		}
		return connections;
	}

	@Override
	public void merge(Graph other) {
		addConnections(other.getConnections());
	}

	@Override
	public void addConnections(Collection<Entry<Point, Point>> connections) {
		for (Entry<Point, Point> connection : connections) {
			if (connection instanceof Connection) {
				addConnection(connection.getKey(), connection.getValue(), ((Connection) connection).getAttributes());
			} else {
				addConnection(connection.getKey(), connection.getValue());
			}
		}
	}

	class ConnectionAttributes {
		double length;

		public ConnectionAttributes() {
			this(-1);
		}

		public ConnectionAttributes(double length) {
			this.length = length;
		}
	}

	private class Connection implements Entry<Point, Point> {

		final Point key;
		final Point value;
		final ConnectionAttributes attributes;

		public Connection(Point key, Point value, ConnectionAttributes attributes) {
			this.key = key;
			this.value = value;
			this.attributes = attributes;
		}

		@Override
		public Point getKey() {
			return key;
		}

		@Override
		public Point getValue() {
			return value;
		}

		@Override
		public Point setValue(Point value) {
			throw new UnsupportedOperationException();
		}

		public ConnectionAttributes getAttributes() {
			return attributes;
		}

		@Override
		public String toString() {
			return key + "=" + value;
		}

	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public double connectionLength(Point from, Point to) {
		if (hasConnection(from, to)) {
			double length = data.get(from, to).length;
			return length >= 0 ? length : Point.distance(from, to);
		}
		throw new IllegalArgumentException("Can not get connection length from a non-existing connection.");
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Graph ? equals((Graph) other) : false;
	}

	@Override
	public boolean equals(Graph other) {
		return Graphs.equals(this, other);
	}

}
