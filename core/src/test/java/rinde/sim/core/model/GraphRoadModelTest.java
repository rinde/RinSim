/**
 * 
 */
package rinde.sim.core.model;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.LengthEdgeData;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TestMultimapGraph;
import rinde.sim.core.graph.TestTableGraph;
import rinde.sim.core.model.AbstractRoadModel.PathProgress;
import rinde.sim.util.TimeUnit;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
@RunWith(Parameterized.class)
public class GraphRoadModelTest extends AbstractRoadModelTest<GraphRoadModel> {

	protected Class<? extends Graph<?>> graphType;
	protected Graph<? extends EdgeData> graph;
	protected Class<? extends GraphRoadModel> roadModelType;

	// TODO what about negative speeds? and what about negative speed limits?

	static boolean connectionEquals(Connection<? extends EdgeData> conn, Point from, Point to) {
		return conn.from.equals(from) && conn.to.equals(to);
	}

	public GraphRoadModelTest(Class<? extends Graph<?>> pGraphType, Class<? extends GraphRoadModel> pRoadModelType) {
		graphType = pGraphType;
		roadModelType = pRoadModelType;
	}

	@Parameters
	public static Collection<Object[]> configs() {
		return Arrays.asList(new Object[][] { { TestMultimapGraph.class, GraphRoadModel.class },
				{ TestMultimapGraph.class, CachedRoadModel.class }, { TestTableGraph.class, GraphRoadModel.class },
				{ TestTableGraph.class, CachedRoadModel.class } });
	}

	@Before
	public void setUp() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			SecurityException, InvocationTargetException, NoSuchMethodException {

		graph = (graphType.newInstance());
		model = roadModelType.getConstructor(Graph.class).newInstance(graph);

		SW = new Point(0, 0);
		SE = new Point(10, 0);
		NE = new Point(10, 10);
		NW = new Point(0, 10);

		graph.addConnection(SW, SE);
		graph.addConnection(SE, NE);
		graph.addConnection(NE, NW);

		Set<Point> points = graph.getNodes();
		assertEquals(4, points.size());
		assertTrue(points.contains(SW));
		assertTrue(points.contains(SE));
		assertTrue(points.contains(NE));

		assertEquals(3, graph.getNumberOfConnections());
		assertEquals(4, graph.getNumberOfNodes());
	}

	@Test(expected = InvocationTargetException.class)
	public void constructorFail() throws IllegalArgumentException, SecurityException, InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		roadModelType.getConstructor(Graph.class).newInstance((Graph<?>) null);
	}

	/**
	 * Follow path
	 */
	@Test
	public void followPathAllAtOnce() {
		SpeedyRoadUser agent = new SpeedyRoadUser(sc.from(5, TimeUnit.MS).to(TimeUnit.H));
		model.addObjectAt(agent, SW);
		assertEquals(SW, model.getPosition(agent));

		Queue<Point> path = asPath(SW, SE, NE);
		PathProgress travelled = model.followPath(agent, path, 5);
		assertEquals(20, travelled.distance, EPSILON);
		assertEquals(4, travelled.time);
		assertEquals(0, path.size());
		assertEquals(NE, model.getPosition(agent));
	}

	@Test
	public void followLongPath() {
		Point A = new Point(10, 20);
		Point B = new Point(10, 22);
		Point C = new Point(0, 15);
		Graphs.addPath(graph, NE, A, B, C);

		TestRoadUser ab = new TestRoadUser();
		model.addObjectAt(ab, A);
		model.followPath(ab, asPath(B), ONE_HOUR);
		assertEquals(A, model.getConnection(ab).from);
		assertEquals(B, model.getConnection(ab).to);

		TestRoadUser bc = new TestRoadUser();
		model.addObjectAt(bc, B);
		model.followPath(bc, asPath(C), ONE_HOUR);
		assertEquals(B, model.getConnection(bc).from);
		assertEquals(C, model.getConnection(bc).to);

		Queue<Point> path = asPath(SW, SE, NE, A, model.getPosition(ab), B, model.getPosition(bc), C);
		assertEquals(8, path.size());

		TestRoadUser driver1 = new TestRoadUser();
		model.addObjectAt(driver1, SW);
		PathProgress pp = model.followPath(driver1, path, 44 * ONE_HOUR);
		assertEquals(44 * ONE_HOUR, pp.time);
		assertEquals(44, pp.distance, EPSILON);
		assertEquals(asList(SW, SE, NE, A, B), pp.travelledNodes);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathToMiddleAndBackFail() {
		TestRoadUser middle = new TestRoadUser();
		model.addObjectAt(middle, SW);
		model.followPath(middle, asPath(SE), ONE_HOUR);
		assertTrue(connectionEquals(model.getConnection(middle), SW, SE));

		TestRoadUser driver = new TestRoadUser();
		model.addObjectAt(driver, SW);
		model.followPath(driver, asPath(model.getPosition(middle), SW), 10 * ONE_HOUR);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathUnconnected() {
		MovingRoadUser agent = new SpeedyRoadUser(100);
		model.addObjectAt(agent, new Point(0, 0));
		assertEquals(new Point(0, 0), model.getPosition(agent));

		// illegal trajectory, the two points are not connected
		Queue<Point> traject = new LinkedList<Point>(Arrays.asList(new Point(0, 0), new Point(10, 10)));

		assertEquals(2, traject.size());
		model.followPath(agent, traject, 20);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathNotAvertex() {
		MovingRoadUser agent = new SpeedyRoadUser(EPSILON);
		model.addObjectAt(agent, new Point(0, 0));
		assertEquals(new Point(0, 0), model.getPosition(agent));

		// illegal trajectory, the second point is not a vertex
		Queue<Point> traject = new LinkedList<Point>(Arrays.asList(new Point(0, 0), new Point(10, 1)));

		assertEquals(2, traject.size());
		model.followPath(agent, traject, 20);
	}

	@Test
	public void followPathNotTillEnd() {
		MovingRoadUser agent = new SpeedyRoadUser(sc.from(1, TimeUnit.MS).to(TimeUnit.H));
		model.addObjectAt(agent, new Point(0, 0));
		assertEquals(new Point(0, 0), model.getPosition(agent));

		Queue<Point> path = asPath(SW, SE, NE);
		PathProgress travelled = model.followPath(agent, path, 10);
		assertEquals(10d, travelled.distance, EPSILON);
		assertEquals(1, path.size());

		travelled = model.followPath(agent, path, 1);
		assertEquals(1d, travelled.distance, EPSILON);
		assertEquals(1, path.size());
		assertEquals(new Point(10, 1), model.getPosition(agent));
	}

	/**
	 * Simplest check for time based following path
	 * {@link RoadModel#followPath(MovingRoadUser, Queue, long)}
	 */
	@Test
	public void followPathTime() {
		Queue<Point> path = asPath(SW, SE, NE);
		assertEquals(3, path.size());

		MovingRoadUser agent = new SpeedyRoadUser(sc.from(5, TimeUnit.MS).to(TimeUnit.H));
		model.addObjectAt(agent, new Point(0, 0));
		assertTrue(model.getPosition(agent).equals(new Point(0, 0)));
		assertEquals(3, path.size());

		PathProgress progress = model.followPath(agent, path, 1);
		assertEquals(5d, progress.distance, EPSILON);
		assertEquals(2, path.size());
		assertEquals(new Point(5, 0), model.getPosition(agent));

		progress = model.followPath(agent, path, 2); // follow path for 2 x time
		assertEquals(10, progress.distance, EPSILON);
		assertEquals(1, path.size());
		assertEquals(new Point(10, 5), model.getPosition(agent));

		progress = model.followPath(agent, path, 3); // follow path for 3 x time
														// == 15
		assertEquals(5, progress.distance, EPSILON);
		assertEquals(1, progress.time);
		assertEquals(0, path.size());
		assertEquals(new Point(10, 10), model.getPosition(agent));
	}

	@Test
	public void followPathHalfway1() {
		graph.addConnection(SE, SW);
		graph.addConnection(NE, SE);

		MovingRoadUser agent1 = new TestRoadUser();
		model.addObjectAt(agent1, SW);
		model.followPath(agent1, new LinkedList<Point>(asList(SW, SE)), TimeUnit.H.toMs(5));
		assertEquals(new Point(5, 0), model.getPosition(agent1));

		MovingRoadUser agent2 = new TestRoadUser();
		model.addObjectAt(agent2, SW);
		assertEquals(new Point(0, 0), model.getPosition(agent2));

		Queue<Point> path1 = new LinkedList<Point>(model.getShortestPathTo(agent2, agent1));
		assertEquals(asList(SW, new Point(5, 0)), path1);

		model.followPath(agent2, path1, TimeUnit.H.toMs(10));
		assertEquals(new Point(5, 0), model.getPosition(agent2));

		Queue<Point> path2 = new LinkedList<Point>(model.getShortestPathTo(agent2, NE));
		assertEquals(asList(new Point(5, 0), SE, NE), path2);
		model.followPath(agent2, path2, TimeUnit.H.toMs(10));
		assertEquals(new Point(10, 5), model.getPosition(agent2));
		assertTrue(connectionEquals(model.getConnection(agent2), SE, NE));

		// coming from the front side, we have to turn around at p1
		Queue<Point> path3 = new LinkedList<Point>(model.getShortestPathTo(agent2, agent1));
		assertEquals(asList(new Point(10, 5), NE, SE, SW, new Point(5, 0)), path3);
		model.followPath(agent2, path3, TimeUnit.H.toMs(100));

		assertEquals(new Point(5, 0), model.getPosition(agent1));
		assertEquals(new Point(5, 0), model.getPosition(agent2));

		graph.addConnection(SW, NW);
		graph.addConnection(NW, SW);
		model.followPath(agent2, new LinkedList<Point>(asList(SE, SW, NW)), TimeUnit.H.toMs(25));
		assertEquals(new Point(0, 10), model.getPosition(agent2));

		// coming from the back side, no turning around is required
		Queue<Point> path4 = new LinkedList<Point>(model.getShortestPathTo(agent2, agent1));
		assertEquals(asList(NW, SW, new Point(5, 0)), path4);
		assertEquals(10, model.followPath(agent2, path4, TimeUnit.H.toMs(10)).distance, EPSILON);
		assertEquals(new Point(0, 0), model.getPosition(agent2));
		assertEquals(5, model.followPath(agent2, path4, TimeUnit.H.toMs(20)).distance, EPSILON);
		assertEquals(new Point(5, 0), model.getPosition(agent2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathFailObject1() {
		model.followPath(null, null, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathFailObject2() {
		model.followPath(new TestRoadUser(), null, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathFailPath1() {
		TestRoadUser testRoadUser = new TestRoadUser();
		model.addObjectAt(testRoadUser, SW);
		model.followPath(testRoadUser, null, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathFailPath2() {
		TestRoadUser testRoadUser = new TestRoadUser();
		model.addObjectAt(testRoadUser, SW);
		model.followPath(testRoadUser, new LinkedList<Point>(), 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathFailTime() {
		TestRoadUser testRoadUser = new TestRoadUser();
		model.addObjectAt(testRoadUser, SW);
		model.followPath(testRoadUser, new LinkedList<Point>(Arrays.asList(SW)), 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathWrongFirstPoint() {
		MovingRoadUser agent = new SpeedyRoadUser(3);
		model.addObjectAt(agent, new Point(10, 10));

		Queue<Point> path = asPath(SW, SE, NE);
		assertEquals(new Point(10, 10), model.getPosition(agent));
		assertEquals(3, path.size());
		// the path does not directly connect to the current position
		model.followPath(agent, path, 1);
	}

	/**
	 * Situation: <code>SW ->- tru1 ->- SE </code><br/>
	 * tru1 wants to move directly to SW, which should throw an exception since
	 * moving backward over an directed edge is not allowed.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void followPathFailIllegalPath1() {
		TestRoadUser testRoadUser = new TestRoadUser();
		model.addObjectAt(testRoadUser, SW);
		Queue<Point> p = new LinkedList<Point>(Arrays.asList(SW, SE));
		PathProgress progress = model.followPath(testRoadUser, p, 3600000);
		assertEquals(1.0, progress.distance, EPSILON);
		assertEquals(asList(SE), p);
		assertEquals(3600000, progress.time);
		assertEquals(asList(SW), progress.travelledNodes);
		model.followPath(testRoadUser, asPath(SW), 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathFailIllegalPath2() {
		TestRoadUser testRoadUser1 = new TestRoadUser();
		model.addObjectAt(testRoadUser1, SW);

		graph.addConnection(SE, SW);

		model.followPath(testRoadUser1, asPath(SW, SE, SW), 11 * ONE_HOUR);

		TestRoadUser testRoadUser2 = new TestRoadUser();
		model.addObjectAt(testRoadUser2, SW);
		model.followPath(testRoadUser2, asPath(SW, SE), 2 * ONE_HOUR);
		model.followPath(testRoadUser2, asPath(model.getPosition(testRoadUser1)), 2 * ONE_HOUR);
	}

	/**
	 * Situation: <code>SW(tru2) ->- SE ->- tru1 ->- NE </code><br/>
	 * tru2 moves towards tru1, it should end up between SE and tru1.
	 */
	@Test
	public void followPathMoveTowardOther() {
		TestRoadUser tru1 = new TestRoadUser();
		model.addObjectAt(tru1, SW);
		model.followPath(tru1, asPath(SW, SE, NE), 12 * ONE_HOUR);

		TestRoadUser tru2 = new TestRoadUser();
		model.addObjectAt(tru2, SW);
		PathProgress pp = model.followPath(tru2, asPath(SW, SE, model.getPosition(tru1)), 11 * ONE_HOUR);
		assertEquals(11, pp.distance, EPSILON);

		Point p1 = model.getPosition(tru1);
		Point p2 = model.getPosition(tru2);

		Point diff = Point.diff(p1, p2);
		assertTrue(diff.x == 0 && diff.y > 0);
	}

	/**
	 * Situation: <code>SW ->- tru1 ->- SE </code><br/>
	 * tru1 wants to move directly to SW, which should throw an exception since
	 * moving backward over an directed edge is not allowed.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void followPathFailGoBackwardToNode() {
		TestRoadUser testRoadUser1 = new TestRoadUser();
		model.addObjectAt(testRoadUser1, SW);
		model.followPath(testRoadUser1, asPath(SW, SE), ONE_HOUR);
		model.followPath(testRoadUser1, asPath(SW), ONE_HOUR);
	}

	/**
	 * Situation: <code>SW ->- tru1 ->- tru2 ->- SE </code><br/>
	 * tru2 wants to move directly to tru1, which should throw an exception
	 * since moving backward over an directed edge is not allowed.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void followPathFailGoBackwardToEdgePoint() {
		// first move tru1 and tru2 in position
		TestRoadUser tru1 = new TestRoadUser();
		model.addObjectAt(tru1, SW);
		model.followPath(tru1, asPath(SW, SE), ONE_HOUR);

		TestRoadUser tru2 = new TestRoadUser();
		model.addObjectAt(tru2, SW);
		model.followPath(tru2, asPath(SW, SE), 2 * ONE_HOUR);

		// now the road users are in the above described positions
		Point difference = Point.diff(model.getPosition(tru1), model.getPosition(tru2));
		assertTrue(difference.x < 0 && difference.y == 0);

		model.followPath(tru2, asPath(model.getPosition(tru1)), 2 * ONE_HOUR);
	}

	/**
	 * Situation: <code>SW ->- tru1 ->- SE -<- tru2 -<- NE </code><br/>
	 * tru2 wants to move directly to tru1, which should throw an exception
	 * since they are on different edges.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void followPathFailWrongEdge() {
		graph.addConnection(NE, SE);

		// first move tru1 and tru2 in position
		TestRoadUser tru1 = new TestRoadUser();
		model.addObjectAt(tru1, SW);
		model.followPath(tru1, asPath(SW, SE), ONE_HOUR);

		TestRoadUser tru2 = new TestRoadUser();
		model.addObjectAt(tru2, NE);
		model.followPath(tru2, asPath(NE, SE), 2 * ONE_HOUR);

		model.followPath(tru2, asPath(model.getPosition(tru1)), 2 * ONE_HOUR);
	}

	/**
	 * Situation: <code>SW(tru1) ->- SE -<- tru2 -<- NE </code><br/>
	 * tru2 wants to move directly to tru1, which should throw an exception
	 * since they are on different edges.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void followPathFailWrongEdge2() {
		graph.addConnection(NE, SE);

		// first move tru1 and tru2 in position
		TestRoadUser tru1 = new TestRoadUser();
		model.addObjectAt(tru1, SW);

		TestRoadUser tru2 = new TestRoadUser();
		model.addObjectAt(tru2, NE);
		model.followPath(tru2, asPath(NE, SE), 2 * ONE_HOUR);

		model.followPath(tru1, asPath(model.getPosition(tru2)), 2 * ONE_HOUR);
	}

	/**
	 * Situation: <code>SW(tru1) ->- SE -<- tru2 -<- NE </code><br/>
	 * tru2 wants to move directly to tru1, which should throw an exception
	 * since they are on different edges.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void followPathFailWrongEdgeIndirectly() {
		graph.addConnection(NE, SE);

		// first move tru1 and tru2 in position
		TestRoadUser tru1 = new TestRoadUser();
		model.addObjectAt(tru1, SW);

		TestRoadUser tru2 = new TestRoadUser();
		model.addObjectAt(tru2, NE);
		model.followPath(tru2, asPath(NE, SE), 2 * ONE_HOUR);

		model.followPath(tru1, asPath(SW, model.getPosition(tru2)), 200 * ONE_HOUR);
	}

	/**
	 * Situation: <code>SW ->- tru1 ->- tru2 ->- SE</code><br/>
	 * tru1 wants to move directly to tru2, which should be allowed.
	 */
	@Test
	public void followPathChase() {
		// first move tru1 and tru2 in position
		TestRoadUser tru1 = new TestRoadUser();
		model.addObjectAt(tru1, SW);
		model.followPath(tru1, asPath(SW, SE), ONE_HOUR);

		TestRoadUser tru2 = new TestRoadUser();
		model.addObjectAt(tru2, SW);
		model.followPath(tru2, asPath(SW, SE), 2 * ONE_HOUR);

		model.followPath(tru1, asPath(model.getPosition(tru2)), 2 * ONE_HOUR);
		assertEquals(model.getPosition(tru1), model.getPosition(tru2));
	}

	/**
	 * Situation: <code>SW(tru1) ->- tru2 ->- SE</code><br/>
	 * tru1 wants to move directly to tru2, which should be allowed.
	 */
	@Test
	public void followPathChaseFromNode() {
		// first move tru1 and tru2 in position
		TestRoadUser tru1 = new TestRoadUser();
		model.addObjectAt(tru1, SW);

		TestRoadUser tru2 = new TestRoadUser();
		model.addObjectAt(tru2, SW);
		model.followPath(tru2, asPath(SW, SE), 2 * ONE_HOUR);

		model.followPath(tru1, asPath(model.getPosition(tru2)), 3 * ONE_HOUR);
		assertEquals(model.getPosition(tru1), model.getPosition(tru2));
	}

	@Test
	public void getConnectionTest() {
		TestRoadUser driver = new TestRoadUser();
		model.addObjectAt(driver, SE);
		assertNull(model.getConnection(driver));
		model.followPath(driver, asPath(NE), ONE_HOUR);
		assertTrue(connectionEquals(model.getConnection(driver), SE, NE));
	}

	@Test
	public void getShortestPath() {
		TestRoadUser driver = new TestRoadUser();
		model.addObjectAt(driver, SW);
		model.followPath(driver, asPath(SE), ONE_HOUR);
		assertTrue(connectionEquals(model.getConnection(driver), SW, SE));

		List<Point> path = model.getShortestPathTo(driver, NE);
		assertEquals(asList(model.getPosition(driver), SE, NE), path);

	}

	@Test(expected = IllegalArgumentException.class)
	public void getShortestPathToFail() {
		model.getShortestPathTo(new TestRoadUser(), new Point(0, 0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getShortestPathToFail2() {
		model.getShortestPathTo(new TestRoadUser(), new TestRoadUser());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getShortestPathToFail3() {
		model.getShortestPathTo((RoadUser) null, new Point(0, 0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getShortestPathToFail4() {
		model.getShortestPathTo((Point) null, new Point(0, 0));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getShortestPathToFail5() {
		model.getShortestPathTo(new Point(1, 2), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getShortestPathToFail6() {
		model.getShortestPathTo((RoadUser) null, (RoadUser) null);
	}

	@Test(expected = NullPointerException.class)
	public void locationConstructorFail() {
		GraphRoadModel.newLoc(null);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void locationIsOnSameEdge() {
		// FIXME
		throw new NotImplementedException("todo");
		// ((Graph<MultiAttributeEdgeData>) graph).addConnection(SE, SW, new
		// MultiAttributeEdgeData(300));
		// ((Graph<MultiAttributeEdgeData>) graph).addConnection(NE, SW, new
		// MultiAttributeEdgeData(Double.NaN));
		// Location loc1 = model.new Location(SW, SE, 3);
		// Location loc2 = model.new Location(SW, SE, 1);
		// Location loc3 = model.new Location(SE, NE, 9.999999);
		// Location loc4 = model.new Location(SW);
		// Location loc5 = model.new Location(SE, SW, 1);
		// Location loc6 = model.new Location(NE, SW, 1);
		//
		// assertEquals(NE, loc3.getPosition());
		//
		// assertTrue(loc1.isOnSameConnection(loc2));
		// assertTrue(loc2.isOnSameConnection(loc1));
		// assertFalse(loc1.isOnSameConnection(loc3));
		// assertFalse(loc3.isOnSameConnection(loc1));
		// assertFalse(loc4.isOnSameConnection(loc1));
		// assertFalse(loc1.isOnSameConnection(loc4));
		// assertFalse(loc3.isOnSameConnection(loc5));
		// assertFalse(loc3.isOnSameConnection(loc6));
	}

	@Test(expected = IllegalArgumentException.class)
	public void checkLocationFail1() {
		// FIXME
		throw new NotImplementedException("todo");
		// model.checkLocation(model.new Location(new Point(1, 2), SE, 3));
	}

	@Test(expected = IllegalArgumentException.class)
	public void checkLocationFail2() {
		// FIXME
		throw new NotImplementedException("todo");
		// model.checkLocation(model.new Location(new Point(1, 2)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getMaxSpeedFail1() {
		model.getMaxSpeed(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getMaxSpeedFail2() {
		model.getMaxSpeed(new TestRoadUser(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getMaxSpeedFail3() {
		model.getMaxSpeed(new TestRoadUser(), new Point(1, 2), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getMaxSpeedFail4() {
		model.getMaxSpeed(new TestRoadUser(), new Point(1, 2), new Point(2, 1));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getMaxSpeed() {
		Point A = new Point(0, 0);
		Point B = new Point(10, 0);

		Graph<LengthEdgeData> g = new MultimapGraph<LengthEdgeData>();
		GraphRoadModel rm = new GraphRoadModel(g);
		g.addConnection(A, B, new LengthEdgeData(3));
		assertEquals(10, rm.getMaxSpeed(new SpeedyRoadUser(10), A, B), EPSILON);

		((Graph<MultiAttributeEdgeData>) graph).addConnection(SE, SW, new MultiAttributeEdgeData(3, 5));
		assertEquals(5, model.getMaxSpeed(new SpeedyRoadUser(10), SE, SW), EPSILON);

		((Graph<MultiAttributeEdgeData>) graph).addConnection(NE, SE, new MultiAttributeEdgeData(3, Double.NaN));
		assertEquals(10, model.getMaxSpeed(new SpeedyRoadUser(10), NE, SE), EPSILON);
	}

	@Test(expected = IllegalArgumentException.class)
	public void computeConnectionLengthFail1() {
		model.computeConnectionLength(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void computeConnectionLengthFail2() {
		model.computeConnectionLength(new Point(1, 2), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void computeConnectionLengthFail3() {
		model.computeConnectionLength(new Point(1, 2), new Point(2, 1));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void computeConnectionLength() {
		assertEquals(0, model.computeConnectionLength(new Point(1, 2), new Point(1, 2)), EPSILON);
		((Graph<MultiAttributeEdgeData>) graph).addConnection(SE, SW, new MultiAttributeEdgeData(5, 5));

		TestRoadUser agent1 = new TestRoadUser();
		model.addObjectAt(agent1, SE);
		PathProgress pp1 = model.followPath(agent1, asPath(SW), ONE_HOUR);
		assertEquals(1, pp1.distance, EPSILON);
		assertEquals(ONE_HOUR, pp1.time);

		TestRoadUser agent2 = new TestRoadUser();
		model.addObjectAt(agent2, SE);
		PathProgress pp2 = model.followPath(agent2, asPath(SW), 2 * ONE_HOUR);
		assertEquals(2, pp2.distance, EPSILON);
		assertEquals(2 * ONE_HOUR, pp2.time);

		assertEquals(1, model.computeConnectionLength(model.getPosition(agent1), model.getPosition(agent2)), EPSILON);

		assertEquals(4, model.computeConnectionLength(model.getPosition(agent1), SW), EPSILON);
		assertEquals(1, model.computeConnectionLength(SE, model.getPosition(agent1)), EPSILON);
	}

	// @Test
	// public void followPathHalfway2() {
	// RoadUser agent1 = new TestRoadUser();
	// model.addObjectAt(agent1, SW);
	// assertTrue(model.containsObjectAt(agent1, SW));
	// assertFalse(model.containsObjectAt(agent1, SE));
	// assertFalse(model.containsObjectAt(null, null));
	// assertTrue(model.equalPosition(agent1, agent1));
	//
	// model.followPath(agent1, new LinkedList<Point>(asList(SW, SE)), 5);
	// assertEquals(new Point(5, 0), model.getPosition(agent1));
	//
	// RoadUser agent2 = new TestRoadUser();
	// model.addObjectAtSamePosition(agent2, agent1);
	// assertEquals(new Point(5, 0), model.getPosition(agent2));
	// assertTrue(model.equalPosition(agent1, agent2));
	// assertFalse(model.equalPosition(null, agent1));
	// assertFalse(model.equalPosition(agent1, null));
	//
	// // rs.followPath(agent2, new
	// LinkedList<Point>(rs.getShortestPathTo(agent2, agent1)), 5);
	// }
	// @Test
	// public void followPathHalfway2() {
	// RoadUser agent1 = new TestRoadUser();
	// model.addObjectAt(agent1, SW);
	// assertTrue(model.containsObjectAt(agent1, SW));
	// assertFalse(model.containsObjectAt(agent1, SE));
	// assertFalse(model.containsObjectAt(null, null));
	// assertTrue(model.equalPosition(agent1, agent1));
	//
	// model.followPath(agent1, new LinkedList<Point>(asList(SW, SE)), 5);
	// assertEquals(new Point(5, 0), model.getPosition(agent1));
	//
	// RoadUser agent2 = new TestRoadUser();
	// model.addObjectAtSamePosition(agent2, agent1);
	// assertEquals(new Point(5, 0), model.getPosition(agent2));
	// assertTrue(model.equalPosition(agent1, agent2));
	// assertFalse(model.equalPosition(null, agent1));
	// assertFalse(model.equalPosition(agent1, null));
	//
	// // rs.followPath(agent2, new
	// LinkedList<Point>(rs.getShortestPathTo(agent2, agent1)), 5);
	// }

}