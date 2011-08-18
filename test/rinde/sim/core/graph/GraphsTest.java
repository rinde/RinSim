package rinde.sim.core.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
@RunWith(Parameterized.class)
public class GraphsTest {
	Graph graph;
	Class<? extends Graph> graphType;

	public GraphsTest(Class<? extends Graph> c) {
		graphType = c;
	}

	@Parameters
	public static Collection<Object[]> configs() {
		return Arrays.asList(new Object[][] { { MultimapGraph.class }, { TableGraph.class } });
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

		List<Entry<Point, Point>> connections = new ArrayList<Entry<Point, Point>>(graph.getConnections());
		for (int i = 1; i < points.size(); i++) {
			assertTrue(connections.get(i - 1).getKey() == points.get(i - 1));
			assertTrue(connections.get(i - 1).getValue() == points.get(i));
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
	public void equalsTest() {
		assertFalse(graph.equals(new Object()));
	}
}
