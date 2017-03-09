/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.core.model.road;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.road.GenericRoadModel.RoadEventType;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.event.ListenerEventHistory;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.PointTestUtil;
import com.github.rinde.rinsim.util.TrivialRoadUser;
import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @param <T> the type of RoadModel to test
 *
 */
public abstract class AbstractRoadModelTest<T extends GenericRoadModel> {

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

  protected static Measure<Double, Length> meter(double multiplier) {
    return Measure.valueOf(multiplier, SI.METER);
  }

  protected static Measure<Long, Duration> durationSecond(long s) {
    return Measure.valueOf(s, SI.SECOND);
  }

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
    doSetUp();
  }

  public abstract void doSetUp() throws Exception;

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

  @Test
  public void register() {
    final TestRoadUser driver = new TestRoadUser();
    model.register(driver);
    // this is important for checking whether a decorated RoadModel actually
    // sends its decorated reference to RoadUsers
    assertEquals(model, driver.getRoadModel());
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
    model.followPath(testRoadUser, newLinkedList(path),
      TimeLapseFactory.create(0, 1));
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
    model.followPath(testRoadUser, new LinkedList<Point>(Arrays.asList(SW)),
      emptyTimeLapse);
  }

  @Test
  public void getSupportedType() {
    assertEquals(RoadUser.class, model.getSupportedType());
  }

  @Test
  public void testMoveToDistance() {
    final TestRoadUser testRoadUser = new TestRoadUser();
    model.addObjectAt(testRoadUser, SW);

    final Point start = model.getPosition(testRoadUser);
    final MoveProgress mp = model.moveTo(testRoadUser, NW, timeLength(3));
    final Point end = model.getPosition(testRoadUser);

    final double lineDist = Point.distance(start, end);
    assertThat(lineDist).isWithin(EPSILON)
      .of(mp.distance().doubleValue(model.getDistanceUnit()));
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
    assertTrue(Sets.difference(asSet(agent1),
      model.getObjectsAt(agent1, TestRoadUser.class)).isEmpty());
    assertTrue(Sets.difference(asSet(agent2, agent4),
      model.getObjectsAt(agent2, TestRoadUser.class)).isEmpty());
    assertTrue(model.getObjectsAt(agent2, SpeedyRoadUser.class).isEmpty());
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

  @Test
  public void containsObjectAt() {
    assertFalse(model.containsObjectAt(new TestRoadUser(), new Point(2, 3)));
    final TestRoadUser ru = new TestRoadUser();
    model.addObjectAt(ru, SW);
    assertFalse(model.containsObjectAt(ru, new Point(2, 3)));
    assertTrue(model.containsObjectAt(ru, SW));

    model.followPath(ru, asPath(SE), hour(1));
    final Point p = model.getPosition(ru);
    assertTrue(model.containsObjectAt(ru, PointTestUtil.duplicate(p)));
  }

  @Test
  public void followPathRoundingTimeCheck() {
    // FIXME fix this rounding time bug!
    final MovingRoadUser ru = new SpeedyRoadUser(1d);
    model.addObjectAt(ru, SW);

    model.followPath(ru, newLinkedList(asList(SE)),
      TimeLapseFactory.create(0, 36000000 - 1));
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
      final Table<Point, Point, List<Point>> cache = HashBasedTable.create();
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

  @SuppressWarnings({"unused", "null"})
  @Test(expected = IllegalArgumentException.class)
  public void pathProgressConstructorFail1() {
    MoveProgress.create(meter(-1), durationSecond(1), new ArrayList<Point>());
  }

  @SuppressWarnings({"unused", "null"})
  @Test(expected = IllegalArgumentException.class)
  public void pathProgressConstructorFail2() {
    MoveProgress.create(meter(1), durationSecond(-1), new ArrayList<Point>());
  }

  @Test
  public void moveToEventIssuerType() {
    final MovingRoadUser user = new TestRoadUser();
    model.addObjectAt(user, SW);

    final ListenerEventHistory list = new ListenerEventHistory();
    model.getEventAPI().addListener(list, RoadEventType.MOVE);
    assertTrue(list.getHistory().isEmpty());
    model.moveTo(user, NW, TimeLapseFactory.create(0, 10));

    assertEquals(1, list.getHistory().size());

    assertEquals(RoadEventType.MOVE, list.getHistory().get(0).getEventType());
    assertEquals(model, list.getHistory().get(0).getIssuer());

  }

  @Test
  public void followPathEventIssuerType() {
    final MovingRoadUser user = new TestRoadUser();
    model.addObjectAt(user, SW);

    final ListenerEventHistory list = new ListenerEventHistory();
    model.getEventAPI().addListener(list, RoadEventType.MOVE);
    assertTrue(list.getHistory().isEmpty());
    model.followPath(user, newLinkedList(asList(SW, SE, NE, NW)),
      TimeLapseFactory.create(0, 10));

    assertEquals(1, list.getHistory().size());

    assertEquals(RoadEventType.MOVE, list.getHistory().get(0).getEventType());
    assertEquals(model, list.getHistory().get(0).getIssuer());
  }

  /**
   * Tests whether the iteration order in the model is according to insertion
   * ordering.
   */
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
      assertSame(modelObjects.get(i), objects.get(i));
    }

    model.removeObject(objects.remove(97));
    model.removeObject(objects.remove(67));
    model.removeObject(objects.remove(44));
    model.removeObject(objects.remove(13));
    model.removeObject(objects.remove(3));

    // check to see if the objects are still in insertion order, even after
    // removals
    final List<RoadUser> modelObjects2 = new ArrayList<RoadUser>(
      model.getObjects());
    assertEquals(objects.size(), modelObjects2.size());
    for (int i = 0; i < modelObjects2.size(); i++) {
      assertSame(modelObjects2.get(i), objects.get(i));
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
      assertSame(modelObjects3.get(i), objects.get(i));
    }

    // make sure that the order of the objects is preserved, even in this
    // RoadUser, Point map
    final List<Entry<RoadUser, Point>> modelObjects4 =
      new ArrayList<Entry<RoadUser, Point>>(
        model.getObjectsAndPositions().entrySet());
    assertEquals(objects.size(), modelObjects4.size());
    for (int i = 0; i < modelObjects4.size(); i++) {
      assertSame(modelObjects4.get(i).getKey(), objects.get(i));
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
      assertSame(modelObjects5.get(i), objects.get(j));
      j++;
    }

  }

  /**
   * Tests whether decoration works correctly.
   */
  @Test
  public void decoration() {
    assertSame(model, model.self);
    final GenericRoadModel a = new ForwardingRoadModel(model);
    assertSame(a, model.self);
    assertSame(a, a.self);

    final GenericRoadModel b = new ForwardingRoadModel(a);
    assertSame(b, model.self);
    assertSame(b, a.self);
    assertSame(b, b.self);

    final GenericRoadModel c = new ForwardingRoadModel(b);
    assertSame(c, model.self);
    assertSame(c, a.self);
    assertSame(c, b.self);
    assertSame(c, c.self);
  }

  /**
   * Tests whether decoration fails when already initialized.
   */
  @SuppressWarnings("unused")
  @Test
  public void decorationFail() {
    model.register(new RoadUser() {
      @Override
      public void initRoadUser(RoadModel m) {}
    });

    boolean fail = false;
    try {
      new ForwardingRoadModel(model);
    } catch (final IllegalStateException e) {
      fail = true;
    }
    assertTrue(fail);
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

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + speed + ")";
  }

}

class TestRoadUser extends TrivialRoadUser {
  final String name;

  TestRoadUser() {
    this("");
  }

  TestRoadUser(String n) {
    name = n;
  }

  @Override
  public String toString() {
    if (name.isEmpty()) {
      return super.toString();
    }
    return name;
  }
}

class TestRoadUser2 extends TrivialRoadUser {}
