package rinde.sim.pdptw.generator.vehicles;

import static rinde.sim.util.SupplierRngs.constant;

import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.VehicleDTO;
import rinde.sim.util.SupplierRng;
import rinde.sim.util.TimeWindow;

public final class VehicleGenerators {

  private VehicleGenerators() {}

  public static VehicleGenerator homogenous(int num, VehicleDTO dto) {
    return null;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    static SupplierRng<Integer> DEFAULT_NUMBER_OF_VEHICLES = constant(10);
    static SupplierRng<Point> DEFAULT_START_POSITION = constant(new Point(0, 0));
    static SupplierRng<Double> DEFAULT_SPEED = constant(50d);
    static Unit<Velocity> DEFAULT_SPEED_UNIT = NonSI.KILOMETERS_PER_HOUR;
    static SupplierRng<Integer> DEFAULT_CAPACITY = constant(1);
    // TODO what about time unit?
    static SupplierRng<TimeWindow> DEFAULT_TIME_WINDOW = constant(TimeWindow.ALWAYS);
    static SupplierRng<Long> DEFAULT_TIME = constant(-1L);

    SupplierRng<Integer> numberOfVehicles;
    SupplierRng<Point> startPositionGenerator;
    SupplierRng<Double> speedGenerator;
    Unit<Velocity> speedUnit;
    SupplierRng<Integer> capacityGenerator;
    SupplierRng<TimeWindow> timeWindowGenerator;
    SupplierRng<Long> creationTimeGenerator;

    Builder() {
      numberOfVehicles = DEFAULT_NUMBER_OF_VEHICLES;
      startPositionGenerator = DEFAULT_START_POSITION;
      speedGenerator = DEFAULT_SPEED;
      speedUnit = DEFAULT_SPEED_UNIT;
      capacityGenerator = DEFAULT_CAPACITY;
      timeWindowGenerator = DEFAULT_TIME_WINDOW;
      creationTimeGenerator = DEFAULT_TIME;
    }

  }

}
