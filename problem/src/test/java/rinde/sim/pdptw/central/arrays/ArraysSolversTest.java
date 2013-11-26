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
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.DefaultPDPModel;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.pdptw.central.Solvers;
import rinde.sim.pdptw.central.Solvers.SimulationConverter;
import rinde.sim.pdptw.central.Solvers.SolveArgs;
import rinde.sim.pdptw.central.Solvers.StateContext;
import rinde.sim.pdptw.central.arrays.ArraysSolvers.ArraysObject;
import rinde.sim.pdptw.common.DefaultDepot;
import rinde.sim.pdptw.common.DefaultParcel;
import rinde.sim.pdptw.common.PDPRoadModel;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.common.RouteFollowingVehicle;
import rinde.sim.pdptw.common.VehicleDTO;
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

  /**
   * Tests whether parcel that have the same location as origin and destinations
   * are treated correctly by the conversion script.
   */
  @Test
  public void testToInventoriesArrayWithDuplicatePositions() {

    final Simulator sim = new Simulator(new MersenneTwister(123),
        Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));

    sim.register(new DefaultPDPModel());
    sim.register(new PDPRoadModel(new PlaneRoadModel(new Point(0, 0),
        new Point(10, 10), 50), false));
    sim.configure();

    final RouteFollowingVehicle rfv = new RouteFollowingVehicle(new VehicleDTO(
        new Point(1, 1), 50, 10, new TimeWindow(0, 1000000)), false);
    final Depot depot = new DefaultDepot(new Point(5, 5));

    final DefaultParcel dp1 = new DefaultParcel(new ParcelDTO(new Point(2, 2),
        new Point(3, 3), new TimeWindow(0, 1000), new TimeWindow(0, 1000), 0,
        0L, 5L, 5L));
    final DefaultParcel dp2 = new DefaultParcel(new ParcelDTO(new Point(2, 2),
        new Point(3, 3), new TimeWindow(0, 1000), new TimeWindow(0, 1000), 0,
        0L, 5L, 5L));

    sim.register(depot);
    sim.register(rfv);
    sim.register(dp1);
    sim.register(dp2);

    rfv.setRoute(asList(dp1, dp2));

    while (rfv.getRoute().size() > 0) {
      sim.tick();
    }

    final SimulationConverter simConv = Solvers.converterBuilder().with(sim)
        .build();
    final StateContext sc = simConv.convert(SolveArgs.create()
        .noCurrentRoutes().useAllParcels());

    final ArraysObject singleVehicleArrays = ArraysSolvers
        .toSingleVehicleArrays(sc.state,
            SI.MILLI(SI.SECOND));

    final int[][] inventories = ArraysSolvers.toInventoriesArray(sc.state,
        singleVehicleArrays);

    assertArrayEquals(new int[][] { { 0, 1 }, { 0, 2 } }, inventories);
  }
}
