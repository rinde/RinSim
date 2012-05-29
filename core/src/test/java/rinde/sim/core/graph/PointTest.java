/**
 * 
 */
package rinde.sim.core.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rinde.sim.core.graph.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class PointTest {

	final double EPSILON = 0.0000000001;

	@Test
	public void distanceTest() {
		Point p1 = new Point(0, 0);
		Point p2 = new Point(10, 0);
		Point p3 = new Point(10, 10);
		Point p4 = new Point(0, 10);

		assertEquals(10, Point.distance(p1, p2), EPSILON);
		assertEquals(14.14, Point.distance(p1, p3), 0.01);
		assertEquals(14.14, Point.distance(p3, p1), 0.01);
		assertEquals(10, Point.distance(p1, p4), EPSILON);

		assertEquals(14.14, Point.distance(p2, p4), 0.01);
		assertEquals(14.14, Point.distance(p4, p2), 0.01);
	}

	@Test
	public void pointFuncs() {
		assertEquals(new Point(0, 0), Point.diff(new Point(10, 0), new Point(10, 0)));
		assertEquals(new Point(234, 333.3), Point.duplicate(Point.parsePoint(new Point(234, 333.3).toString())));
		assertFalse(new Point(0, 0).equals(null));
		assertFalse(new Point(0, 0).equals(new Point(0, 1)));
		assertFalse(new Point(0, 0).equals(new Point(1, 0)));
		assertFalse(new Point(0, 0).equals(new Point(1, 1)));
		assertTrue(new Point(0, 0).equals(new Point(0, 0)));
		assertFalse(new Point(0, 0).equals(new Object()));
		assertFalse(new Point(0, 0).equals((Object) new Point(1, 0)));
	}

}
