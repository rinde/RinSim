/**
 * 
 */
package rinde.sim.core.graph;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.math.random.RandomGenerator;
import org.neo4j.collections.rtree.Listener;
import org.neo4j.gis.spatial.Layer;
import org.neo4j.gis.spatial.SimplePointLayer;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.tooling.GlobalGraphOperations;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class Neo4jGraph<E extends EdgeData> implements Graph<E> {

	private final static String LAYER_NAME = "test-layer";

	private final GraphDatabaseService graphDb;
	private final SpatialDatabaseService spatialService;
	private final SimplePointLayer layer;
	private final GlobalGraphOperations global;

	public Neo4jGraph(String databaseName, boolean createNew) {
		graphDb = new EmbeddedGraphDatabase(databaseName);
		spatialService = new SpatialDatabaseService(graphDb);
		if (createNew) {
			clear();
		}
		// layer = spatialService.getOrCreatePointLayer("test-layer", null,
		// null);

		global = GlobalGraphOperations.at(graphDb);

		Layer l = spatialService.getLayer(LAYER_NAME);
		if (l == null) {
			layer = spatialService.createSimplePointLayer(LAYER_NAME);
		} else {
			layer = (SimplePointLayer) l;
		}

		// layer = spatialService.createSimplePointLayer("test-layer");

		// Coordinate
		// Node n = graphDb.createNode();
		// n.

		// layer.fin

		// layer.add(geomNode)

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("shutting graphdb down..");
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

		point2coordinate(node);

		// layer.add(coordinate)

		// layer.getSpatialDatabase()

		// layer.getIndex().count()

		// layer.getIndex().

		// graphDb.get

		// layer.getSpatialDatabase().

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
		LinkedHashSet<Point> set = new LinkedHashSet<Point>();
		for (Geometry g : layer.getDataset().getAllGeometries()) {
			set.add(coordinate2point(g.getCoordinate()));
		}
		return set;
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
		Transaction tx = graphDb.beginTx();
		try {
			SpatialDatabaseRecord sdrFrom = layer.add(point2coordinate(from));

			// layer.getIndex().

			// layer.add(geomNode)
			// GeometryFactory.toLineStringArray(lineStrings)

			System.out.println(sdrFrom.getId());
			SpatialDatabaseRecord sdrTo = layer.add(point2coordinate(to));
			System.out.println(sdrTo.getId());
			sdrFrom.getGeomNode().createRelationshipTo(sdrTo.getGeomNode(), GraphRelationshipTypes.CONNECTION);
			tx.success();
		} finally {
			tx.finish();
		}

	}

	Coordinate point2coordinate(Point p) {
		return new Coordinate(p.x, p.y);
	}

	Point coordinate2point(Coordinate c) {
		return new Point(c.x, c.y);
	}

	// Point node2point(Node n) {
	// return n.g
	// }

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

	public void clear() {
		if (spatialService.getLayer(LAYER_NAME) != null) {
			spatialService.deleteLayer(LAYER_NAME, new Listener() {
				@Override
				public void begin(int unitsOfWork) {}

				@Override
				public void worked(int workedSinceLastNotification) {}

				@Override
				public void done() {}
			});
		}
	}

	enum GraphRelationshipTypes implements RelationshipType {
		CONNECTION
	}

}
