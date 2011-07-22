/**
 * 
 */
package rinde.sim.core;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class PathFinderTest {

	private final double EPSILON = 0.02;

	RoadStructure rs;
	Point a, b, c, d, e, f, g;

	String o1, o2, o3;
	Long o4, o5, o6;

	@Before
	public void setUp() {
		rs = new RoadStructure();
		a = new Point(0, 0);
		b = new Point(10, 0);
		c = new Point(15, 15);
		d = new Point(15, 20);
		e = new Point(22, 0);
		f = new Point(8, 20);
		g = new Point(0, 12);

		rs.addConnection(a, b);
		rs.addConnection(a, c);
		//rs.addConnection(a, d);

		rs.addConnection(b, a);
		rs.addConnection(b, c);

		rs.addConnection(c, d);
		rs.addConnection(c, e);

		rs.addConnection(d, c);
		rs.addConnection(d, f);

		rs.addConnection(e, b);

		rs.addConnection(f, g);

		rs.addConnection(g, a);

		o1 = "object1";
		o2 = "object2";
		o3 = "object3";
		o4 = new Long(444);
		o5 = new Long(555);
		o6 = new Long(666);

		rs.addObjectAt(o1, a);
		rs.addObjectAt(o2, b);
		rs.addObjectAt(o3, c);
		rs.addObjectAt(o4, d);
		rs.addObjectAt(o5, e);
		rs.addObjectAt(o6, f);

	}

	@Test
	public void shortestDistance() {
		List<Point> t = PathFinder.shortestDistance(rs.getGraph(), a, d);
		compatibilityCheck(t);
		assertEquals(asList(a, c, d), t);

		List<Point> t2 = PathFinder.shortestDistance(rs.getGraph(), d, a);
		compatibilityCheck(t2);
		assertEquals(asList(d, f, g, a), t2);

		List<Point> t3 = PathFinder.shortestDistance(rs.getGraph(), g, e);
		compatibilityCheck(t3);
		assertEquals(asList(g, a, c, e), t3);

		List<Point> t4 = PathFinder.shortestDistance(rs.getGraph(), a, e);
		compatibilityCheck(t4);
		assertEquals(asList(a, c, e), t4);

		List<Point> t5 = PathFinder.shortestDistance(rs.getGraph(), a, c);
		compatibilityCheck(t5);
		assertEquals(asList(a, c), t5);

		List<Point> t6 = PathFinder.shortestDistance(rs.getGraph(), e, g);
		compatibilityCheck(t6);
		assertEquals(asList(e, b, c, d, f, g), t6);
	}

	@Test(expected = RuntimeException.class)
	public void impossiblePath() {
		RoadStructure roads = new RoadStructure();
		roads.addConnection(a, b);
		roads.addConnection(b, c);

		PathFinder.shortestDistance(roads.getGraph(), b, a);
	}

	public void compatibilityCheck(List<Point> t) {
		Object truck = new Object();
		rs.addObjectAt(truck, t.get(0));
		double len = length(t);
		double travelled = rs.followPath(truck, new LinkedList<Point>(t), len);
		assertEquals(len, travelled, EPSILON);

	}

	@Test
	public void checkRutgerBug() {

		RoadStructure graph = new RoadStructure();

		Point q = new Point(0, 10);
		Point r = new Point(10, 15);
		Point s = new Point(10, 5);
		Point t = new Point(20, 10);

		graph.addConnection(q, r);
		graph.addConnection(q, s);
		graph.addConnection(s, t);

		//DotExporter.saveToDot(graph.getGraph(), "files/test/rutgerbug");

		// this shouldn't fail
		PathFinder.shortestDistance(graph.getGraph(), q, t);
	}

	private double length(List<Point> path) {
		double length = 0;
		for (int i = 1; i < path.size(); i++) {
			length += Point.distance(path.get(i - 1), path.get(i));
		}
		return length;
	}

	@Test
	public void findObjectsWithinRadius() {
		Collection<Object> objects = PathFinder.findObjectsWithinRadius(new Point(10, 10), rs, 15);
		System.out.println(objects);
	}
}
