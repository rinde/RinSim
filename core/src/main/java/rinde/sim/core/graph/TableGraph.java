/**
 * 
 */
package rinde.sim.core.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.LinkedHashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Table-based implementation of a graph. Since this graph is backed by a table
 * look ups for both incoming and outgoing connections from nodes is fast.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> - change to the
 *         parametric version
 * @param <E> The type of {@link ConnectionData} that is used in the edges.
 */
public class TableGraph<E extends ConnectionData> extends AbstractGraph<E> {

	// FIXME implement hashCode()

	private final Table<Point, Point, E> data;
	private final E EMPTY;

	/**
	 * Create a new empty graph.
	 * @param emptyValue A special connection data instance that is used as the
	 *            'empty' instance.
	 */
	public TableGraph(E emptyValue) {
		if (emptyValue == null) {
			throw new IllegalArgumentException("the representation of empty value is needed");
		}
		data = LinkedHashBasedTable.create();
		EMPTY = emptyValue;
	}

	@Override
	public Set<Point> getNodes() {
		LinkedHashSet<Point> nodes = new LinkedHashSet<Point>(data.rowKeySet());
		nodes.addAll(data.columnKeySet());
		return Collections.unmodifiableSet(nodes);
	}

	@Override
	public boolean hasConnection(Point from, Point to) {
		return data.contains(from, to);
	}

	@Override
	public int getNumberOfNodes() {
		return getNodes().size();
	}

	@Override
	public int getNumberOfConnections() {
		return data.size();
	}

	@Override
	public boolean containsNode(Point node) {
		return data.containsRow(node) || data.containsColumn(node);
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
	public List<Connection<E>> getConnections() {
		List<Connection<E>> connections = new ArrayList<Connection<E>>();
		for (Cell<Point, Point, E> cell : data.cellSet()) {
			if (EMPTY.equals(cell.getValue())) {
				connections.add(new Connection<E>(cell.getRowKey(), cell.getColumnKey(), null));
			} else {
				connections.add(new Connection<E>(cell.getRowKey(), cell.getColumnKey(), cell.getValue()));
			}
		}
		return connections;
	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	protected boolean isEmptyConnectionData(E connData) {
		return super.isEmptyConnectionData(connData) || EMPTY.equals(connData);
	}

	@Override
	public Connection<E> getConnection(Point from, Point to) {
		if (!hasConnection(from, to)) {
			throw new IllegalArgumentException(from + " -> " + to + " is not a connection.");
		}
		return new Connection<E>(from, to, connectionData(from, to));
	}

	@Override
	public E connectionData(Point from, Point to) {
		E e = data.get(from, to);
		if (EMPTY.equals(e)) {
			return null;
		}
		return e;
	}

	@Override
	protected void doAddConnection(Point from, Point to, E edgeData) {
		if (edgeData == null) {
			data.put(from, to, EMPTY);
		} else {
			data.put(from, to, edgeData);
		}
	}

	@Override
	public E setEdgeData(Point from, Point to, E edgeData) {
		if (hasConnection(from, to)) {
			E e;
			if (edgeData == null) {
				e = data.put(from, to, EMPTY);
			} else {
				e = data.put(from, to, edgeData);
			}

			if (EMPTY.equals(e)) {
				return null;
			}
			return e;
		}
		throw new IllegalArgumentException("Can not get connection length from a non-existing connection.");
	}

}