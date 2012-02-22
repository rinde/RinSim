/**
 * 
 */
package rinde.sim.core.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


import com.google.common.collect.LinkedHashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * Table-based implementation of the graph. 
 * TODO add more comments
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> - change to the parametric version
 * 
 */
public class TableGraph<E extends EdgeData> implements Graph<E> {

	private final Table<Point, Point, E> data;
	
	private final E EMPTY;

	public TableGraph(E emptyValue) {
		if(emptyValue == null) throw new IllegalArgumentException("the representation of empty value is needed");
		data = LinkedHashBasedTable.create();
		EMPTY = emptyValue;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Point> getNodes() {
		LinkedHashSet<Point> nodes = new LinkedHashSet<Point>(data.rowKeySet());
		nodes.addAll(data.columnKeySet());
		//TODO [bm] unmodifiable is really needed ???
//		return Collections.unmodifiableSet(nodes);
		return nodes;
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
	public void addConnection(Point from, Point to) {
		addConnection(from, to, null);
	}


	@Override
	public List<Connection<E>> getConnections() {
		List<Connection<E>> connections = new ArrayList<Connection<E>>();
		for (Cell<Point, Point, E> cell : data.cellSet()) {
			if(EMPTY.equals(cell.getValue()))
				connections.add(new Connection<E>(cell.getRowKey(), cell.getColumnKey(), null));
			else connections.add(new Connection<E>(cell.getRowKey(), cell.getColumnKey(), cell.getValue()));
		}
		return connections;
	}

	@Override
	public void merge(Graph<E> other) {
		addConnections(other.getConnections());
	}

	@Override
	public void addConnections(Collection<Connection<E>> connections) {
		for (Connection<E> connection : connections) {
			addConnection(connection);			
		}
	}


	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public double connectionLength(Point from, Point to) {
		if (hasConnection(from, to)) {
			E e = data.get(from, to);
			return EMPTY.equals(e) ? Point.distance(from, to) : e.getLength();
		}
		throw new IllegalArgumentException("Can not get connection length from a non-existing connection.");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean equals(Object other) {
		return other instanceof Graph ? equals((Graph) other) : false;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public E connectionData(Point from, Point to) {
		E e = data.get(from, to);
		if(EMPTY.equals(e)) return null;
		return e;
	}

	@Override
	public void addConnection(Point from, Point to, E edgeData) {
		if (from.equals(to)) {
			throw new IllegalArgumentException("A connection cannot be circular: " + from + " -> " + to);
		}
		if(hasConnection(from, to)) {
			throw new IllegalArgumentException("A connection exists: " + from + " -> " + to);
		}
		if(edgeData == null) {
			data.put(from, to, EMPTY);			
		} else {
			data.put(from, to, edgeData);
		}
	}
	
	@Override
	public void addConnection(Connection<E> c) {
		if(c == null) return;
		addConnection(c.from, c.to, c.edgeData);
	}

	@Override
	public E setEdgeData(Point from, Point to, E edgeData) {
		if(hasConnection(from, to)) {
			E e = data.put(from, to, edgeData);
			if(EMPTY.equals(e)) return null;
			return e;
		}
		throw new IllegalArgumentException("Can not get connection length from a non-existing connection.");
	}

	@Override
	public boolean equals(Graph<? extends E> other) {
		return Graphs.equals(this, other);
	}
}