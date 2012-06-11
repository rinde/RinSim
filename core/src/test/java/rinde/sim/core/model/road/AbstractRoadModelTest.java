/**
 * 
 */
package rinde.sim.core.model.road;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.util.SpeedConverter;
import rinde.sim.util.TrivialRoadUser;

import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public abstract class AbstractRoadModelTest<T extends RoadModel> {

	protected static final long ONE_HOUR = 60 * 60 * 1000;
	protected final SpeedConverter sc = new SpeedConverter();
	protected final double EPSILON = 0.02;

	protected T model;
	protected Point SW;
	protected Point SE;
	protected Point NE;
	protected Point NW;

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

	public static Queue<Point> asPath(Point... points) {
		return new LinkedList<Point>(Arrays.asList(points));
	}

	public static <T> Set<T> asSet(T... list) {
		return new LinkedHashSet<T>(asList(list));
	}

	@Test
	public void getPosition() {
		RoadUser ru = new TestRoadUser();
		model.addObjectAt(ru, SW);
		assertEquals(SW, model.getPosition(ru));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPositionFail() {
		model.getPosition(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPositionFail2() {
		model.getPosition(new TestRoadUser());
	}

	@Test(expected = IllegalArgumentException.class)
	public void registerNull() {
		model.register(null);
	}

	@Test
	public void register() {
		TestRoadUser driver = new TestRoadUser();
		model.register(driver);
		assertEquals(model, driver.getRoadModel());
	}

	@Test(expected = IllegalArgumentException.class)
	public void unregisterNull() {
		model.unregister(null);
	}

	@Test
	public void unregister() {
		assertFalse(model.unregister(new TestRoadUser()));
		TestRoadUser driver = new TestRoadUser();
		assertTrue(model.register(driver));
		assertFalse(model.unregister(driver));
		model.addObjectAt(driver, SW);
		assertTrue(model.unregister(driver));
	}

	@Test
	public void getSupportedType() {
		assertEquals(RoadUser.class, model.getSupportedType());
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

	@Test
	public void getObjectsAt() {
		TestRoadUser agent1 = new TestRoadUser();
		TestRoadUser agent2 = new TestRoadUser();
		TestRoadUser agent3 = new TestRoadUser();
		TestRoadUser agent4 = new TestRoadUser();
		TestRoadUser agent5 = new TestRoadUser();

		model.addObjectAt(agent1, SW);
		model.addObjectAt(agent2, NE);
		model.addObjectAt(agent3, SE);
		model.addObjectAt(agent4, NE);
		model.addObjectAt(agent5, SE);
		assertTrue(Sets.difference(asSet(agent1), model.getObjectsAt(agent1, TestRoadUser.class)).isEmpty());
		assertTrue(Sets.difference(asSet(agent2, agent4), model.getObjectsAt(agent2, TestRoadUser.class)).isEmpty());
		assertTrue(model.getObjectsAt(agent2, SpeedyRoadUser.class).isEmpty());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getObjectsOfTypeFail() {
		model.getObjectsOfType(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getObjectsAtFail1() {
		model.getObjectsAt(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getObjectsAtFail2() {
		model.getObjectsAt(new TestRoadUser(), null);
	}

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

		model.followPath(ru, asPath(SE), 1 * ONE_HOUR);
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

	/**
	 * 
	 */
	public AbstractRoadModelTest() {
		super();
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

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void pathProgressConstructorFail1() {
		new PathProgress(-1, 1, null);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void pathProgressConstructorFail2() {
		new PathProgress(1, -1, null);
	}

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void pathProgressConstructorFail3() {
		new PathProgress(1, 1, null);
	}

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