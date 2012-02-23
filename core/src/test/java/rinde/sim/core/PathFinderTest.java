/**
 * 
 */
package rinde.sim.core;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.LengthEdgeData;
import rinde.sim.core.graph.TestMultimapGraph;
import rinde.sim.core.graph.TestTableGraph;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TableGraph;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;
import rinde.sim.util.TrivialRoadUser;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
@RunWith(Parameterized.class)
public class PathFinderTest {

	@Parameters
	public static Collection<Object[]> configs() {
		return Arrays.asList(new Object[][] { { TestMultimapGraph.class }, { TestTableGraph.class } });
	}

	public PathFinderTest(Class<? extends Graph<LengthEdgeData>> c) {
		rsType = c;
	}

	Class<? extends Graph<LengthEdgeData>> rsType;

	private final double EPSILON = 0.02;

	RoadModel rs;
	Point a, b, c, d, e, f, g;

	RoadUser o1, o2, o3, o4, o5, o6;

	@Before
	public void setUp() throws InstantiationException, IllegalAccessException {
		rs = new RoadModel(rsType.newInstance());

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

		o1 = new StringRoadUser("object1");
		o2 = new StringRoadUser("object2");
		o3 = new StringRoadUser("object3");
		o4 = new LongRoadUser(444L);
		o5 = new LongRoadUser(555L);
		o6 = new LongRoadUser(666L);

		rs.addObjectAt(o1, a);
		rs.addObjectAt(o2, b);
		rs.addObjectAt(o3, c);
		rs.addObjectAt(o4, d);
		rs.addObjectAt(o5, e);
		rs.addObjectAt(o6, f);

	}

	class StringRoadUser extends TrivialRoadUser {
		public final String name;

		public StringRoadUser(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	class LongRoadUser extends TrivialRoadUser {
		public final long number;

		public LongRoadUser(long number) {
			this.number = number;
		}

		@Override
		public String toString() {
			return "" + number;
		}
	}

	@Test
	public void shortestDistance() {
		List<Point> t = Graphs.shortestPathEuclidianDistance(rs.getGraph(), a, d);
		compatibilityCheck(t);
		assertEquals(asList(a, c, d), t);

		List<Point> t2 = Graphs.shortestPathEuclidianDistance(rs.getGraph(), d, a);
		compatibilityCheck(t2);
		assertEquals(asList(d, f, g, a), t2);

		List<Point> t3 = Graphs.shortestPathEuclidianDistance(rs.getGraph(), g, e);
		compatibilityCheck(t3);
		assertEquals(asList(g, a, c, e), t3);

		List<Point> t4 = Graphs.shortestPathEuclidianDistance(rs.getGraph(), a, e);
		compatibilityCheck(t4);
		assertEquals(asList(a, c, e), t4);

		List<Point> t5 = Graphs.shortestPathEuclidianDistance(rs.getGraph(), a, c);
		compatibilityCheck(t5);
		assertEquals(asList(a, c), t5);

		List<Point> t6 = Graphs.shortestPathEuclidianDistance(rs.getGraph(), e, g);
		compatibilityCheck(t6);
		assertEquals(asList(e, b, c, d, f, g), t6);
	}

	@Test(expected = RuntimeException.class)
	public void impossiblePath() throws InstantiationException, IllegalAccessException {
		RoadModel roads = new RoadModel(rsType.newInstance());
		roads.addConnection(a, b);
		roads.addConnection(b, c);

		Graphs.shortestPathEuclidianDistance(roads.getGraph(), b, a);
	}

	public void compatibilityCheck(List<Point> t) {
		RoadUser truck = new TrivialRoadUser();		
		rs.addObjectAt(truck, t.get(0));
		double len = length(t);
		double travelled = rs.followPath(truck, new LinkedList<Point>(t), len);
		assertEquals(len, travelled, EPSILON);

	}

	@Test
	public void checkRutgerBug() throws InstantiationException, IllegalAccessException {

		Graph graph = rsType.newInstance();

		Point q = new Point(0, 10);
		Point r = new Point(10, 15);
		Point s = new Point(10, 5);
		Point t = new Point(20, 10);

		graph.addConnection(q, r);
		graph.addConnection(q, s);
		graph.addConnection(s, t);

		//DotExporter.saveToDot(graph.getGraph(), "files/test/rutgerbug");

		// this shouldn't fail
		Graphs.shortestPathEuclidianDistance(graph, q, t);
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
		Collection<RoadUser> objects = Graphs.findObjectsWithinRadius(new Point(10, 10), rs, 15);
		//		System.out.println(objects);
	}
}
