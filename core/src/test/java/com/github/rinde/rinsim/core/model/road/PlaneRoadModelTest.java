/**
 * 
 */
package com.github.rinde.rinsim.core.model.road;

import static com.google.common.collect.Lists.newLinkedList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Queue;

import javax.measure.Measure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import com.github.rinde.rinsim.core.graph.Point;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class PlaneRoadModelTest extends AbstractRoadModelTest<PlaneRoadModel> {

  @Override
  public void setUp() {
    model = new PlaneRoadModel(new Point(0, 0), new Point(10, 10),
        SI.KILOMETER, Measure.valueOf(10d, NonSI.KILOMETERS_PER_HOUR));
  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void constructorFail1() {
    new PlaneRoadModel(new Point(1, 0), new Point(0, 1), SI.KILOMETER,
        Measure.valueOf(10d, NonSI.KILOMETERS_PER_HOUR));
  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void constructorFail2() {
    new PlaneRoadModel(new Point(0, 1), new Point(1, 0), SI.KILOMETER,
        Measure.valueOf(10d, NonSI.KILOMETERS_PER_HOUR));
  }

  @Test
  public void checkPointIsInBoundary() {
    assertTrue(model.isPointInBoundary(new Point(0, 0)));
    assertTrue(model.isPointInBoundary(new Point(10, 10)));
    assertTrue(model.isPointInBoundary(new Point(0, 10)));
    assertTrue(model.isPointInBoundary(new Point(10, 0)));
    assertTrue(model.isPointInBoundary(new Point(5, 5)));
    assertTrue(model.isPointInBoundary(new Point(0, 3)));

    assertFalse(model.isPointInBoundary(new Point(-1, 5)));
    assertFalse(model.isPointInBoundary(new Point(11, 5)));
    assertFalse(model.isPointInBoundary(new Point(5, -234)));
    assertFalse(model.isPointInBoundary(new Point(5, 10.00001)));

    assertFalse(model.isPointInBoundary(new Point(-5, -0.0001)));
    assertFalse(model.isPointInBoundary(new Point(14, -0.0009)));
    assertFalse(model.isPointInBoundary(new Point(100, 100)));
    assertFalse(model.isPointInBoundary(new Point(-100, 100)));

    final RandomGenerator rnd = new MersenneTwister(123);
    for (int i = 0; i < 100; i++) {
      final Point p = model.getRandomPosition(rnd);
      assertTrue(model.isPointInBoundary(p));
    }
  }

  @Test
  public void followPath() {
    final MovingRoadUser mru = new TestRoadUser();
    model.addObjectAt(mru, new Point(0, 0));
    final Queue<Point> path = asPath(new Point(0, 0), new Point(5, 0), new Point(
        5, 5));

    final MoveProgress pp = model.followPath(mru, path, hour());
    assertEquals(asPath(new Point(5, 0), new Point(5, 5)), path);
    assertEquals(1, pp.distance.getValue(), EPSILON);
    assertEquals(hour().getTimeStep(), pp.time.getValue(), EPSILON);
    assertEquals(asList(new Point(0, 0)), pp.travelledNodes);
    assertTrue(Point.distance(new Point(1, 0), model.getPosition(mru)) < EPSILON);

    final MoveProgress pp2 = model.followPath(mru, path, hour(5));
    assertEquals(asPath(new Point(5, 5)), path);
    assertEquals(5, pp2.distance.getValue(), EPSILON);
    assertEquals(hour(5).getTimeStep(), pp2.time.getValue(), EPSILON);
    assertEquals(asList(new Point(5, 0)), pp2.travelledNodes);
    assertTrue(Point.distance(new Point(5, 1), model.getPosition(mru)) < EPSILON);

    final MoveProgress pp3 = model.followPath(mru, path, hour(50));
    assertTrue(path.isEmpty());
    assertEquals(4, pp3.distance.getValue(), EPSILON);
    assertEquals(hour(4).getTimeStep(), pp3.time.getValue(), EPSILON);
    assertEquals(asList(new Point(5, 5)), pp3.travelledNodes);
    assertTrue(Point.distance(new Point(5, 5), model.getPosition(mru)) < EPSILON);
  }

  @Test(expected = IllegalArgumentException.class)
  public void followPathFail() {
    final Queue<Point> path = asPath(new Point(0, 0), new Point(5, 0), new Point(
        5, 5), new Point(100, 0));
    final MovingRoadUser mru = new TestRoadUser();
    model.addObjectAt(mru, new Point(0, 0));
    model.followPath(mru, path, hour(100));
  }

  @Test
  public void moveTo() {
    final MovingRoadUser agent = new SpeedyRoadUser(1);

    model.addObjectAt(agent, SW);
    assertEquals(new Point(0, 0), model.getPosition(agent));

    model.moveTo(agent, NW, hour(9));
    assertTrue(Point.distance(model.getPosition(agent), new Point(0, 9)) < EPSILON);

    model.followPath(agent, newLinkedList(asList(SW, SE)), hour(10));
    assertTrue(Point.distance(model.getPosition(agent), new Point(1, 0)) < EPSILON);

    model.moveTo(agent, SE, hour(4));
    assertTrue(Point.distance(model.getPosition(agent), new Point(5, 0)) < EPSILON);

    model.moveTo(agent, NW, hour(Math.sqrt(5)));
    assertTrue(Point.distance(model.getPosition(agent), new Point(4, 2)) < EPSILON);

    model.moveTo(agent, new Point(6, 2), hour(2));
    assertTrue(Point.distance(model.getPosition(agent), new Point(6, 2)) < EPSILON);

    model.followPath(agent, newLinkedList(asList(NE)), hour(Math.sqrt(5)));
    assertTrue(Point.distance(model.getPosition(agent), new Point(7, 4)) < EPSILON);

    model.moveTo(agent, NW, hour(13));
    assertTrue(Point.distance(model.getPosition(agent), new Point(0, 10)) < EPSILON);

    final MovingRoadUser agent2 = new SpeedyRoadUser(1);
    model.addObjectAt(agent2, SW);
    assertEquals(new Point(0, 0), model.getPosition(agent2));

    model.moveTo(agent2, agent, hour(30));
    assertEquals(model.getPosition(agent), model.getPosition(agent2));
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
    assertEquals(asList(new Point(0, 0), new Point(5, 5)), model
        .getShortestPathTo(new Point(0, 0), new Point(5, 5)));
  }

}
