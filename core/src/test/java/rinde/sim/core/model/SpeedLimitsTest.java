package rinde.sim.core.model;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeEdgeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.graph.TestMultimapGraph;
import rinde.sim.core.graph.TestTableGraph;
import rinde.sim.core.model.RoadModel.PathProgress;
import rinde.sim.util.SpeedConverter;
import rinde.sim.util.TimeUnit;

import static org.junit.Assert.*;
import static java.util.Arrays.asList;

/**
 * Test for graph with speed limits
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 *
 */
@RunWith(Parameterized.class)
public class SpeedLimitsTest {
	final double DELTA = 0.00001;
	
	
	
	
	Class<? extends Graph<MultiAttributeEdgeData>> graphType;
	Class<? extends RoadModel> roadModelType;
	RoadModel model;
	Queue<Point> path;
	Point A, B, C, D, E;

	private final double speed;

	private double pathLength;
	
	
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
	
	public SpeedLimitsTest(Class<? extends Graph<MultiAttributeEdgeData>> graphType, Class<? extends RoadModel> roadModelType, double speed) {
		this.graphType = graphType;
		this.roadModelType = roadModelType;
		this.speed = speed;
	}
	
	@Parameters
	public static Collection<Object[]> configs() {
		double five = 5;
		double twoAndHalf = 2.5;
		return Arrays.asList(new Object[][] { 
				{ TestMultimapGraph.class, RoadModel.class, five}, { TestMultimapGraph.class, CachedRoadModel.class, five},
				{ TestMultimapGraph.class, RoadModel.class, twoAndHalf}, { TestMultimapGraph.class, CachedRoadModel.class, twoAndHalf}, 
				{ TestTableGraph.class, RoadModel.class, five},    { TestTableGraph.class, CachedRoadModel.class,  five},
				{ TestTableGraph.class, RoadModel.class, twoAndHalf},    { TestTableGraph.class, CachedRoadModel.class, twoAndHalf}
		});
	}
	
	
	@Before
	public void setUp() throws InstantiationException, IllegalAccessException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException {

		Graph<MultiAttributeEdgeData> graph = graphType.newInstance();
		
		model = roadModelType.getConstructor(Graph.class).newInstance(graph);
		
		
		A = new Point(0, 0);
		B = new Point(0, 10);
		C = new Point(10, 0);
		D = new Point(10,10);
		E = new Point(5, 15);
		
		
		graph.addConnection(A, B); // length 10 no speed limit
		graph.addConnection(B, C, new MultiAttributeEdgeData(10, 2.5)); //length 10 speed 4
		graph.addConnection(C, B); // length Math.sqr(10^2 + 10^2)
		graph.addConnection(B, D, new MultiAttributeEdgeData(10, 10)); //length 10 speed 10
		graph.addConnection(C, D); //length 10
		graph.addConnection(D, C, new MultiAttributeEdgeData(12, 1)); //length 12 speed 1
		graph.addConnection(D, E, new MultiAttributeEdgeData(5, 7));
		

		Set<Point> points = graph.getNodes();
		assertEquals(5, points.size());
		assertTrue(points.contains(A));
		assertTrue(points.contains(B));
		assertTrue(points.contains(C));
		assertTrue(points.contains(D));
		assertTrue(points.contains(E));

		assertEquals(7, model.getGraph().getNumberOfConnections());
		assertEquals(5, model.getGraph().getNumberOfNodes());

		path = new LinkedList<Point>();
		path.addAll(asList(A, B, C, D, E));
		
		pathLength = 10 + 10 + 10 + 5;
	}
	
	/**
	 * Follow trajectory using time version of the function. 
	 */
	@Test
	public void followTrajectoryAllAtOnce() {
		int timeNeeded = (int) (TimeUnit.H.toMs((long) pathLength) / speed * 1.5);
		
		SpeedyRoadUser agent = new SpeedyRoadUser(speed);
		model.addObjectAt(agent, new Point(0, 0));
		assertEquals(new Point(0, 0), model.getPosition(agent));

		assertEquals(5, path.size());
		PathProgress travelled = model.followPath(agent, path, timeNeeded);
		
		assertEquals(pathLength, travelled.distance, DELTA);
		assertTrue("time spend < timeNeeded",  timeNeeded > travelled.time);
		assertEquals(0, path.size());
		assertEquals(new Point(5, 15),model.getPosition(agent));
	}
	
	/**
	 * Simplest check for time based following path {@link RoadModel#followPath(MovingRoadUser, Queue, long)}
	 */
	@Test
	public void followTrajectory() {
		
		assertEquals(5, path.size());
		
		MovingRoadUser agent = new SpeedyRoadUser(speed);
		model.addObjectAt(agent, new Point(0, 0));
		assertTrue(model.getPosition(agent).equals(new Point(0, 0)));
		assertEquals(5, path.size());
		
		PathProgress progress = model.followPath(agent, path, TimeUnit.H.toMs(2));
		assertEquals(2*speed, progress.distance, DELTA);
		assertEquals(new Point(0,2*speed), model.getPosition(agent));
		if(speed < 5) {
			progress = model.followPath(agent, path, TimeUnit.H.toMs(2));
			assertEquals(2*speed, progress.distance, DELTA);
		}
		
		assertEquals(3, path.size());		
		assertEquals(new Point(0,10), model.getPosition(agent));
		
		// traveling on edge with max speed 2.5
		progress = model.followPath(agent, path, TimeUnit.H.toMs(2));
		assertEquals(3, path.size());
		assertEquals(5, progress.distance, DELTA);
		
		progress = model.followPath(agent, path, TimeUnit.H.toMs(2));
		assertEquals(3, path.size());
		assertEquals(5, progress.distance, DELTA);
		assertEquals(C, model.getPosition(agent));
		
		long time = speed < 5 ? 4 : 2; 
		progress = model.followPath(agent, path, TimeUnit.H.toMs(time)); //follow path for 2 x time
		assertEquals(10, progress.distance, DELTA);
		assertEquals(TimeUnit.H.toMs(time), progress.time);
		assertEquals(1, path.size());
		assertEquals(D, model.getPosition(agent));
		
		//travel with max speed of the vehicle and time longer than needed
		time = speed < 5 ? 2 : 1; 
		progress = model.followPath(agent, path, TimeUnit.H.toMs(3)); 
		assertEquals(5, progress.distance, DELTA);
		assertEquals(TimeUnit.H.toMs(time), progress.time);
		assertEquals(0, path.size());
		assertEquals(E, model.getPosition(agent));

	}
	
	@Test
	public void maxSpeedTest() {
		SpeedyRoadUser agent = new SpeedyRoadUser(speed);
		assertEquals(speed, model.getMaxSpeed(agent, A, B), DELTA);
		assertEquals(speed > 2.5 ? 2.5  :speed, model.getMaxSpeed(agent, B, C), DELTA);
		assertEquals(speed, model.getMaxSpeed(agent, C, B), DELTA);
		assertEquals(speed, model.getMaxSpeed(agent, B, D), DELTA);
		assertEquals(speed, model.getMaxSpeed(agent, C, D), DELTA);
		assertEquals(1, model.getMaxSpeed(agent, D, C), DELTA);
		assertEquals(speed, model.getMaxSpeed(agent, D, E), DELTA);
	}
	
	@Test
	public void followPathStepByStep() {
		SpeedyRoadUser agent = new SpeedyRoadUser(speed);
		model.addObjectAt(agent, new Point(0, 0));
		assertEquals(new Point(0, 0), model.getPosition(agent));
		assertEquals(5, path.size());
		
		PathProgress progress = model.followPath(agent, path, TimeUnit.H.toMs(1));
		assertEquals(speed, progress.distance, DELTA);
		assertEquals(4, path.size());
	}

	
	private class SpeedyRoadUser implements MovingRoadUser {
		private double speedRU;

		public SpeedyRoadUser(double speed) {
			this.speedRU = speed;
		}
		
		@Override
		public void initRoadUser(RoadModel model) {
		}

		@Override
		public double getSpeed() {
			return speedRU;
		}	
	}
	
}


