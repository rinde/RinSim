/**
 * 
 */
package rinde.sim.core.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import rinde.sim.core.Point;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class TableGraph implements Graph {

	private final Table<Point, Point, TableEntry> data;

	public TableGraph() {
		data = HashBasedTable.create();
	}

	/**
	 * @see rinde.sim.core.RoadStructure#getNodes()
	 */
	@Override
	public List<Point> getNodes() {
		return Collections.unmodifiableList(new ArrayList<Point>(data.rowKeySet()));
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
	public Collection<Point> getConnectedNodes(Point node) {
		return data.row(node).keySet();
	}

	@Override
	public void addConnection(Point from, Point to) {
		data.put(from, to, new TableEntry());
	}

	public static TableGraph create() {
		return new TableGraph();
	}

	@Override
	public Collection<Entry<Point, Point>> getConnections() {
		Collection<Entry<Point, Point>> connections = new ArrayList<Entry<Point, Point>>();
		for (Cell<Point, Point, TableEntry> cell : data.cellSet()) {
			connections.add(new EntryImpl(cell.getRowKey(), cell.getColumnKey()));
		}
		return connections;
	}

	@Override
	public void merge(Graph other) {
		for (Entry<Point, Point> connection : other.getConnections()) {
			addConnection(connection.getKey(), connection.getValue());
		}
	}

	class TableEntry {
	}

	private class EntryImpl implements Entry<Point, Point> {

		final Point key;
		final Point value;

		public EntryImpl(Point key, Point value) {
			this.key = key;
			this.value = value;
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
	}

}
