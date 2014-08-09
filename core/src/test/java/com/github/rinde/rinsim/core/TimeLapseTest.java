/**
 * 
 */
package com.github.rinde.rinsim.core;

import static com.github.rinde.rinsim.core.TimeLapseFactory.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.measure.unit.SI;

import org.junit.Test;

import com.github.rinde.rinsim.core.TimeLapse;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class TimeLapseTest {

  @Test
  public void unitConstructor() {
    final TimeLapse tl = new TimeLapse(SI.SECOND);
    assertEquals(0, tl.getTime());
    assertEquals(0, tl.getStartTime());
    assertEquals(0, tl.getEndTime());
    assertEquals(SI.SECOND, tl.getTimeUnit());
  }

  @Test
  public void constructor() {
    final TimeLapse tl = create(0, 10);

    assertEquals(0, tl.getTime());
    assertEquals(0, tl.getTimeConsumed());
    assertEquals(10, tl.getTimeStep());
    assertEquals(10, tl.getTimeLeft());
    assertTrue(tl.hasTimeLeft());

  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void constructorFail1() {
    create(-1, 0);
  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void constructorFail2() {
    create(1, 0);
  }

  @Test
  public void consume1() {

    final int[] start = { 0, 10, 100, 500 };
    final int[] end = { 100, 1000, 113, 783 };

    for (int i = 0; i < start.length; i++) {
      final TimeLapse tl = create(start[i], end[i]);
      assertEquals(end[i] - start[i], tl.getTimeLeft());
      assertEquals(start[i], tl.getTime());
      assertEquals(0, tl.getTimeConsumed());
      assertTrue(tl.hasTimeLeft());
      assertEquals(end[i] - start[i], tl.getTimeStep());

      tl.consume(10);
      assertEquals(end[i] - start[i] - 10, tl.getTimeLeft());
      assertEquals(start[i] + 10, tl.getTime());
      assertEquals(10, tl.getTimeConsumed());
      assertTrue(tl.hasTimeLeft());
      assertEquals(end[i] - start[i], tl.getTimeStep());

      tl.consumeAll();
      assertEquals(0, tl.getTimeLeft());
      assertEquals(end[i], tl.getTime());
      assertEquals(end[i] - start[i], tl.getTimeConsumed());
      assertFalse(tl.hasTimeLeft());
      assertEquals(end[i] - start[i], tl.getTimeStep());
    }

  }

  @Test(expected = IllegalArgumentException.class)
  public void consumeFail1() {
    final TimeLapse tl = create(0, 10);
    tl.consume(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void consumeFail2() {
    final TimeLapse tl = create(0, 10);
    tl.consume(11);
  }

}
