package rinde.sim.core.model.road;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.ConnectionData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.LengthData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TestMultimapGraph;
import rinde.sim.core.graph.TestTableGraph;
import rinde.sim.core.model.road.GraphRoadModel; 

/**
 * Test whether points are value objects
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 */

@RunWith(Parameterized.class)
public class PointsEquality {

	@Parameters
	public static Collection<Object[]> configs() {
		return Arrays.asList(new Object[][] { { new TestMultimapGraph() }, { new TestTableGraph() } });
	}

	private final Graph<LengthData> graph;

	public PointsEquality(Graph<LengthData> g) {
		graph = g;
	}

	@Test
	public void pointsEqual() {
		Point p1 = new Point(0.1, 0.2);
		Point p2 = new Point(0.1, 0.2);
		assertEquals(p1, p1);
		assertEquals(p2, p2);
		assertEquals(p1, p2);

		assertEquals(p1, Point.duplicate(p1));
		assertEquals(p1, Point.duplicate(p2));
	}

	@Test
	public void midPointsEqual() {
		GraphRoadModel.Loc p1 = GraphRoadModel.newLoc(new Point(0.2, 10000));
		Connection<?> conn = new Connection<ConnectionData>(new Point(0.2, 10000), new Point(0.2, 10000), null);
		GraphRoadModel.Loc p2 = GraphRoadModel.newLoc(conn, 10);

		assertEquals(p1, p1);
		assertEquals(p2, p2);
		assertEquals(p1, p2);
		assertEquals(p1, Point.duplicate(p1));
		assertEquals(p1, Point.duplicate(p2));
	}

	@Test
	public void mixedPointsEqual() {
		Point p1 = new Point(5, 0.20101);
		GraphRoadModel.Loc p2 = GraphRoadModel.newLoc(new Point(5, 0.20101));

		assertEquals(p1, p1);
		assertEquals(p2, p2);
		assertEquals(p1, p2);

		assertEquals(p1, Point.duplicate(p1));
		assertEquals(p1, Point.duplicate(p2));
	}

	/**
	 * Check consistency wrt. handling value objects
	 */
	@Test
	public void graphDataConsistency() {
		Point A, B, C, D;
		A = new Point(0, 0);
		B = new Point(0, 13);
		C = new Point(13, 17);
		D = new Point(17, 0);

		graph.addConnection(A, B);
		graph.addConnection(B, C);
		graph.addConnection(B, D);

		checkAssertions(A, B, C, D);

		// check if for other objects
		Point Aa, Bb;
		Aa = new Point(0, 0);
		Bb = Point.duplicate(B);

		GraphRoadModel.Loc Cc = GraphRoadModel.newLoc(new Point(13, 17));
		GraphRoadModel.Loc Dd = GraphRoadModel.newLoc(graph.getConnection(B, D), 100);

		checkAssertions(Aa, Bb, C, D);
		checkAssertions(A, B, Cc, Dd);
		checkAssertions(Aa, Bb, Cc, Dd);
	}

	private void checkAssertions(Point A, Point B, Point C, Point D) {
		// contain nodes
		assertTrue("contains A", graph.containsNode(A));
		assertTrue("contains B", graph.containsNode(B));
		assertTrue("contains C", graph.containsNode(C));
		assertTrue("contains D", graph.containsNode(D));

		assertTrue("connection A->B", graph.hasConnection(A, B));
		assertTrue("connection B->C", graph.hasConnection(B, C));
		assertTrue("connection B->D", graph.hasConnection(B, D));

		assertFalse("!connection B->A", graph.hasConnection(B, A));
		assertFalse("!connection C->B", graph.hasConnection(A, C));
		assertFalse("!connection D->B", graph.hasConnection(A, D));
		assertFalse("!connection A->C", graph.hasConnection(A, C));
		assertFalse("!connection A->D", graph.hasConnection(A, D));
	}

	@Test
	public void testNaN() {
		// assertTrue(Double.NaN == Double.NaN);
		assertTrue(Double.compare(Double.NaN, Double.NaN) == 0);
	}

}
