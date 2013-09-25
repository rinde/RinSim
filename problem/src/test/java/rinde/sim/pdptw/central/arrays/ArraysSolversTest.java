/**
 * 
 */
package rinde.sim.pdptw.central.arrays;

import static java.util.Arrays.asList;
import static javax.measure.unit.NonSI.KILOMETERS_PER_HOUR;
import static javax.measure.unit.NonSI.MINUTE;
import static javax.measure.unit.SI.KILOMETER;
import static javax.measure.unit.SI.METER;
import static javax.measure.unit.SI.MILLI;
import static javax.measure.unit.SI.SECOND;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static rinde.sim.pdptw.central.arrays.ArraysSolvers.convertTW;

import java.math.RoundingMode;

import javax.measure.Measure;
import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Velocity;
import javax.measure.unit.ProductUnit;

import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ArraysSolversTest {

  @Test
  public void travelTimeMatrix() {
    final Point p0 = new Point(0, 0);
    final Point p1 = new Point(10, 0);
    final Point p2 = new Point(10, 10);
    final Point p3 = new Point(0, 10);

    // input in kilometers, output in minutes (rounded up), speed 40 km/h
    final Measure<Double, Velocity> speed1 = Measure.valueOf(40d,
        KILOMETERS_PER_HOUR);
    final int[][] matrix1 = ArraysSolvers
        .toTravelTimeMatrix(asList(p0, p1, p2, p3), KILOMETER, speed1, MINUTE,
            RoundingMode.CEILING);
    assertArrayEquals(new int[] { 0, 15, 22, 15 }, matrix1[0]);
    assertArrayEquals(new int[] { 15, 0, 15, 22 }, matrix1[1]);
    assertArrayEquals(new int[] { 22, 15, 0, 15 }, matrix1[2]);
    assertArrayEquals(new int[] { 15, 22, 15, 0 }, matrix1[3]);

    final Point p4 = new Point(11, 3);
    // input in meters, output in milliseconds (round down), speed .0699
    // m/ms
    final Measure<Double, Velocity> speed2 = Measure.valueOf(.0699,
        new ProductUnit<Velocity>(METER.divide(MILLI(SECOND))));
    final int[][] matrix2 = ArraysSolvers.toTravelTimeMatrix(
        asList(p0, p1, p2, p3, p4), METER, speed2, MILLI(SECOND),
        RoundingMode.FLOOR);
    assertArrayEquals(new int[] { 0, 143, 202, 143, 163 }, matrix2[0]);
    assertArrayEquals(new int[] { 143, 0, 143, 202, 45 }, matrix2[1]);
    assertArrayEquals(new int[] { 202, 143, 0, 143, 101 }, matrix2[2]);
    assertArrayEquals(new int[] { 143, 202, 143, 0, 186 }, matrix2[3]);
    assertArrayEquals(new int[] { 163, 45, 101, 186, 0 }, matrix2[4]);
  }

  @Test
  public void convertTWtest() {
    final UnitConverter timeConverter = MILLI(SECOND).getConverterTo(SECOND);

    final int[] tw1 = convertTW(new TimeWindow(300, 800), 5, timeConverter);
    assertEquals(0, tw1[0]);
    assertEquals(1, tw1[1]);

    final int[] tw2 = convertTW(new TimeWindow(7300, 8800), 0, timeConverter);
    assertEquals(8, tw2[0]);
    assertEquals(8, tw2[1]);

    final int[] tw3 = convertTW(new TimeWindow(7300, 8800), 7300, timeConverter);
    assertEquals(0, tw3[0]);
    assertEquals(1, tw3[1]);
  }

  /**
   * Checks correctness of tardiness computation. Also checks whether the
   * arrival time at the current position is correctly ignored when calculating
   * tardiness.
   */
  @Test
  public void computeSumTardinessTest() {
    final int[] route = new int[] { 0, 1, 2, 3 };
    final int[] arrivalTimes = new int[] { 50, 70, 90, 100 };
    final int[] serviceTimes = new int[] { 0, 5, 5, 0 };
    final int[] dueDates = new int[] { 40, 70, 80, 110 };
    final int tardiness = ArraysSolvers.computeRouteTardiness(route,
        arrivalTimes, serviceTimes, dueDates, 0);
    assertEquals(20, tardiness);
  }
}
