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
	public List<Point> getNodes() {
		return Collections.unmodifiableList(new ArrayList<Point>(data.keySet()));
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.Graph#getConnections()
	 */
	@Override
	public Collection<Entry<Point, Point>> getConnections() {
		return data.entries();
	}

	// returns the backing multimap
	public Multimap<Point, Point> getMultimap() {
		return Multimaps.unmodifiableMultimap(data);
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#merge(rinde.sim.core.graph.Graph)
	 */
	@Override
	public void merge(Graph other) {
		if (other instanceof MultimapGraph) {
			data.putAll(((MultimapGraph) other).getMultimap());
		} else {
			for (Entry<Point, Point> connection : other.getConnections()) {
				data.put(connection.getKey(), connection.getValue());
			}
		}

	}
}
