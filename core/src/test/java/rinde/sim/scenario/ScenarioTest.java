/**
 * 
 */
package rinde.sim.scenario;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static rinde.sim.scenario.ScenarioTest.TestEvents.EVENT_A;
import static rinde.sim.scenario.ScenarioTest.TestEvents.EVENT_B;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import rinde.sim.core.graph.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ScenarioTest {

  enum TestEvents {
    EVENT_A, EVENT_B;
  }

  @Test
  public void testEquals() {
    final List<TimedEvent> events1 = newArrayList(new TimedEvent(EVENT_A, 0));
    final List<TimedEvent> events2 = newArrayList(new TimedEvent(EVENT_A, 0));
    final List<TimedEvent> events3 = newArrayList(new TimedEvent(EVENT_A, 1));
    final List<TimedEvent> events4 = newArrayList(new TimedEvent(EVENT_A, 1),
        new TimedEvent(
            EVENT_A, 2));

    final Scenario s1 = Scenario.builder().addEvents(events1).build();
    final Scenario s2 = Scenario.builder().addEvents(events2).build();
    final Scenario s3 = Scenario.builder().addEvents(events3).build();
    final Scenario s4 = Scenario.builder().addEvents(events4).build();

    assertNotEquals(s1, new Object());
    assertEquals(s1, s2);
    assertNotEquals(s1, s3);
    assertNotEquals(s1, s4);
  }

  @Test
  public void testSorting() {
    final List<TimedEvent> events = new ArrayList<TimedEvent>(10);
    final AddObjectEvent A1 = new AddObjectEvent(0, new Point(1, 0));
    final AddObjectEvent A2 = new AddObjectEvent(0, new Point(2, 0));
    final AddObjectEvent B = new AddObjectEvent(1, new Point(1, 1));
    final AddObjectEvent C = new AddObjectEvent(2, new Point(1, 0));
    final AddObjectEvent D1 = new AddObjectEvent(3, new Point(1, 2));
    final AddObjectEvent D2 = new AddObjectEvent(3, new Point(1, 3));
    final AddObjectEvent E = new AddObjectEvent(4, new Point(2, 0));
    final AddObjectEvent F = new AddObjectEvent(5, new Point(4, 0));
    events.addAll(asList(A1, A2, B, C, D1, D2, E, F));
    Collections.reverse(events);

    final Scenario s = Scenario.builder().addEvents(events).build();
    final List<TimedEvent> res = newArrayList(s.asList());

    assertEquals(asList(A2, A1, B, C, D2, D1, E, F), res);
    assertFalse(res.equals(events));
    assertEquals(events.size(), res.size());
    Collections.reverse(res);
    assertEquals(res, events);
  }

  // @SuppressWarnings("unused")
  // @Test(expected = IllegalArgumentException.class)
  // public void constructorFail1() {
  // final List<TimedEvent> events = newArrayList();
  // new Scenario(events, new HashSet<Enum<?>>());
  // }
  //
  // @SuppressWarnings("unused")
  // @Test(expected = IllegalArgumentException.class)
  // public void constructorFail2() {
  // new Scenario(asList(new TimedEvent(EVENT_A, 1L)),
  // new HashSet<Enum<?>>());
  // }
  //
  // @SuppressWarnings("unused")
  // @Test(expected = IllegalArgumentException.class)
  // public void constructorFail3() {
  // new Scenario(new ArrayList<TimedEvent>());
  // }

  @Test
  public void testCreateScenarioByCopying() {

    final Scenario s = Scenario.builder()
        .addEventType(EVENT_A)
        .addEvent(new AddObjectEvent(100, new Point(0, 0)))
        .addEvent(new AddObjectEvent(200, new Point(0, 0)))
        .addEvent(new AddObjectEvent(300, new Point(0, 0)))
        .build();

    assertEquals(3, s.asList().size());

    final Scenario s2 = Scenario.builder(s).build();

    assertEquals(3, s.asList().size());
    assertEquals(3, s2.asList().size());

    // assertEquals(s.peek(), s2.peek());
    // final TimedEvent sP0 = s.poll();
    //
    // assertEquals(2, s.asList().size());
    // assertEquals(3, s2.asList().size());
    //
    // final TimedEvent s2P0 = s2.poll();
    //
    // assertEquals(2, s.asList().size());
    // assertEquals(2, s2.asList().size());
    //
    // assertEquals(sP0, s2P0);

  }

  @Test
  public void timedEventEquals() {
    assertFalse(new AddObjectEvent(10, new Point(10, 0))
        .equals(new TimedEvent(EVENT_A, 10)));
    assertFalse(new TimedEvent(EVENT_A, 10).equals(null));
    assertFalse(new TimedEvent(EVENT_A, 10).equals(new TimedEvent(EVENT_B,
        10)));
    assertTrue(new TimedEvent(EVENT_B, 10).equals(new TimedEvent(EVENT_B,
        10)));
  }

  public static Scenario randomScenario(RandomGenerator gen, int numTrucks,
      List<Point> positions) {
    final ScenarioBuilder res = new ScenarioBuilder(EVENT_A);
    final int size = positions.size();
    for (int i = 0; i < numTrucks; i++) {
      res.addEvent(new AddObjectEvent(0L,
          positions.get(gen.nextInt(size))));
    }
    return res.build();
  }
}

class AddObjectEvent extends TimedEvent {

  private static final long serialVersionUID = 5946753206998904050L;

  public final Point pos;

  public AddObjectEvent(String[] parts) {
    this(Long.parseLong(parts[1]), Point.parsePoint(parts[2]));
  }

  public AddObjectEvent(long pTime, Point pPos) {
    super(EVENT_A, pTime);
    pos = pPos;
    hashCode();
    toString();
  }

  @Override
  public String toString() {
    return super.toString() + "|" + pos;
  }

}
