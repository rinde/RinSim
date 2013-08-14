/**
 * 
 */
package rinde.sim.core.model.road;

import static com.google.common.collect.Lists.newLinkedList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.TimeLapseFactory;
import rinde.sim.core.graph.Point;
import rinde.sim.util.SpeedConverter;
import rinde.sim.util.TrivialRoadUser;

import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @param <T> the type of RoadModel to test
 * 
 */
public abstract class AbstractRoadModelTest<T extends RoadModel> {

    // TODO add a special class for equivalence testing between roadmodels. The
    // models should behave exactly the same when traveling over routes which
    // are possible in both models.

    protected static TimeLapse hour() {
        return hour(1);
    }

    protected static TimeLapse hour(long multiplier) {
        return TimeLapseFactory.create(0, 60 * 60 * 1000 * multiplier);
    }

    protected static TimeLapse hour(double multiplier) {
        return TimeLapseFactory.create(0, (long) (60 * 60 * 1000 * multiplier));
    }

    protected static TimeLapse timeLength(long length) {
        return TimeLapseFactory.create(0, length);
    }

    protected final SpeedConverter sc = new SpeedConverter();
    protected final double EPSILON = 0.02;

    protected T model;
    protected Point SW;
    protected Point SE;
    protected Point NE;
    protected Point NW;

    /**
     * must instantiate model and points
     * @throws Exception any exception
     */
    @Before
    public void setUpPoints() throws Exception {
        SW = new Point(0, 0);
        SE = new Point(10, 0);
        NE = new Point(10, 10);
        NW = new Point(0, 10);
        setUp();
    }

    public abstract void setUp() throws Exception;

    @BeforeClass
    public static void assertionCheck() {
        boolean assertionsAreOn = false;
        try {
            assert false;
        } catch (final AssertionError ae) {
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
        final RoadUser ru = new TestRoadUser();
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
        final TestRoadUser driver = new TestRoadUser();
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
        final TestRoadUser driver = new TestRoadUser();
        assertTrue(model.register(driver));
        assertFalse(model.unregister(driver));
        model.addObjectAt(driver, SW);
        assertTrue(model.unregister(driver));
    }

    @Test
    public void getDestination() {
        final TestRoadUser testRoadUser = new TestRoadUser();
        model.addObjectAt(testRoadUser, SW);
        assertNull(model.getDestination(testRoadUser));
        assertNull(model.getDestination(new TestRoadUser()));
        final List<Point> path = model.getShortestPathTo(SW, NW);
        model.followPath(testRoadUser, newLinkedList(path), TimeLapseFactory
                .create(0, 1));
        assertEquals(NW, model.getDestination(testRoadUser));

        model.moveTo(testRoadUser, NE, TimeLapseFactory.create(0, 1));
        assertEquals(NE, model.getDestination(testRoadUser));
    }

    @Test(expected = NullPointerException.class)
    public void followPathFailPath1() {
        final TestRoadUser testRoadUser = new TestRoadUser();
        model.addObjectAt(testRoadUser, SW);
        model.followPath(testRoadUser, null, emptyTimeLapse);
    }

    static TimeLapse emptyTimeLapse = init();

    static TimeLapse init() {
        final TimeLapse tl = TimeLapseFactory.create(0, 1);
        tl.consumeAll();
        return tl;
    }

    @Test(expected = IllegalArgumentException.class)
    public void followPathFailPath2() {
        final TestRoadUser testRoadUser = new TestRoadUser();
        model.addObjectAt(testRoadUser, SW);
        model.followPath(testRoadUser, new LinkedList<Point>(), emptyTimeLapse);
    }

    @Test(expected = IllegalArgumentException.class)
    public void followPathFailTime() {
        final TestRoadUser testRoadUser = new TestRoadUser();
        model.addObjectAt(testRoadUser, SW);
        model.followPath(testRoadUser, new LinkedList<Point>(Arrays.asList(SW)), emptyTimeLapse);
    }

    @Test
    public void getSupportedType() {
        assertEquals(RoadUser.class, model.getSupportedType());
    }

    @Test
    public void testClear() {
        final RoadUser agent1 = new TestRoadUser();
        final RoadUser agent2 = new TestRoadUser();
        final RoadUser agent3 = new TestRoadUser();
        model.addObjectAt(agent1, SW);
        model.addObjectAt(agent2, SE);
        model.addObjectAt(agent3, NE);
        assertEquals(3, model.getObjects().size());
        model.clear();
        assertTrue(model.getObjects().isEmpty());
    }

    @Test
    public void testGetObjectsAndPositions() {
        final RoadUser agent1 = new TestRoadUser();
        final RoadUser agent2 = new TestRoadUser();
        final RoadUser agent3 = new RoadUser() {
            @Override
            public void initRoadUser(RoadModel pModel) {
                // can be ignored in this test [bm]
            }
        };
        model.addObjectAt(agent1, SW);
        model.addObjectAt(agent2, SE);
        model.addObjectAt(agent3, NE);

        final Map<RoadUser, Point> mapCopy = model.getObjectsAndPositions();
        final Set<RoadUser> setCopy = model.getObjects();
        final Set<TestRoadUser> subsetCopy = model
                .getObjectsOfType(TestRoadUser.class);
        final Collection<Point> posCopy = model.getObjectPositions();

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
        final TestRoadUser agent1 = new TestRoadUser();
        final TestRoadUser agent2 = new TestRoadUser();
        final TestRoadUser agent3 = new TestRoadUser();
        final TestRoadUser agent4 = new TestRoadUser();
        final TestRoadUser agent5 = new TestRoadUser();

        model.addObjectAt(agent1, SW);
        model.addObjectAt(agent2, NE);
        model.addObjectAt(agent3, SE);
        model.addObjectAt(agent4, NE);
        model.addObjectAt(agent5, SE);
        assertTrue(Sets
                .difference(asSet(agent1), model.getObjectsAt(agent1, TestRoadUser.class))
                .isEmpty());
        assertTrue(Sets
                .difference(asSet(agent2, agent4), model.getObjectsAt(agent2, TestRoadUser.class))
                .isEmpty());
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
    public void addTruckTest2() {
        final RoadUser t = new TestRoadUser();
        model.addObjectAt(t, new Point(0, 0));
        model.addObjectAt(t, new Point(10, 0));// object is already added
    }

    @Test
    public void removeObjectTest() {
        final RoadUser agent1 = new TestRoadUser();
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
        final TestRoadUser ru = new TestRoadUser();
        model.addObjectAt(ru, SW);
        assertFalse(model.containsObjectAt(ru, new Point(2, 3)));
        assertTrue(model.containsObjectAt(ru, SW));

        model.followPath(ru, asPath(SE), hour(1));
        final Point p = model.getPosition(ru);
        assertTrue(model.containsObjectAt(ru, Point.duplicate(p)));
    }

    @Test
    public void followPathRoundingTimeCheck() {

        // FIXME fix this rounding time bug!
        final MovingRoadUser ru = new SpeedyRoadUser(1);
        model.addObjectAt(ru, SW);

        model.followPath(ru, newLinkedList(asList(SE)), TimeLapseFactory
                .create(0, 36000000 - 1));

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
        final RoadUser agent1 = new TestRoadUser();
        final RoadUser agent2 = new TestRoadUser();
        model.addObjectAt(agent1, SW);
        model.addObjectAtSamePosition(agent2, agent1);
        assertEquals(SW, model.getPosition(agent1));
        assertEquals(SW, model.getPosition(agent2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addObjectAtSamePositionFail() {
        final RoadUser agent1 = new TestRoadUser();
        final RoadUser agent2 = new TestRoadUser();
        model.addObjectAt(agent1, SW);
        model.addObjectAt(agent2, SE);
        model.addObjectAtSamePosition(agent2, agent1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addObjectAtSamePositionFail2() {
        final RoadUser agent1 = new TestRoadUser();
        final RoadUser agent2 = new TestRoadUser();
        final RoadUser agent3 = new TestRoadUser();
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
        if (model instanceof CachedGraphRoadModel) {
            final Table<Point, Point, List<Point>> cache = HashBasedTable
                    .create();
            final List<Point> cachePath = Arrays.asList(SW, NE);
            cache.put(SW, NE, cachePath);

            ((CachedGraphRoadModel) model).setPathCache(cache);

            final List<Point> shortPath = model.getShortestPathTo(SW, NE);

            assertEquals(shortPath, cachePath);

            assertEquals(cache, ((CachedGraphRoadModel) model).getPathCache());
        }
    }

    @Test
    public void testEqualPosition() {
        final RoadUser agent1 = new TestRoadUser();
        final RoadUser agent2 = new TestRoadUser();

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
        new MoveProgress(-1, 1, null);
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void pathProgressConstructorFail2() {
        new MoveProgress(1, -1, null);
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void pathProgressConstructorFail3() {
        new MoveProgress(1, 1, null);
    }

    @Test
    public void testObjectOrder() {
        final List<RoadUser> objects = new ArrayList<RoadUser>();
        final List<Point> positions = Arrays.asList(NE, SE, SW, NE);
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
        final List<RoadUser> modelObjects = new ArrayList<RoadUser>(
                model.getObjects());
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
        final List<RoadUser> modelObjects2 = new ArrayList<RoadUser>(
                model.getObjects());
        assertEquals(objects.size(), modelObjects2.size());
        for (int i = 0; i < modelObjects2.size(); i++) {
            assertTrue(modelObjects2.get(i) == objects.get(i));
        }

        // make sure that the order is preserved, even when using a predicate
        final List<RoadUser> modelObjects3 = new ArrayList<RoadUser>(
                model.getObjects(new Predicate<RoadUser>() {
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
        final List<Entry<RoadUser, Point>> modelObjects4 = new ArrayList<Entry<RoadUser, Point>>(
                model.getObjectsAndPositions().entrySet());
        assertEquals(objects.size(), modelObjects4.size());
        for (int i = 0; i < modelObjects4.size(); i++) {
            assertTrue(modelObjects4.get(i).getKey() == objects.get(i));
        }

        // make sure that the order is preserved, even when using a type
        final List<RoadUser> modelObjects5 = new ArrayList<RoadUser>(
                model.getObjectsOfType(TestRoadUser2.class));
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
