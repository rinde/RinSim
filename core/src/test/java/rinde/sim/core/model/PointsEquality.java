package rinde.sim.core.model;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.LengthEdgeData;
import rinde.sim.core.graph.TestMultimapGraph;
import rinde.sim.core.graph.TestTableGraph;
import rinde.sim.core.graph.Point;

import static org.junit.Assert.*;

/**
 * Test whether points are value objects
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 *
 */

@RunWith(Parameterized.class)
public class PointsEquality {

	
	@Parameters
	public static Collection<Object[]> configs() {
		return Arrays.asList(new Object[][] { { new TestMultimapGraph()}, { new TestTableGraph()} });
	}
	
	private Graph<LengthEdgeData> graph;
	
	public PointsEquality(Graph<LengthEdgeData> g) {
		this.graph = g;
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
		RoadModel rm = new RoadModel(new TestTableGraph());
		RoadModel.MidPoint p1 = rm.new MidPoint(0.2, 10000, null);
		RoadModel.MidPoint p2 = rm.new MidPoint(0.2, 10000, rm.new Location(p1));
		assertEquals(p1, p1);
		assertEquals(p2, p2);
		assertEquals(p1, p2);
		
		assertEquals(p1, Point.duplicate(p1));
		assertEquals(p1, Point.duplicate(p2));
	}
	
	
	@Test
	public void mixedPointsEqual() {
		RoadModel rm = new RoadModel(new TestTableGraph());
		Point p1 = new Point(5, 0.20101);
		RoadModel.MidPoint p2 = rm.new MidPoint(5, 0.20101, rm.new Location(new Point(0, 0)));
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
		
		//check if for other objects
		
		Point Aa, Bb;
		Aa = new Point(0,0);
		Bb = Point.duplicate(B);
		
		RoadModel rm = new RoadModel(new TestTableGraph());
		
		RoadModel.MidPoint Cc = rm.new MidPoint(13, 17, null);
		RoadModel.MidPoint Dd = rm.new MidPoint(17, 0, rm.new Location(A));
		
		checkAssertions(Aa, Bb, C, D);
		checkAssertions(A, B, Cc, Dd);
		checkAssertions(Aa, Bb, Cc, Dd);
	}
	
	private void checkAssertions(Point A, Point B, Point C, Point D) {
		//contain nodes
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
//		assertTrue(Double.NaN == Double.NaN); 
		assertTrue(Double.compare(Double.NaN, Double.NaN) == 0);
	}

}
