/**
 * 
 */
package rinde.sim.core.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> - added edge data
 *         + and dead end nodes
 * @param <E> The type of {@link ConnectionData} that is used in the edges.
 */
public class MultimapGraph<E extends ConnectionData> implements Graph<E> {

	private final Multimap<Point, Point> data;
	private final HashMap<Connection<E>, E> edgeData;
	private final HashSet<Point> deadEndNodes;

	public MultimapGraph(Multimap<Point, Point> map) {
		this.data = LinkedHashMultimap.create(map);
		this.edgeData = new HashMap<Connection<E>, E>();
		this.deadEndNodes = new HashSet<Point>();
		deadEndNodes.addAll(data.values());
		deadEndNodes.removeAll(data.keySet());
	}

	public MultimapGraph() {
		data = LinkedHashMultimap.create();
		this.edgeData = new HashMap<Connection<E>, E>();
		deadEndNodes = new LinkedHashSet<Point>();
	}

	@Override
	public boolean containsNode(Point node) {
		return data.containsKey(node) || deadEndNodes.contains(node);
	}

	@Override
	public Collection<Point> getOutgoingConnections(Point node) {
		return data.get(node);
	}

	@Override
	public boolean hasConnection(Point from, Point to) {
		return data.containsEntry(from, to);
	}

	@Override
	public int getNumberOfConnections() {
		return data.size();
	}

	@Override
	public int getNumberOfNodes() {
		return data.keySet().size() + deadEndNodes.size();
	}

	@Override
	public void addConnection(Point from, Point to) {
		addConnection(from, to, null);
	}

	@Override
	public void addConnection(Point from, Point to, E connData) {
		if (from.equals(to)) {
			throw new IllegalArgumentException("A connection cannot be circular");
		}
		if (hasConnection(from, to)) {
			throw new IllegalArgumentException("Connection already exists: " + from + " -> " + to);
		}

		data.put(from, to);
		deadEndNodes.remove(from);
		if (!data.containsKey(to)) {
			deadEndNodes.add(to);
		}

		if (connData != null) {
			this.edgeData.put(new Connection<E>(from, to, null), connData);
		}
	}

	@Override
	public void addConnection(Connection<E> c) {
		if (c == null) {
			return;
		}
		addConnection(c.from, c.to, c.getData());
	}

	@Override
	public E setEdgeData(Point from, Point to, E connData) {
		if (!hasConnection(from, to)) {
			throw new IllegalArgumentException("the connection " + from + " -> " + to + "does not exist");
		}
		return this.edgeData.put(new Connection<E>(from, to, null), connData);
	}

	@Override
	public E connectionData(Point from, Point to) {
		return edgeData.get(new Connection<E>(from, to, null));
	}

	@Override
	public Set<Point> getNodes() {
		LinkedHashSet<Point> nodes = new LinkedHashSet<Point>(data.keySet());
		nodes.addAll(deadEndNodes);
		return nodes;
	}

	@Override
	public List<Connection<E>> getConnections() {
		ArrayList<Connection<E>> res = new ArrayList<Connection<E>>(edgeData.size());
		for (Entry<Point, Point> p : data.entries()) {
			Connection<E> connection = new Connection<E>(p.getKey(), p.getValue(), null);
			E eD = edgeData.get(connection);
			connection.setData(eD);
			res.add(connection);
		}
		return res;
	}

	// returns the backing multimap
	public Multimap<Point, Point> getMultimap() {
		return Multimaps.unmodifiableMultimap(data);
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

	/**
	 * Warning: very inefficient! If this function is needed regularly it is
	 * advised to use {@link TableGraph} instead.
	 */
	@Override
	public Collection<Point> getIncomingConnections(Point node) {
		HashSet<Point> set = new LinkedHashSet<Point>();
		for (Entry<Point, Point> entry : data.entries()) {
			if (entry.getValue().equals(node)) {
				set.add(entry.getKey());
			}
		}
		return set;
	}

	/**
	 * Warning: very inefficient! If this function is needed regularly it is
	 * advised to use {@link TableGraph} instead.
	 */
	@Override
	public void removeNode(Point node) {
		// copy data first to avoid concurrent modification exceptions
		List<Point> out = new ArrayList<Point>();
		out.addAll(getOutgoingConnections(node));
		for (Point p : out) {
			removeConnection(node, p);
		}
		List<Point> in = new ArrayList<Point>();
		in.addAll(getIncomingConnections(node));
		for (Point p : in) {
			removeConnection(p, node);
		}
		deadEndNodes.remove(node);
	}

	@Override
	public void removeConnection(Point from, Point to) {
		if (hasConnection(from, to)) {
			data.remove(from, to);
			removeData(from, to);
			if (!data.containsKey(to)) {
				deadEndNodes.add(to);
			}
		} else {
			throw new IllegalArgumentException("Can not remove non-existing connection: " + from + " -> " + to);
		}
	}

	private void removeData(Point from, Point to) {
		edgeData.remove(new Connection<ConnectionData>(from, to, null));
	}

	@Override
	public double connectionLength(Point from, Point to) {
		if (hasConnection(from, to)) {
			E eD = connectionData(from, to);
			return eD != null ? eD.getLength() : Point.distance(from, to);
		}
		throw new IllegalArgumentException("Can not get connection length from a non-existing connection.");
	}

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
		if (getNumberOfNodes() == 0) {
			throw new IllegalStateException("no nodes in the graph");
		}
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