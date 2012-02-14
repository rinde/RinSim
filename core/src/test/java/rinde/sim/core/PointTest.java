/**
 * 
 */
package rinde.sim.core;

import static org.junit.Assert.assertEquals;

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

}
