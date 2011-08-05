/**
 * 
 */
package rinde.sim.core;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TableGraph;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
@RunWith(Parameterized.class)
public class RoadModelTest {

	final double EPSILON = 0.02;

	Class<? extends Graph> graphType;
	Class<? extends RoadModel> roadModelType;
	RoadModel rs;
	Queue<Point> path;
	Point p1, p2, p3, p4;

	public RoadModelTest(Class<? extends Graph> graphType, Class<? extends RoadModel> roadModelType) {
		this.graphType = graphType;
		this.roadModelType = roadModelType;
	}

	@Parameters
	public static Collection<Object[]> configs() {
		return Arrays.asList(new Object[][] { { MultimapGraph.class, RoadModel.class }, { MultimapGraph.class, CachedRoadModel.class }, { TableGraph.class, RoadModel.class },
				{ TableGraph.class, CachedRoadModel.class } });
	}

	@Before
	public void setUp() throws InstantiationException, IllegalAccessException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException {

		rs = roadModelType.getConstructor(Graph.class).newInstance((graphType.newInstance()));
		p1 = new Point(0, 0);
		p2 = new Point(10, 0);
		p3 = new Point(10, 10);
		p4 = new Point(0, 10);

		rs.addConnection(p1, p2);
		rs.addConnection(p2, p3);
		rs.addConnection(p3, p4);

		Set<Point> points = rs.getNodes();
		assertEquals(3, points.size());
		assertTrue(points.contains(p1));
		assertTrue(points.contains(p2));
		assertTrue(points.contains(p3));

		assertEquals(3, rs.getNumberOfConnections());
		assertEquals(3, rs.getNumberOfNodes());

		path = new LinkedList<Point>();
		path.addAll(asList(p1, p2, p3));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addConnection() {
		// connection already exists
		rs.addConnection(p1, p2);
	}

	@Test
	public void followTrajectory() {
		RoadUser agent = new TestRoadUser();
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
		RoadUser agent = new TestRoadUser();
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
		RoadUser agent = new TestRoadUser();
		rs.addObjectAt(agent, new Point(10, 10));
		assertTrue(rs.getPosition(agent).equals(new Point(10, 10)));

		assertTrue(path.size() == 3);
		// the trajectory does not directly connect to the current position
		rs.followPath(agent, path, 20);
	}

	@Test(expected = IllegalStateException.class)
	public void followTrajectory4() {
		RoadUser agent = new TestRoadUser();
		rs.addObjectAt(agent, new Point(0, 0));
		assertTrue(rs.getPosition(agent).equals(new Point(0, 0)));

		// illegal trajectory, the two points are not connected
		Queue<Point> traject = new LinkedList<Point>(Arrays.asList(new Point(0, 0), new Point(10, 10)));

		assertTrue(traject.size() == 2);
		rs.followPath(agent, traject, 20);
	}

	@Test(expected = IllegalStateException.class)
	public void followTrajectory5() {
		RoadUser agent = new TestRoadUser();
		rs.addObjectAt(agent, new Point(0, 0));
		assertTrue(rs.getPosition(agent).equals(new Point(0, 0)));

		// illegal trajectory, the second point is not a vertex
		Queue<Point> traject = new LinkedList<Point>(Arrays.asList(new Point(0, 0), new Point(10, 1)));

		assertTrue(traject.size() == 2);
		rs.followPath(agent, traject, 20);
	}

	@Test
	public void followTrajectory6() {
		RoadUser agent = new TestRoadUser();
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

		RoadUser agent1 = new TestRoadUser();
		rs.addObjectAt(agent1, p1);
		rs.followPath(agent1, new LinkedList<Point>(asList(p1, p2)), 5);
		assertEquals(new Point(5, 0), rs.getPosition(agent1));

		RoadUser agent2 = new TestRoadUser();
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

	@Test(expected = AssertionError.class)
	public void followPathFail1() {
		rs.followPath(null, null, 0);
	}

	@Test(expected = AssertionError.class)
	public void followPathFail2() {
		rs.followPath(null, new LinkedList<Point>(), 0);
	}

	@Test(expected = AssertionError.class)
	public void followPathFail3() {
		rs.followPath(null, new LinkedList<Point>(Arrays.asList(p1)), 0);
	}

	@Test(expected = AssertionError.class)
	public void followPathFail4() {
		rs.followPath(null, new LinkedList<Point>(Arrays.asList(p1)), 1);
	}

	@Test(expected = AssertionError.class)
	public void followPathFail5() {
		rs.followPath(new TestRoadUser(), new LinkedList<Point>(Arrays.asList(p1)), 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathFail6() {
		RoadUser agent = new TestRoadUser();
		rs.addObjectAt(agent, p1);
		rs.followPath(agent, path, 1);
		rs.followPath(agent, new LinkedList<Point>(Arrays.asList(p1, p2, p3)), 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathFail7() {
		RoadUser agent = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();

		rs.addObjectAt(agent2, p3);
		rs.addConnection(p3, p2);
		rs.followPath(agent2, new LinkedList<Point>(Arrays.asList(p3, p2)), 1);

		rs.addObjectAt(agent, p1);
		List<Point> curPath = rs.getShortestPathTo(agent, agent2);
		rs.followPath(agent, new LinkedList<Point>(curPath), 1);

		rs.followPath(agent, new LinkedList<Point>(Arrays.asList(p1, curPath.get(0))), 1);
	}

	@Test
	public void followPathOk() {
		RoadUser agent = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();

		rs.addObjectAt(agent2, p3);
		rs.addConnection(p3, p2);
		rs.followPath(agent2, new LinkedList<Point>(Arrays.asList(p3, p2)), 1);

		rs.addObjectAt(agent, p1);
		Queue<Point> curPath = new LinkedList<Point>(rs.getShortestPathTo(agent, agent2));
		System.out.println(curPath);
		rs.followPath(agent, curPath, 1);

		// this is to get more test coverage
		rs.followPath(agent, curPath, 19.5);
	}

	@Test(expected = AssertionError.class)
	public void getShortestPathToFail() {
		rs.getShortestPathTo(new TestRoadUser(), new Point(0, 0));
	}

	@Test(expected = AssertionError.class)
	public void getShortestPathToFail2() {
		rs.getShortestPathTo(new TestRoadUser(), new TestRoadUser());
	}

	@Test(expected = AssertionError.class)
	public void getPositionFail() {
		rs.getPosition(null);
	}

	@Test(expected = AssertionError.class)
	public void getPositionFail2() {
		rs.getPosition(new TestRoadUser());
	}

	@Test(expected = InvocationTargetException.class)
	public void constructorFail() throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		roadModelType.getConstructor(Graph.class).newInstance((Graph) null);
	}

	@Test
	public void testClear() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();
		RoadUser agent3 = new TestRoadUser();
		rs.addObjectAt(agent1, p1);
		rs.addObjectAt(agent2, p2);
		rs.addObjectAt(agent3, p3);
		assertEquals(3, rs.getObjects().size());
		rs.clear();
		assertTrue(rs.getObjects().isEmpty());
	}

	@Test
	public void testGetObjectsAndPositions() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();
		RoadUser agent3 = new RoadUser() {
		};
		rs.addObjectAt(agent1, p1);
		rs.addObjectAt(agent2, p2);
		rs.addObjectAt(agent3, p3);

		Map<RoadUser, Point> mapCopy = rs.getObjectsAndPositions();
		Set<RoadUser> setCopy = rs.getObjects();
		Set<TestRoadUser> subsetCopy = rs.getObjectsOfType(TestRoadUser.class);
		Collection<Point> posCopy = rs.getObjectPositions();

		assertEquals(3, rs.getObjectsAndPositions().size());
		assertEquals(3, mapCopy.size());
		assertEquals(3, setCopy.size());
		assertEquals(2, subsetCopy.size());
		assertEquals(3, posCopy.size());

		rs.removeObject(agent1);
		assertEquals(2, rs.getObjectsAndPositions().size());
		assertEquals(3, mapCopy.size());
		assertEquals(3, setCopy.size());
		assertEquals(2, subsetCopy.size());
		assertEquals(3, posCopy.size());
	}

	class TestRoadUser implements RoadUser {
	}

	@Test
	public void followPathHalfway2() {
		RoadUser agent1 = new TestRoadUser();
		rs.addObjectAt(agent1, p1);
		assertTrue(rs.containsObjectAt(agent1, p1));
		assertFalse(rs.containsObjectAt(agent1, p2));
		assertFalse(rs.containsObjectAt(null, null));
		assertTrue(rs.equalPosition(agent1, agent1));

		rs.followPath(agent1, new LinkedList<Point>(asList(p1, p2)), 5);
		assertEquals(new Point(5, 0), rs.getPosition(agent1));

		RoadUser agent2 = new TestRoadUser();
		rs.addObjectAtSamePosition(agent2, agent1);
		assertEquals(new Point(5, 0), rs.getPosition(agent2));
		assertTrue(rs.equalPosition(agent1, agent2));
		assertFalse(rs.equalPosition(null, agent1));
		assertFalse(rs.equalPosition(agent1, null));

		//		rs.followPath(agent2, new LinkedList<Point>(rs.getShortestPathTo(agent2, agent1)), 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addTruckTest() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();
		rs.addObjectAt(agent1, new Point(0, 0));
		rs.addObjectAt(agent2, new Point(1, 0));// this location is not a crossroad
	}

	@Test(expected = IllegalArgumentException.class)
	public void addTruckTest2() {
		RoadUser t = new TestRoadUser();
		rs.addObjectAt(t, new Point(0, 0));
		rs.addObjectAt(t, new Point(10, 0));// object is already added
	}

	@Test
	public void removeObjectTest() {
		RoadUser agent1 = new TestRoadUser();
		rs.addObjectAt(agent1, new Point(0, 0));
		rs.removeObject(agent1);
	}

	@Test(expected = AssertionError.class)
	public void removeObjectTestFail() {
		rs.removeObject(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addObjectAtSamePositionFail() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();
		rs.addObjectAt(agent1, p1);
		rs.addObjectAt(agent2, p2);
		rs.addObjectAtSamePosition(agent2, agent1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addObjectAtSamePositionFail2() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();
		RoadUser agent3 = new TestRoadUser();
		rs.addObjectAt(agent2, p2);
		rs.addObjectAtSamePosition(agent3, agent1);
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

	@Test
	public void cacheTest() {
		if (rs instanceof CachedRoadModel) {
			Table<Point, Point, List<Point>> cache = HashBasedTable.create();
			List<Point> cachePath = Arrays.asList(p1, p3);
			cache.put(p1, p3, cachePath);

			((CachedRoadModel) rs).setPathCache(cache);

			List<Point> shortPath = rs.getShortestPathTo(p1, p3);

			assertEquals(shortPath, cachePath);

			assertEquals(cache, ((CachedRoadModel) rs).getPathCache());
		}
	}
}