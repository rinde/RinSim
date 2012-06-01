package rinde.sim.core.graph;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math.random.MersenneTwister;
import org.apache.commons.math.random.RandomGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Function;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
@RunWith(Parameterized.class)
public class GraphsTest {
	protected static final double DELTA = 0.0001;

	Graph<LengthEdgeData> graph;
	Class<? extends Graph<LengthEdgeData>> graphType;

	public GraphsTest(Class<? extends Graph<LengthEdgeData>> c) {
		graphType = c;
	}

	@Parameters
	public static Collection<Object[]> configs() {
		return Arrays.asList(new Object[][] { { TestMultimapGraph.class }, { TestTableGraph.class } });
	}

	@Before
	public void setUp() throws InstantiationException, IllegalAccessException {
		graph = graphType.newInstance();
	}

	@Test(expected = IllegalArgumentException.class)
	public void addConnection2() {
		graph.addConnection(new Point(0, 0), new Point(0, 0));
	}

	@Test
	public void addNullConnection() {
		graph.addConnection(null);
	}

	@Test
	public void shortestPathConsistencyCheck() {
		Point A, B, C, D;
		A = new Point(0, 0);
		B = new Point(0, 10);
		C = new Point(10, 10);
		D = new Point(10, 0);
		Graphs.addBiPath(graph, A, B, C, D, A);

		List<Point> prevPath = Graphs.shortestPathEuclidianDistance(graph, A, C);
		for (int i = 0; i < 100; i++) {
			List<Point> newPath = Graphs.shortestPathEuclidianDistance(graph, A, C);
			assertEquals(prevPath, newPath);
			prevPath = newPath;
		}
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void tableGraphConstructor() {
		new TableGraph<MultiAttributeEdgeData>(null);
	}

	/**
	 * In this test there are two paths of equal length between two nodes. The
	 * function should always return the same path.
	 */
	@Test
	public void shortestPathConsistencyCheck2() {
		Point N, NE, E, SE, S, SW, W, NW;
		N = new Point(0, 5);
		NE = new Point(5, 5);
		E = new Point(5, 0);
		SE = new Point(5, -5);
		S = new Point(0, -5);
		SW = new Point(-5, -5);
		W = new Point(-5, 0);
		NW = new Point(-5, 5);
		Graphs.addBiPath(graph, N, NE, E, SE, S, SW, W, NW);

		List<Point> prevPath = Graphs.shortestPathEuclidianDistance(graph, N, S);
		for (int i = 0; i < 100; i++) {
			List<Point> newPath = Graphs.shortestPathEuclidianDistance(graph, N, S);
			assertEquals(prevPath, newPath);
			prevPath = newPath;
		}

	}

	@Test(expected = IllegalArgumentException.class)
	public void shortestPathNull() {
		Graphs.shortestPathEuclidianDistance(graph, null, new Point(2, 3));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shortestPathNotExistingPoint() {
		Graphs.shortestPathEuclidianDistance(graph, new Point(1, 2), new Point(2, 3));
	}

	@Test(expected = PathNotFoundException.class)
	public void noShortestPath() {
		Point from = new Point(0, 0);
		Graphs.addBiPath(graph, from, new Point(1, 0));
		Point to = new Point(10, 0);
		Graphs.addBiPath(graph, to, new Point(9, 0));
		Graphs.shortestPathEuclidianDistance(graph, from, to);
	}

	@Test
	public void connectionOrder() {
		Point N, NE, E, SE, S, SW, W, NW;
		N = new Point(0, 5);
		NE = new Point(5, 5);
		E = new Point(5, 0);
		SE = new Point(5, -5);
		S = new Point(0, -5);
		SW = new Point(-5, -5);
		W = new Point(-5, 0);
		NW = new Point(-5, 5);
		Graphs.addPath(graph, N, NE, E, SE, S, SW, W, NW);
		List<Point> points = Arrays.asList(N, NE, E, SE, S, SW, W, NW);

		List<Connection<LengthEdgeData>> connections = graph.getConnections();
		for (int i = 1; i < points.size(); i++) {
			assertTrue(connections.get(i - 1).from == points.get(i - 1));
			assertTrue(connections.get(i - 1).to == points.get(i));
		}
	}

	@Test
	public void incomingConnectionsOrder() {
		Point incoming = new Point(0, 0);
		Point p0 = new Point(1, 0);
		Point p1 = new Point(2, 0);
		Point p2 = new Point(3, 0);
		Point p3 = new Point(4, 0);
		Point p4 = new Point(5, 0);
		Point p5 = new Point(6, 0);

		List<Point> points = Arrays.asList(p0, p1, p2, p3, p4, p5);
		for (Point p : points) {
			graph.addConnection(p, incoming);
		}

		List<Point> incomingConn = new ArrayList<Point>(graph.getIncomingConnections(incoming));
		for (int i = 0; i < incomingConn.size(); i++) {
			assertTrue(incomingConn.get(i) == points.get(i));
		}
	}

	@Test
	public void outgoingConnectionsOrder() {
		Point outgoing = new Point(0, 0);
		Point p0 = new Point(1, 0);
		Point p1 = new Point(2, 0);
		Point p2 = new Point(3, 0);
		Point p3 = new Point(4, 0);
		Point p4 = new Point(5, 0);
		Point p5 = new Point(6, 0);

		List<Point> points = Arrays.asList(p0, p1, p2, p3, p4, p5);
		for (Point p : points) {
			graph.addConnection(outgoing, p);
		}

		List<Point> outgoingConn = new ArrayList<Point>(graph.getOutgoingConnections(outgoing));
		for (int i = 0; i < outgoingConn.size(); i++) {
			assertTrue(outgoingConn.get(i) == points.get(i));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void removeConnectionFail() {
		graph.removeConnection(new Point(0, 0), new Point(1, 0));
	}

	@Test
	public void isEmtpy() {
		assertTrue(graph.isEmpty());
		graph.addConnection(new Point(0, 0), new Point(1, 0));
		assertFalse(graph.isEmpty());
		graph.removeConnection(new Point(0, 0), new Point(1, 0));
		assertTrue(graph.isEmpty());
	}

	@Test(expected = IllegalArgumentException.class)
	public void connectionLengthFail() {
		graph.connectionLength(new Point(0, 3), new Point(4, 5));
	}

	@Test
	public void edgeDataUsage() {
		Point A = new Point(0, 0), B = new Point(0, 1), C = new Point(1, 0);

		graph.addConnection(A, B);
		graph.addConnection(new Connection<LengthEdgeData>(B, A, new LengthEdgeData(1.5)));
		graph.addConnection(B, C, new LengthEdgeData(2));
		graph.addConnection(A, C, new LengthEdgeData(Double.NaN)); // explicit
																	// empty
																	// value

		assertNull("existing but empty", graph.connectionData(A, B));
		assertNull("non existing", graph.connectionData(C, A));
		// assertNull("explicit null A->C", graph.connectionData(A, C)); //
		// works only for TableGraph

		assertNotNull("existing B->A", graph.connectionData(B, A));
		assertNotNull("existing B->C", graph.connectionData(B, C));

		// use of the edge data
		assertEquals(1, graph.connectionLength(A, B), DELTA);
		assertEquals(1.5, graph.connectionLength(B, A), DELTA);
		assertEquals(2, graph.connectionLength(B, C), DELTA);
		try {
			graph.connectionLength(C, B);
			fail();
		} catch (IllegalArgumentException e) {}

	}

	@Test
	public void equalsTest() {
		assertFalse(graph.equals(new Object()));
		assertTrue(graph.equals(graph));

		Point N = new Point(0, 5);
		Point E = new Point(5, 0);
		Point S = new Point(0, -5);
		Point W = new Point(-5, 0);

		Graphs.addBiPath(graph, N, E, S, W, N);
		assertTrue(graph.equals(graph));

		Graph<LengthEdgeData> g1 = new TableGraph<LengthEdgeData>(LengthEdgeData.EMPTY);
		g1.merge(graph);
		assertEquals(g1, graph);

		Graph<LengthEdgeData> g2 = new MultimapGraph<LengthEdgeData>();
		g2.merge(graph);
		assertEquals(g2, graph);
		assertEquals(g1, g2);

		g1.removeConnection(N, E);
		assertFalse(g1.equals(graph));

		g1.removeNode(N);
		assertFalse(g1.equals(graph));

		Point C = new Point(0, 0);
		Graphs.addBiPath(g1, W, C, E);
		assertFalse(g1.equals(graph));

		graph.removeConnection(N, E);
		graph.addConnection(N, E, new LengthEdgeData(10));
		assertFalse(g1.equals(graph));
		assertFalse(graph.equals(g1));

		Graph<LengthEdgeData> g3 = new TableGraph<LengthEdgeData>(LengthEdgeData.EMPTY);
		g3.merge(graph);
		assertEquals(graph, g3);

		g3.removeConnection(N, E);
		g3.addConnection(N, E, new LengthEdgeData(9));
		assertFalse(g3.equals(graph));

		assertFalse(g2.equals(graph));
		assertFalse(g2.equals(g3));
		assertFalse(graph.equals(g2));
		assertFalse(g3.equals(g2));

	}

	@Test
	public void closestObjectsTest() {
		Function<Point, Point> f = new Function<Point, Point>() {
			@Override
			public Point apply(Point input) {
				return input;
			}
		};

		List<Point> points = Arrays.asList(new Point(10, 34), new Point(234, 2), new Point(10, 10), new Point(1, 1));

		List<Point> results = Graphs.findClosestObjects(new Point(0, 0), points, f, 2);
		assertEquals(results.size(), 2);
		assertEquals(new Point(1, 1), results.get(0));
		assertEquals(new Point(10, 10), results.get(1));

		List<Point> results2 = Graphs.findClosestObjects(new Point(0, 0), points, f, 5);
		assertEquals(results2.size(), 4);
		assertEquals(new Point(1, 1), results2.get(0));
		assertEquals(new Point(10, 10), results2.get(1));
		assertEquals(new Point(10, 34), results2.get(2));
		assertEquals(new Point(234, 2), results2.get(3));

	}

	@Test(expected = IllegalArgumentException.class)
	public void nonExistingConnection() {
		graph.getConnection(new Point(1, 2), new Point(2, 3));
	}

	@Test
	public void testRandomNode() {
		RandomGenerator rnd = new MersenneTwister(456);
		for (int i = 0; i < 500; i++) {
			Graphs.addBiPath(graph, new Point(rnd.nextInt(), rnd.nextInt()), new Point(rnd.nextInt(), rnd.nextInt()));
		}
		Graph<LengthEdgeData> unmod = Graphs.unmodifiableGraph(graph);
		Point p1 = graph.getRandomNode(new MersenneTwister(123));
		Point p2 = unmod.getRandomNode(new MersenneTwister(123));
		assertEquals(p1, p2);
	}

	@Test(expected = IllegalStateException.class)
	public void randomNodeEmptyGraph() {
		graph.getRandomNode(new MersenneTwister(234));
	}

	@Test
	public void unmodifiable() {
		Point N = new Point(0, 5);
		Point E = new Point(5, 0);
		Point S = new Point(0, -5);
		Point W = new Point(-5, 0);

		Graphs.addBiPath(graph, N, E, S, W, N);
		Graph<LengthEdgeData> g = Graphs.unmodifiableGraph(graph);
		g.hashCode();

		assertEquals(graph, g);
		assertEquals(g, graph);
		assertFalse(g.equals(new Object()));
		assertFalse(g.isEmpty());

		for (Point p : g.getNodes()) {
			assertArrayEquals(graph.getIncomingConnections(p).toArray(), g.getIncomingConnections(p).toArray());
		}

		for (Connection<LengthEdgeData> c : g.getConnections()) {
			assertEquals(graph.connectionLength(c.from, c.to), g.connectionLength(c.from, c.to), DELTA);
		}
	}

	@Test
	public void unmodifiable2() {
		Point N = new Point(0, 5);
		Point E = new Point(5, 0);
		Point S = new Point(0, -5);
		Point W = new Point(-5, 0);

		Graphs.addBiPath(graph, N, E, S, W, N);
		Graph<LengthEdgeData> unmod = Graphs.unmodifiableGraph(graph);

		graph.addConnection(N, S);
		assertEquals(graph.getConnection(N, S), unmod.getConnection(N, S));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodAddConn() {
		Graphs.unmodifiableGraph(graph).addConnection(new Point(1, 2), new Point(2, 3));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodMerge() {
		Graphs.unmodifiableGraph(graph).merge(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodAddConns() {
		Graphs.unmodifiableGraph(graph).addConnections(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodRemoveNode() {
		Graphs.unmodifiableGraph(graph).removeNode(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodRemoveConnection() {
		Graphs.unmodifiableGraph(graph).removeConnection(null, null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodAddConnection() {
		Graphs.unmodifiableGraph(graph).addConnection(null, null, null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodAddConnection2() {
		Graphs.unmodifiableGraph(graph).addConnection(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodSetEdgeData() {
		Graphs.unmodifiableGraph(graph).setEdgeData(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addExistingConnection() {
		Point N = new Point(0, 5);
		Point E = new Point(5, 0);
		Graphs.addBiPath(graph, N, E);
		Graphs.addBiPath(graph, N, E);
	}

	@Test
	public void testMultimapGraphConstructor() {
		RandomGenerator rnd = new MersenneTwister(123);
		List<Point> path = new ArrayList<Point>();
		for (int i = 0; i < 20; i++) {
			path.add(new Point(rnd.nextInt(50), rnd.nextInt(50)));
		}
		Graphs.addBiPath(graph, path.toArray(new Point[path.size()]));

		MultimapGraph<LengthEdgeData> testGraph = new MultimapGraph<LengthEdgeData>();
		testGraph.merge(graph);

		MultimapGraph<LengthEdgeData> newGraph = new MultimapGraph<LengthEdgeData>(testGraph.getMultimap());

		assertEquals(testGraph.getMultimap(), newGraph.getMultimap());
	}

	@Test
	public void setEdgeData() {
		Point N = new Point(0, 5);
		Point E = new Point(5, 0);
		Point S = new Point(0, -5);
		Point W = new Point(-5, 0);

		Graphs.addBiPath(graph, N, E, S, W, N);
		assertNull(graph.setEdgeData(N, E, new LengthEdgeData(100)));
		assertEquals(new LengthEdgeData(100), graph.setEdgeData(N, E, null));
		if (graph instanceof TableGraph) {

		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void setNonExistingEdgeData() {
		graph.setEdgeData(new Point(1, 1), new Point(2, 3), null);
	}

	@Test
	public void removeNode() {
		Point N = new Point(0, 5);
		Point E = new Point(5, 0);
		Point S = new Point(0, -5);
		Point W = new Point(-5, 0);

		Graphs.addBiPath(graph, N, E, S, W, N);
		Graph<LengthEdgeData> unmod = Graphs.unmodifiableGraph(graph);
		assertEquals(graph, unmod);
		assertTrue(graph.getNodes().size() == 4);
		assertTrue(graph.getConnections().size() == 8);
		graph.removeNode(N);
		assertEquals(graph, unmod);
		assertTrue(graph.getNodes().size() == 3);
		assertTrue(graph.getConnections().size() == 4);
	}
}
