/**
 * 
 */
package rinde.sim.core;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class RoadStructureTest {

	final double EPSILON = 0.02;

	RoadStructure rs;
	Queue<Point> path;
	Point p1, p2, p3, p4;

	@Before
	public void setUp() {
		rs = new RoadStructure();
		p1 = new Point(0, 0);
		p2 = new Point(10, 0);
		p3 = new Point(10, 10);
		p4 = new Point(0, 10);

		rs.addConnection(p1, p2);
		rs.addConnection(p2, p3);
		rs.addConnection(p3, p4);

		path = new LinkedList<Point>();
		path.addAll(asList(new Point(0, 0), new Point(10, 0), new Point(10, 10)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addConnection() {
		// connection already exists
		rs.addConnection(new Point(0, 0), new Point(10, 0));
	}

	@Test
	public void followTrajectory() {
		Object agent = new Object();
		rs.addObjectAt(agent, new Point(0, 0));
		assertTrue(rs.getPosition(agent).equals(new Point(0, 0)));

		assertTrue(path.size() == 3);
		double travelled = rs.followPath(agent, path, 5);
		assertTrue(travelled == 5);
		assertTrue(path.size() == 2);
		assertTrue(rs.getPosition(agent).equals(new Point(5, 0)));

		travelled = rs.followPath(agent, path, 10);
		assertEquals(10, travelled, EPSILON);
		assertTrue(path.size() == 1);
		assertTrue(rs.getPosition(agent).equals(new Point(10, 5)));

		travelled = rs.followPath(agent, path, 1);
		assertEquals(1, travelled, EPSILON);
		assertTrue(path.size() == 1);
		assertTrue(rs.getPosition(agent).equals(new Point(10, 6)));

		travelled = rs.followPath(agent, path, 2);
		assertEquals(2, travelled, EPSILON);
		assertTrue(path.size() == 1);
		assertTrue(rs.getPosition(agent).equals(new Point(10, 8)));

		travelled = rs.followPath(agent, path, 3);
		assertTrue(travelled == 2);
		assertEquals(2, travelled, EPSILON);
		assertTrue(path.size() == 0);
		assertTrue(rs.getPosition(agent).equals(new Point(10, 10)));
	}

	@Test
	public void followTrajectory2() {
		Object agent = new Object();
		rs.addObjectAt(agent, new Point(0, 0));
		assertTrue(rs.getPosition(agent).equals(new Point(0, 0)));

		assertTrue(path.size() == 3);
		double travelled = rs.followPath(agent, path, 20);
		assertTrue(travelled == 20);
		assertTrue(path.size() == 0);
		assertTrue(rs.getPosition(agent).equals(new Point(10, 10)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void followTrajectory3() {
		Object agent = new Object();
		rs.addObjectAt(agent, new Point(10, 10));
		assertTrue(rs.getPosition(agent).equals(new Point(10, 10)));

		assertTrue(path.size() == 3);
		// the trajectory does not directly connect to the current position
		rs.followPath(agent, path, 20);
	}

	@Test(expected = IllegalStateException.class)
	public void followTrajectory4() {
		Object agent = new Object();
		rs.addObjectAt(agent, new Point(0, 0));
		assertTrue(rs.getPosition(agent).equals(new Point(0, 0)));

		// illegal trajectory, the two points are not connected
		Queue<Point> traject = new LinkedList<Point>(Arrays.asList(new Point(0, 0), new Point(10, 10)));

		assertTrue(traject.size() == 2);
		rs.followPath(agent, traject, 20);
	}

	@Test(expected = IllegalStateException.class)
	public void followTrajectory5() {
		Object agent = new Object();
		rs.addObjectAt(agent, new Point(0, 0));
		assertTrue(rs.getPosition(agent).equals(new Point(0, 0)));

		// illegal trajectory, the second point is not a vertex
		Queue<Point> traject = new LinkedList<Point>(Arrays.asList(new Point(0, 0), new Point(10, 1)));

		assertTrue(traject.size() == 2);
		rs.followPath(agent, traject, 20);
	}

	@Test
	public void followTrajectory6() {
		Object agent = new Object();
		rs.addObjectAt(agent, new Point(0, 0));
		assertTrue(rs.getPosition(agent).equals(new Point(0, 0)));

		double travelled = rs.followPath(agent, path, 10);
		assertEquals(10d, travelled, EPSILON);
		assertTrue(path.size() == 1);

		travelled = rs.followPath(agent, path, 1);
		assertEquals(1d, travelled, EPSILON);
		assertTrue(path.size() == 1);
		assertEquals(new Point(10, 1), rs.getPosition(agent));
	}

	@Test
	public void followPathHalfway1() {
		rs.addConnection(p2, p1);
		rs.addConnection(p3, p2);

		Object agent1 = new Object();
		rs.addObjectAt(agent1, p1);
		rs.followPath(agent1, new LinkedList<Point>(asList(p1, p2)), 5);
		assertEquals(new Point(5, 0), rs.getPosition(agent1));

		Object agent2 = new Object();
		rs.addObjectAt(agent2, p1);
		assertEquals(new Point(0, 0), rs.getPosition(agent2));

		Queue<Point> path1 = new LinkedList<Point>(rs.getShortestPathTo(agent2, agent1));
		assertEquals(asList(p1, new Point(5, 0)), path1);

		rs.followPath(agent2, path1, 10);
		assertEquals(new Point(5, 0), rs.getPosition(agent2));

		Queue<Point> path2 = new LinkedList<Point>(rs.getShortestPathTo(agent2, p3));
		assertEquals(asList(p2, p3), path2);
		rs.followPath(agent2, path2, 10);
		assertEquals(new Point(10, 5), rs.getPosition(agent2));

		// coming from the front side, we have to turn around at p1
		Queue<Point> path3 = new LinkedList<Point>(rs.getShortestPathTo(agent2, agent1));
		assertEquals(asList(p3, p2, p1, new Point(5, 0)), path3);
		rs.followPath(agent2, path3, 100);

		assertEquals(new Point(5, 0), rs.getPosition(agent1));
		assertEquals(new Point(5, 0), rs.getPosition(agent2));

		rs.addConnection(p1, p4);
		rs.addConnection(p4, p1);
		rs.followPath(agent2, new LinkedList<Point>(asList(p2, p1, p4)), 25);
		assertEquals(new Point(0, 10), rs.getPosition(agent2));

		// coming from the back side, no turning around is required
		Queue<Point> path4 = new LinkedList<Point>(rs.getShortestPathTo(agent2, agent1));
		assertEquals(asList(p4, p1, new Point(5, 0)), path4);
		assertEquals(10, rs.followPath(agent2, path4, 10), EPSILON);
		assertEquals(new Point(0, 0), rs.getPosition(agent2));
		assertEquals(5, rs.followPath(agent2, path4, 20), EPSILON);
		assertEquals(new Point(5, 0), rs.getPosition(agent2));
	}

	@Test
	public void followPathHalfway2() {
		Object agent1 = new Object();
		rs.addObjectAt(agent1, p1);
		rs.followPath(agent1, new LinkedList<Point>(asList(p1, p2)), 5);
		assertEquals(new Point(5, 0), rs.getPosition(agent1));

		Object agent2 = new Object();
		rs.addObjectAtSamePosition(agent2, agent1);
		assertEquals(new Point(5, 0), rs.getPosition(agent2));
		assertTrue(rs.equalPosition(agent1, agent2));

		//		rs.followPath(agent2, new LinkedList<Point>(rs.getShortestPathTo(agent2, agent1)), 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addTruckTest() {
		Object t = new Object();
		rs.addObjectAt(t, new Point(0, 0));
		rs.addObjectAt(new Object(), new Point(1, 0));// this location is not a crossroad
	}

	@Test(expected = IllegalArgumentException.class)
	public void addTruckTest2() {
		Object t = new Object();
		rs.addObjectAt(t, new Point(0, 0));
		rs.addObjectAt(t, new Point(10, 0));// object is already added
	}

	@BeforeClass
	public static void assertionCheck() {
		boolean assertionsAreOn = false;
		try {
			assert false;
		} catch (AssertionError ae) {
			assertionsAreOn = true;
		}
		assertTrue(assertionsAreOn);
	}
}