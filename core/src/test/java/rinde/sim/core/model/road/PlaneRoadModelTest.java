/**
 * 
 */
package rinde.sim.core.model.road;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Queue;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import rinde.sim.core.graph.Point;


/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class PlaneRoadModelTest extends AbstractRoadModelTest<PlaneRoadModel> {

	@Override
	public void setUp() {
		model = new PlaneRoadModel(new Point(0, 0), new Point(10, 10), 10);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void constructorFail1() {
		new PlaneRoadModel(new Point(1, 0), new Point(0, 1), 1);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void constructorFail2() {
		new PlaneRoadModel(new Point(0, 1), new Point(1, 0), 1);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void constructorFail3() {
		new PlaneRoadModel(new Point(0, 0), new Point(1, 1), 0);
	}

	@Test
	public void checkPointIsInBoundary() {
		assertTrue(model.checkPointIsInBoundary(new Point(0, 0)));
		assertTrue(model.checkPointIsInBoundary(new Point(10, 10)));
		assertTrue(model.checkPointIsInBoundary(new Point(0, 10)));
		assertTrue(model.checkPointIsInBoundary(new Point(10, 0)));
		assertTrue(model.checkPointIsInBoundary(new Point(5, 5)));
		assertTrue(model.checkPointIsInBoundary(new Point(0, 3)));

		assertFalse(model.checkPointIsInBoundary(new Point(-1, 5)));
		assertFalse(model.checkPointIsInBoundary(new Point(11, 5)));
		assertFalse(model.checkPointIsInBoundary(new Point(5, -234)));
		assertFalse(model.checkPointIsInBoundary(new Point(5, 10.00001)));

		assertFalse(model.checkPointIsInBoundary(new Point(-5, -0.0001)));
		assertFalse(model.checkPointIsInBoundary(new Point(14, -0.0009)));
		assertFalse(model.checkPointIsInBoundary(new Point(100, 100)));
		assertFalse(model.checkPointIsInBoundary(new Point(-100, 100)));

		RandomGenerator rnd = new MersenneTwister(123);
		for (int i = 0; i < 100; i++) {
			Point p = model.getRandomPosition(rnd);
			assertTrue(model.checkPointIsInBoundary(p));
		}
	}

	@Test
	public void followPath() {
		MovingRoadUser mru = new TestRoadUser();
		model.addObjectAt(mru, new Point(0, 0));
		Queue<Point> path = asPath(new Point(0, 0), new Point(5, 0), new Point(5, 5));

		PathProgress pp = model.followPath(mru, path, hour());
		assertEquals(asPath(new Point(5, 0), new Point(5, 5)), path);
		assertEquals(1, pp.distance, EPSILON);
		assertEquals(hour().getTimeStep(), pp.time, EPSILON);
		assertEquals(asList(new Point(0, 0)), pp.travelledNodes);
		assertTrue(Point.distance(new Point(1, 0), model.getPosition(mru)) < EPSILON);

		PathProgress pp2 = model.followPath(mru, path, hour(5));
		assertEquals(asPath(new Point(5, 5)), path);
		assertEquals(5, pp2.distance, EPSILON);
		assertEquals(hour(5).getTimeStep(), pp2.time, EPSILON);
		assertEquals(asList(new Point(5, 0)), pp2.travelledNodes);
		assertTrue(Point.distance(new Point(5, 1), model.getPosition(mru)) < EPSILON);

		PathProgress pp3 = model.followPath(mru, path, hour(50));
		assertTrue(path.isEmpty());
		assertEquals(4, pp3.distance, EPSILON);
		assertEquals(hour(4).getTimeStep(), pp3.time, EPSILON);
		assertEquals(asList(new Point(5, 5)), pp3.travelledNodes);
		assertTrue(Point.distance(new Point(5, 5), model.getPosition(mru)) < EPSILON);
	}

	@Test(expected = IllegalArgumentException.class)
	public void followPathFail() {
		Queue<Point> path = asPath(new Point(0, 0), new Point(5, 0), new Point(5, 5), new Point(100, 0));
		MovingRoadUser mru = new TestRoadUser();
		model.addObjectAt(mru, new Point(0, 0));
		model.followPath(mru, path, hour(100));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getShortestPathToFail1() {
		model.getShortestPathTo(new Point(-1, 0), new Point(5, 5));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getShortestPathToFail2() {
		model.getShortestPathTo(new Point(0, 0), new Point(-5, 5));
	}

	@Test
	public void getShortestPathTo() {
		assertEquals(asList(new Point(0, 0), new Point(5, 5)), model.getShortestPathTo(new Point(0, 0), new Point(5, 5)));
	}

}
