/**
 * 
 */
package rinde.sim.core.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import rinde.sim.core.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class Graphs {

	public static Graph unmodifiableGraph(Graph delegate) {
		return new UnmodifiableGraph(delegate);
	}

	private static class UnmodifiableGraph implements Graph {
		final Graph delegate;

		public UnmodifiableGraph(Graph delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean containsNode(Point node) {
			return delegate.containsNode(node);
		}

		@Override
		public Collection<Point> getConnectedNodes(Point node) {
			return Collections.unmodifiableCollection(delegate.getConnectedNodes(node));
		}

		@Override
		public boolean hasConnection(Point from, Point to) {
			return delegate.hasConnection(from, to);
		}

		@Override
		public int getNumberOfConnections() {
			return delegate.getNumberOfConnections();
		}

		@Override
		public Collection<Entry<Point, Point>> getConnections() {
			return Collections.unmodifiableCollection(delegate.getConnections());
		}

		@Override
		public int getNumberOfNodes() {
			return delegate.getNumberOfNodes();
		}

		@Override
		public List<Point> getNodes() {
			return Collections.unmodifiableList(delegate.getNodes());
		}

		@Override
		public void addConnection(Point from, Point to) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void merge(Graph other) {
			throw new UnsupportedOperationException();
		}
	}

}
