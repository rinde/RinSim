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

import java.math.RoundingMode;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.ProductUnit;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.central.arrays.ArraysSolvers;

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
        final Measure<Double, Velocity> speed1 = Measure
                .valueOf(40d, KILOMETERS_PER_HOUR);
        final int[][] matrix1 = ArraysSolvers
                .toTravelTimeMatrix(asList(p0, p1, p2, p3), KILOMETER, speed1, MINUTE, RoundingMode.CEILING);
        assertArrayEquals(new int[] { 0, 15, 22, 15 }, matrix1[0]);
        assertArrayEquals(new int[] { 15, 0, 15, 22 }, matrix1[1]);
        assertArrayEquals(new int[] { 22, 15, 0, 15 }, matrix1[2]);
        assertArrayEquals(new int[] { 15, 22, 15, 0 }, matrix1[3]);

        final Point p4 = new Point(11, 3);
        // input in meters, output in milliseconds (round down), speed .0699
        // m/ms
        final Measure<Double, Velocity> speed2 = Measure
                .valueOf(.0699, new ProductUnit<Velocity>(METER
                        .divide(MILLI(SECOND))));
        final int[][] matrix2 = ArraysSolvers
                .toTravelTimeMatrix(asList(p0, p1, p2, p3, p4), METER, speed2, MILLI(SECOND), RoundingMode.FLOOR);
        assertArrayEquals(new int[] { 0, 143, 202, 143, 163 }, matrix2[0]);
        assertArrayEquals(new int[] { 143, 0, 143, 202, 45 }, matrix2[1]);
        assertArrayEquals(new int[] { 202, 143, 0, 143, 101 }, matrix2[2]);
        assertArrayEquals(new int[] { 143, 202, 143, 0, 186 }, matrix2[3]);
        assertArrayEquals(new int[] { 163, 45, 101, 186, 0 }, matrix2[4]);
    }

    @Test
    public void test() {

        final Measure<Double, Velocity> speed = Measure
                .valueOf(40d, NonSI.KILOMETERS_PER_HOUR);

        final Measure<Double, Length> distance = Measure
                .valueOf(100d, SI.KILOMETER);

        final Measure<Double, Length> meters = distance.to(SI.METER);

        System.out.println(speed.getUnit().getStandardUnit());

        // m/s
        final double ms = speed.doubleValue(SI.METERS_PER_SECOND);
        final double m = distance.doubleValue(SI.METER);

        final Measure<Double, Duration> duration = Measure
                .valueOf(m / ms, SI.SECOND);

        System.out.println(duration.doubleValue(NonSI.HOUR));

        final Unit<?> conv = SI.KILOMETER.divide(NonSI.KILOMETERS_PER_HOUR)
                .times(100d);

        System.out.println(conv.getConverterTo(NonSI.HOUR).convert(40d));

        System.out.println(NonSI.KILOMETERS_PER_HOUR.divide(distance
                .doubleValue(SI.KILOMETER)));
        // final Unit<?> conv =
        // SI.KILOMETER.divide(NonSI.KILOMETERS_PER_HOUR).times(distance);
        // speed.to(conv)
    }
}
