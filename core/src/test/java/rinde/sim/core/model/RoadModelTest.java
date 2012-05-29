/**
 * 
 */
package rinde.sim.core.model;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.EdgeData;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TestMultimapGraph;
import rinde.sim.core.graph.TestTableGraph;
import rinde.sim.core.model.RoadModel.PathProgress;
import rinde.sim.util.SpeedConverter;
import rinde.sim.util.TimeUnit;
import rinde.sim.util.TrivialRoadUser;

import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
@RunWith(Parameterized.class)
public class RoadModelTest {

	static Queue<Point> asQueue(Point... points) {
		return new LinkedList<Point>(Arrays.asList(points));
	}

	private static final long ONE_HOUR = 60 * 60 * 1000;

	final private SpeedConverter sc = new SpeedConverter();

	final double EPSILON = 0.02;

	Class<? extends Graph<?>> graphType;

	Graph<? extends EdgeData> graph;
	Class<? extends RoadModel> roadModelType;
	RoadModel model;
	Queue<Point> path;
	Point SW, SE, NE, NW;

	public RoadModelTest(Class<? extends Graph<?>> pGraphType, Class<? extends RoadModel> pRoadModelType) {
		graphType = pGraphType;
		roadModelType = pRoadModelType;
	}

	@Parameters
	public static Collection<Object[]> configs() {
		return Arrays.asList(new Object[][] { { TestMultimapGraph.class, RoadModel.class },
				{ TestMultimapGraph.class, CachedRoadModel.class }, { TestTableGraph.class, RoadModel.class },
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

		Set<Point> points = model.getGraph().getNodes();
		assertEquals(4, points.size());
		assertTrue(points.contains(SW));
		assertTrue(points.contains(SE));
		assertTrue(points.contains(NE));

		assertEquals(3, model.getGraph().getNumberOfConnections());
		assertEquals(4, model.getGraph().getNumberOfNodes());

		path = new LinkedList<Point>();
		path.addAll(asList(SW, SE, NE));

	}

	/**
	 * Follow trajectory using time version of the function.
	 */
	@Test
	public void followTrajectoryAllAtOnce() {
		SpeedyRoadUser agent = new SpeedyRoadUser(sc.from(5, TimeUnit.MS).to(TimeUnit.H));
		model.addObjectAt(agent, new Point(0, 0));
		assertEquals(new Point(0, 0), model.getPosition(agent));

		assertEquals(3, path.size());
		PathProgress travelled = model.followPath(agent, path, 5);
		assertEquals(20, travelled.distance, EPSILON);
		assertEquals(4, travelled.time);
		assertEquals(0, path.size());
		assertEquals(new Point(10, 10), model.getPosition(agent));
	}

	// @Test(expected = IllegalArgumentException.class)
	// public void followPathFail1() {
	// model.followPath(null, null, 0);
	// }

	@Test(expected = IllegalArgumentException.class)
	public void followTrajectoryUnconnected() {
		MovingRoadUser agent = new SpeedyRoadUser(100);
		model.addObjectAt(agent, new Point(0, 0));
		assertEquals(new Point(0, 0), model.getPosition(agent));

		// illegal trajectory, the two points are not connected
		Queue<Point> traject = new LinkedList<Point>(Arrays.asList(new Point(0, 0), new Point(10, 10)));

		assertEquals(2, traject.size());
		model.followPath(agent, traject, 20);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followTrajectoryNotAvertex() {
		MovingRoadUser agent = new SpeedyRoadUser(EPSILON);
		model.addObjectAt(agent, new Point(0, 0));
		assertEquals(new Point(0, 0), model.getPosition(agent));

		// illegal trajectory, the second point is not a vertex
		Queue<Point> traject = new LinkedList<Point>(Arrays.asList(new Point(0, 0), new Point(10, 1)));

		assertEquals(2, traject.size());
		model.followPath(agent, traject, 20);
	}

	@Test
	public void followTrajectoryNotTillEnd() {
		MovingRoadUser agent = new SpeedyRoadUser(sc.from(1, TimeUnit.MS).to(TimeUnit.H));
		model.addObjectAt(agent, new Point(0, 0));
		assertEquals(new Point(0, 0), model.getPosition(agent));

		PathProgress travelled = model.followPath(agent, path, 10);
		assertEquals(10d, travelled.distance, EPSILON);
		assertEquals(1, path.size());

		travelled = model.followPath(agent, path, 1);
		assertEquals(1d, travelled.distance, EPSILON);
		assertEquals(1, path.size());
		assertEquals(new Point(10, 1), model.getPosition(agent));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addConnection() {
		// connection already exists
		graph.addConnection(SW, SE);
	}

	// @Test
	// public void followTrajectory() {
	// RoadUser agent = new TestRoadUser();
	// model.addObjectAt(agent, new Point(0, 0));
	// assertTrue(model.getPosition(agent).equals(new Point(0, 0)));
	//
	// assertTrue(path.size() == 3);
	// double travelled = model.followPath(agent, path, 5);
	// assertTrue(travelled == 5);
	// assertTrue(path.size() == 2);
	// assertTrue(model.getPosition(agent).equals(new Point(5, 0)));
	//
	// travelled = model.followPath(agent, path, 10);
	// assertEquals(10, travelled, EPSILON);
	// assertTrue(path.size() == 1);
	// assertTrue(model.getPosition(agent).equals(new Point(10, 5)));
	//
	// travelled = model.followPath(agent, path, 1);
	// assertEquals(1, travelled, EPSILON);
	// assertTrue(path.size() == 1);
	// assertTrue(model.getPosition(agent).equals(new Point(10, 6)));
	//
	// travelled = model.followPath(agent, path, 2);
	// assertEquals(2, travelled, EPSILON);
	// assertTrue(path.size() == 1);
	// assertTrue(model.getPosition(agent).equals(new Point(10, 8)));
	//
	// travelled = model.followPath(agent, path, 3);
	// assertTrue(travelled == 2);
	// assertEquals(2, travelled, EPSILON);
	// assertTrue(path.size() == 0);
	// assertTrue(model.getPosition(agent).equals(new Point(10, 10)));
	// }

	/**
	 * Simplest check for time based following path
	 * {@link RoadModel#followPath(MovingRoadUser, Queue, long)}
	 */
	@Test
	public void followTrajectoryTime() {
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

	// @Test
	// public void followTrajectory2() {
	// RoadUser agent = new TestRoadUser();
	// model.addObjectAt(agent, new Point(0, 0));
	// assertTrue(model.getPosition(agent).equals(new Point(0, 0)));
	//
	// assertTrue(path.size() == 3);
	// double travelled = model.followPath(agent, path, 20);
	// assertTrue(travelled == 20);
	// assertTrue(path.size() == 0);
	// assertTrue(model.getPosition(agent).equals(new Point(10, 10)));
	// }

	// @Test(expected = IllegalArgumentException.class)
	// public void followTrajectory3() {
	// RoadUser agent = new TestRoadUser();
	// model.addObjectAt(agent, new Point(10, 10));
	// assertTrue(model.getPosition(agent).equals(new Point(10, 10)));
	//
	// assertTrue(path.size() == 3);
	// // the trajectory does not directly connect to the current position
	// model.followPath(agent, path, 20);
	// }

	// @Test(expected = IllegalStateException.class)
	// public void followTrajectory4() {
	// RoadUser agent = new TestRoadUser();
	// model.addObjectAt(agent, new Point(0, 0));
	// assertTrue(model.getPosition(agent).equals(new Point(0, 0)));
	//
	// // illegal trajectory, the two points are not connected
	// Queue<Point> traject = new LinkedList<Point>(Arrays.asList(new Point(0,
	// 0), new Point(10, 10)));
	//
	// assertTrue(traject.size() == 2);
	// model.followPath(agent, traject, 20);
	// }

	// @Test(expected = IllegalStateException.class)
	// public void followTrajectory5() {
	// RoadUser agent = new TestRoadUser();
	// model.addObjectAt(agent, new Point(0, 0));
	// assertTrue(model.getPosition(agent).equals(new Point(0, 0)));
	//
	// // illegal trajectory, the second point is not a vertex
	// Queue<Point> traject = new LinkedList<Point>(Arrays.asList(new Point(0,
	// 0), new Point(10, 1)));
	//
	// assertTrue(traject.size() == 2);
	// model.followPath(agent, traject, 20);
	// }

	// @Test
	// public void followTrajectory6() {
	// RoadUser agent = new TestRoadUser();
	// model.addObjectAt(agent, new Point(0, 0));
	// assertTrue(model.getPosition(agent).equals(new Point(0, 0)));
	//
	// double travelled = model.followPath(agent, path, 10);
	// assertEquals(10d, travelled, EPSILON);
	// assertTrue(path.size() == 1);
	//
	// travelled = model.followPath(agent, path, 1);
	// assertEquals(1d, travelled, EPSILON);
	// assertTrue(path.size() == 1);
	// assertEquals(new Point(10, 1), model.getPosition(agent));
	// }

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
		assertEquals(asList(SE, NE), path2);
		model.followPath(agent2, path2, TimeUnit.H.toMs(10));
		assertEquals(new Point(10, 5), model.getPosition(agent2));

		// coming from the front side, we have to turn around at p1
		Queue<Point> path3 = new LinkedList<Point>(model.getShortestPathTo(agent2, agent1));
		assertEquals(asList(NE, SE, SW, new Point(5, 0)), path3);
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
		model.followPath(testRoadUser, asQueue(SW), 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathFailIllegalPath2() {
		TestRoadUser testRoadUser1 = new TestRoadUser();
		model.addObjectAt(testRoadUser1, SW);

		graph.addConnection(SE, SW);

		model.followPath(testRoadUser1, asQueue(SW, SE, SW), 11 * ONE_HOUR);

		TestRoadUser testRoadUser2 = new TestRoadUser();
		model.addObjectAt(testRoadUser2, SW);
		model.followPath(testRoadUser2, asQueue(SW, SE), 2 * ONE_HOUR);
		model.followPath(testRoadUser2, asQueue(model.getPosition(testRoadUser1)), 2 * ONE_HOUR);
	}

	/**
	 * Situation: <code>SW(tru2) ->- SE ->- tru1 ->- NE </code><br/>
	 * tru2 moves towards tru1, it should end up between SE and tru1.
	 */
	@Test
	public void followPathMoveTowardOther() {
		TestRoadUser tru1 = new TestRoadUser();
		model.addObjectAt(tru1, SW);
		model.followPath(tru1, asQueue(SW, SE, NE), 12 * ONE_HOUR);

		TestRoadUser tru2 = new TestRoadUser();
		model.addObjectAt(tru2, SW);
		PathProgress pp = model.followPath(tru2, asQueue(SW, SE, model.getPosition(tru1)), 11 * ONE_HOUR);
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
		model.followPath(testRoadUser1, asQueue(SW, SE), ONE_HOUR);
		model.followPath(testRoadUser1, asQueue(SW), ONE_HOUR);
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
		model.followPath(tru1, asQueue(SW, SE), ONE_HOUR);

		TestRoadUser tru2 = new TestRoadUser();
		model.addObjectAt(tru2, SW);
		model.followPath(tru2, asQueue(SW, SE), 2 * ONE_HOUR);

		// now the road users are in the above described positions
		Point difference = Point.diff(model.getPosition(tru1), model.getPosition(tru2));
		assertTrue(difference.x < 0 && difference.y == 0);

		model.followPath(tru2, asQueue(model.getPosition(tru1)), 2 * ONE_HOUR);
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
		model.followPath(tru1, asQueue(SW, SE), ONE_HOUR);

		TestRoadUser tru2 = new TestRoadUser();
		model.addObjectAt(tru2, NE);
		model.followPath(tru2, asQueue(NE, SE), 2 * ONE_HOUR);

		model.followPath(tru2, asQueue(model.getPosition(tru1)), 2 * ONE_HOUR);
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
		model.followPath(tru2, asQueue(NE, SE), 2 * ONE_HOUR);

		model.followPath(tru1, asQueue(model.getPosition(tru2)), 2 * ONE_HOUR);
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
		model.followPath(tru2, asQueue(NE, SE), 2 * ONE_HOUR);

		model.followPath(tru1, asQueue(SW, model.getPosition(tru2)), 200 * ONE_HOUR);
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
		model.followPath(tru1, asQueue(SW, SE), ONE_HOUR);

		TestRoadUser tru2 = new TestRoadUser();
		model.addObjectAt(tru2, SW);
		model.followPath(tru2, asQueue(SW, SE), 2 * ONE_HOUR);

		model.followPath(tru1, asQueue(model.getPosition(tru2)), 2 * ONE_HOUR);
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
		model.followPath(tru2, asQueue(SW, SE), 2 * ONE_HOUR);

		model.followPath(tru1, asQueue(model.getPosition(tru2)), 3 * ONE_HOUR);
		assertEquals(model.getPosition(tru1), model.getPosition(tru2));
	}

	//
	// @Test(expected = IllegalArgumentException.class)
	// public void followPathFailWrongDirection() {
	// model.followPath(testRoadUser, new LinkedList<Point>(Arrays.asList(SW)),
	// 1);
	// model.followPath(testRoadUser, new LinkedList<Point>(Arrays.asList(SW)),
	// 1);
	// }

	// @Test(expected = AssertionError.class)
	// public void followPathFail5() {
	// model.followPath(new TestRoadUser(), new
	// LinkedList<Point>(Arrays.asList(SW)), 1);
	// }

	// @Test(expected = IllegalArgumentException.class)
	// public void followPathFail6() {
	// RoadUser agent = new TestRoadUser();
	// model.addObjectAt(agent, SW);
	// model.followPath(agent, path, 1);
	// model.followPath(agent, new LinkedList<Point>(Arrays.asList(SW, SE, NE)),
	// 1);
	// }

	// @Test(expected = IllegalArgumentException.class)
	// public void followPathFail7() {
	// RoadUser agent = new TestRoadUser();
	// RoadUser agent2 = new TestRoadUser();
	//
	// model.addObjectAt(agent2, NE);
	// model.addConnection(NE, SE);
	// model.followPath(agent2, new LinkedList<Point>(Arrays.asList(NE, SE)),
	// 1);
	//
	// model.addObjectAt(agent, SW);
	// List<Point> curPath = model.getShortestPathTo(agent, agent2);
	// model.followPath(agent, new LinkedList<Point>(curPath), 1);
	//
	// model.followPath(agent, new LinkedList<Point>(Arrays.asList(SW,
	// curPath.get(0))), 1);
	// }

	// @Test
	// public void followPathOk() {
	// RoadUser agent = new TestRoadUser();
	// RoadUser agent2 = new TestRoadUser();
	//
	// model.addObjectAt(agent2, NE);
	// model.addConnection(NE, SE);
	// model.followPath(agent2, new LinkedList<Point>(Arrays.asList(NE, SE)),
	// 1);
	//
	// model.addObjectAt(agent, SW);
	// Queue<Point> curPath = new
	// LinkedList<Point>(model.getShortestPathTo(agent, agent2));
	// model.followPath(agent, curPath, 1);
	//
	// // this is to get more test coverage
	// model.followPath(agent, curPath, 19.5);
	// }

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
	public void getPositionFail() {
		model.getPosition(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPositionFail2() {
		model.getPosition(new TestRoadUser());
	}

	@Test(expected = InvocationTargetException.class)
	public void constructorFail() throws IllegalArgumentException, SecurityException, InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		roadModelType.getConstructor(Graph.class).newInstance((Graph<?>) null);
	}

	@Test
	public void testClear() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();
		RoadUser agent3 = new TestRoadUser();
		model.addObjectAt(agent1, SW);
		model.addObjectAt(agent2, SE);
		model.addObjectAt(agent3, NE);
		assertEquals(3, model.getObjects().size());
		model.clear();
		assertTrue(model.getObjects().isEmpty());
	}

	@Test
	public void testGetObjectsAndPositions() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();
		RoadUser agent3 = new RoadUser() {

			@Override
			public void initRoadUser(RoadModel pModel) {

				// can be ignored in this test [bm]
			}
		};
		model.addObjectAt(agent1, SW);
		model.addObjectAt(agent2, SE);
		model.addObjectAt(agent3, NE);

		Map<RoadUser, Point> mapCopy = model.getObjectsAndPositions();
		Set<RoadUser> setCopy = model.getObjects();
		Set<TestRoadUser> subsetCopy = model.getObjectsOfType(TestRoadUser.class);
		Collection<Point> posCopy = model.getObjectPositions();

		assertEquals(3, model.getObjectsAndPositions().size());
		assertEquals(3, mapCopy.size());
		assertEquals(3, setCopy.size());
		assertEquals(2, subsetCopy.size());
		assertEquals(3, posCopy.size());

		model.removeObject(agent1);
		assertEquals(2, model.getObjectsAndPositions().size());
		assertEquals(3, mapCopy.size());
		assertEquals(3, setCopy.size());
		assertEquals(2, subsetCopy.size());
		assertEquals(3, posCopy.size());
	}

	class SpeedyRoadUser implements MovingRoadUser {

		private final double speed;

		public SpeedyRoadUser(double pSpeed) {
			speed = pSpeed;
		}

		@Override
		public void initRoadUser(RoadModel pModel) {}

		@Override
		public double getSpeed() {
			return speed;
		}

	}

	class TestRoadUser extends TrivialRoadUser {}

	class TestRoadUser2 extends TrivialRoadUser {}

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

	@Test(expected = IllegalArgumentException.class)
	public void addTruckTest() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();
		model.addObjectAt(agent1, new Point(0, 0));
		model.addObjectAt(agent2, new Point(1, 0));// this location is not a
													// crossroad
	}

	@Test(expected = IllegalArgumentException.class)
	public void addTruckTest2() {
		RoadUser t = new TestRoadUser();
		model.addObjectAt(t, new Point(0, 0));
		model.addObjectAt(t, new Point(10, 0));// object is already added
	}

	@Test
	public void removeObjectTest() {
		RoadUser agent1 = new TestRoadUser();
		model.addObjectAt(agent1, new Point(0, 0));
		model.removeObject(agent1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void removeObjectTestFail() {
		model.removeObject(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void containsObjectNull() {
		model.containsObject(null);
	}

	@Test
	public void containsObjectAt() {
		assertFalse(model.containsObjectAt(new TestRoadUser(), new Point(2, 3)));
		TestRoadUser ru = new TestRoadUser();
		model.addObjectAt(ru, SW);
		assertFalse(model.containsObjectAt(ru, new Point(2, 3)));
		assertTrue(model.containsObjectAt(ru, SW));

		model.followPath(ru, asQueue(SE), 1 * ONE_HOUR);
		Point p = model.getPosition(ru);
		assertTrue(model.containsObjectAt(ru, Point.duplicate(p)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void containsObjectAtNull1() {
		model.containsObjectAt(null, new Point(1, 2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void containsObjectAtNull2() {
		model.containsObjectAt(new TestRoadUser(), null);
	}

	@Test
	public void addObjectAtSamePosition() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();
		model.addObjectAt(agent1, SW);
		model.addObjectAtSamePosition(agent2, agent1);
		assertEquals(SW, model.getPosition(agent1));
		assertEquals(SW, model.getPosition(agent2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addObjectAtSamePositionFail() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();
		model.addObjectAt(agent1, SW);
		model.addObjectAt(agent2, SE);
		model.addObjectAtSamePosition(agent2, agent1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addObjectAtSamePositionFail2() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();
		RoadUser agent3 = new TestRoadUser();
		model.addObjectAt(agent2, SE);
		model.addObjectAtSamePosition(agent3, agent1);
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
		if (model instanceof CachedRoadModel) {
			Table<Point, Point, List<Point>> cache = HashBasedTable.create();
			List<Point> cachePath = Arrays.asList(SW, NE);
			cache.put(SW, NE, cachePath);

			((CachedRoadModel) model).setPathCache(cache);

			List<Point> shortPath = model.getShortestPathTo(SW, NE);

			assertEquals(shortPath, cachePath);

			assertEquals(cache, ((CachedRoadModel) model).getPathCache());
		}
	}

	@Test
	public void testEqualPosition() {
		RoadUser agent1 = new TestRoadUser();
		RoadUser agent2 = new TestRoadUser();

		assertFalse(model.equalPosition(agent1, agent2));
		model.addObjectAt(agent1, SW);
		assertFalse(model.equalPosition(agent1, agent2));
		model.addObjectAt(agent2, NE);
		assertFalse(model.equalPosition(agent1, agent2));
		model.removeObject(agent2);
		model.addObjectAt(agent2, SW);
		assertTrue(model.equalPosition(agent1, agent2));
	}

	// @Test
	// public void testMiddlePoint() {
	// RoadUser agent1 = new TestRoadUser();
	// RoadUser agent2 = new TestRoadUser();
	// model.addObjectAt(agent1, SW);
	// model.addObjectAt(agent2, NE);
	// model.followPath(agent1, new LinkedList<Point>(Arrays.asList(SW, SE)),
	// 1);
	// model.followPath(agent2, new LinkedList<Point>(Arrays.asList(NE, NW)),
	// 1);
	//
	// Point a1 = model.getPosition(agent1);
	// Point a2 = model.getPosition(agent2);
	// assertEquals(new Point(1, 0), a1);
	// assertEquals(new Point(9, 10), a2);
	// assertTrue(a1 instanceof RoadModel.MidPoint);
	// assertTrue(a2 instanceof RoadModel.MidPoint);
	//
	// List<Point> shortestPath = model.getShortestPathTo(a1, a2);
	// assertEquals(Arrays.asList(a1, SE, NE, a2), shortestPath);
	// }

	@Test
	public void testObjectOrder() {
		List<RoadUser> objects = new ArrayList<RoadUser>();
		List<Point> positions = Arrays.asList(NE, SE, SW, NE);
		for (int i = 0; i < 100; i++) {
			RoadUser u;
			if (i % 2 == 0) {
				u = new TestRoadUser();
			} else {
				u = new TestRoadUser2();
			}
			objects.add(u);
			model.addObjectAt(u, positions.get(i % positions.size()));
		}

		// checking whether the returned objects are in insertion order
		List<RoadUser> modelObjects = new ArrayList<RoadUser>(model.getObjects());
		assertEquals(objects.size(), modelObjects.size());
		for (int i = 0; i < modelObjects.size(); i++) {
			assertTrue(modelObjects.get(i) == objects.get(i));
		}

		model.removeObject(objects.remove(97));
		model.removeObject(objects.remove(67));
		model.removeObject(objects.remove(44));
		model.removeObject(objects.remove(13));
		model.removeObject(objects.remove(3));

		// check to see if the objects are still in insertion order, event after
		// removals
		List<RoadUser> modelObjects2 = new ArrayList<RoadUser>(model.getObjects());
		assertEquals(objects.size(), modelObjects2.size());
		for (int i = 0; i < modelObjects2.size(); i++) {
			assertTrue(modelObjects2.get(i) == objects.get(i));
		}

		// make sure that the order is preserved, even when using a predicate
		List<RoadUser> modelObjects3 = new ArrayList<RoadUser>(model.getObjects(new Predicate<RoadUser>() {
			@Override
			public boolean apply(RoadUser input) {
				return true;
			}
		}));
		assertEquals(objects.size(), modelObjects3.size());
		for (int i = 0; i < modelObjects3.size(); i++) {
			assertTrue(modelObjects3.get(i) == objects.get(i));
		}

		// make sure that the order of the objects is preserved, even in this
		// RoadUser, Point map
		List<Entry<RoadUser, Point>> modelObjects4 = new ArrayList<Entry<RoadUser, Point>>(model
				.getObjectsAndPositions().entrySet());
		assertEquals(objects.size(), modelObjects4.size());
		for (int i = 0; i < modelObjects4.size(); i++) {
			assertTrue(modelObjects4.get(i).getKey() == objects.get(i));
		}

		// make sure that the order is preserved, even when using a type
		List<RoadUser> modelObjects5 = new ArrayList<RoadUser>(model.getObjectsOfType(TestRoadUser2.class));
		assertEquals(46, modelObjects5.size());
		int j = 0;
		for (int i = 0; i < modelObjects5.size(); i++) {
			// skip all other objects
			while (!(objects.get(j) instanceof TestRoadUser2)) {
				j++;
			}
			assertTrue(modelObjects5.get(i) == objects.get(j));
			j++;
		}

	}
}