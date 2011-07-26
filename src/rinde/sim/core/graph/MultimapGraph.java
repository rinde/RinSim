/**
 * 
 */
package rinde.sim.core.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class MultimapGraph implements Graph {

	private final Multimap<Point, Point> data;

	public MultimapGraph(Multimap<Point, Point> data) {
		this.data = data;
	}

	public MultimapGraph() {
		data = HashMultimap.create();
	}

	@Override
	public boolean containsNode(Point node) {
		return data.containsKey(node);
	}

	@Override
	public Collection<Point> getConnectedNodes(Point node) {
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
		return data.keySet().size();
	}

	@Override
	public void addConnection(Point from, Point to) {
		data.put(from, to);
	}

	@Override
	public Set<Point> getNodes() {
		return Collections.unmodifiableSet(new HashSet<Point>(data.keySet()));
	}

	@Override
	public Collection<Entry<Point, Point>> getConnections() {
		return data.entries();
	}

	// returns the backing multimap
	public Multimap<Point, Point> getMultimap() {
		return Multimaps.unmodifiableMultimap(data);
	}

	@Override
	public void merge(Graph other) {
		if (other instanceof MultimapGraph) {
			data.putAll(((MultimapGraph) other).getMultimap());
		} else {
			addConnections(other.getConnections());
		}

	}

	@Override
	public void addConnections(Collection<Entry<Point, Point>> connections) {
		for (Entry<Point, Point> connection : connections) {
			addConnection(connection.getKey(), connection.getValue());
		}
	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}
}
