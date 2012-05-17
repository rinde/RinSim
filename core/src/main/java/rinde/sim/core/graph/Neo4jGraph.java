/**
 * 
 */
package rinde.sim.core.graph;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.math.random.RandomGenerator;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class Neo4jGraph<E extends EdgeData> implements Graph<E> {

	public Neo4jGraph() {
		final GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("testdb");
		SpatialDatabaseService db = new SpatialDatabaseService(graphDb);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#containsNode(rinde.sim.core.graph.Point)
	 */
	@Override
	public boolean containsNode(Point node) {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.graph.Graph#getOutgoingConnections(rinde.sim.core.graph
	 * .Point)
	 */
	@Override
	public Collection<Point> getOutgoingConnections(Point node) {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.graph.Graph#getIncomingConnections(rinde.sim.core.graph
	 * .Point)
	 */
	@Override
	public Collection<Point> getIncomingConnections(Point node) {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#hasConnection(rinde.sim.core.graph.Point,
	 * rinde.sim.core.graph.Point)
	 */
	@Override
	public boolean hasConnection(Point from, Point to) {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#getConnection(rinde.sim.core.graph.Point,
	 * rinde.sim.core.graph.Point)
	 */
	@Override
	public Connection<E> getConnection(Point from, Point to) {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.graph.Graph#connectionData(rinde.sim.core.graph.Point,
	 * rinde.sim.core.graph.Point)
	 */
	@Override
	public E connectionData(Point from, Point to) {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.graph.Graph#connectionLength(rinde.sim.core.graph.Point,
	 * rinde.sim.core.graph.Point)
	 */
	@Override
	public double connectionLength(Point from, Point to) {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#getNumberOfConnections()
	 */
	@Override
	public int getNumberOfConnections() {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#getConnections()
	 */
	@Override
	public List<Connection<E>> getConnections() {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#getNumberOfNodes()
	 */
	@Override
	public int getNumberOfNodes() {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#getNodes()
	 */
	@Override
	public Set<Point> getNodes() {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#addConnection(rinde.sim.core.graph.Point,
	 * rinde.sim.core.graph.Point, rinde.sim.core.graph.EdgeData)
	 */
	@Override
	public void addConnection(Point from, Point to, E edgeData) {
		throw new NotImplementedException();

	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#addConnection(rinde.sim.core.graph.Point,
	 * rinde.sim.core.graph.Point)
	 */
	@Override
	public void addConnection(Point from, Point to) {
		throw new NotImplementedException();

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.graph.Graph#addConnection(rinde.sim.core.graph.Connection)
	 */
	@Override
	public void addConnection(Connection<E> connection) {
		throw new NotImplementedException();

	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#setEdgeData(rinde.sim.core.graph.Point,
	 * rinde.sim.core.graph.Point, rinde.sim.core.graph.EdgeData)
	 */
	@Override
	public E setEdgeData(Point from, Point to, E edgeData) {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#addConnections(java.util.Collection)
	 */
	@Override
	public void addConnections(Collection<Connection<E>> connections) {
		throw new NotImplementedException();

	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#merge(rinde.sim.core.graph.Graph)
	 */
	@Override
	public void merge(Graph<E> other) {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#removeNode(rinde.sim.core.graph.Point)
	 */
	@Override
	public void removeNode(Point node) {
		throw new NotImplementedException();

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.graph.Graph#removeConnection(rinde.sim.core.graph.Point,
	 * rinde.sim.core.graph.Point)
	 */
	@Override
	public void removeConnection(Point from, Point to) {
		throw new NotImplementedException();

	}

	/*
	 * (non-Javadoc)
	 * @see rinde.sim.core.graph.Graph#equals(rinde.sim.core.graph.Graph)
	 */
	@Override
	public boolean equals(Graph<? extends E> other) {
		throw new NotImplementedException();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * rinde.sim.core.graph.Graph#getRandomNode(org.apache.commons.math.random
	 * .RandomGenerator)
	 */
	@Override
	public Point getRandomNode(RandomGenerator generator) {
		throw new NotImplementedException();
	}

}
