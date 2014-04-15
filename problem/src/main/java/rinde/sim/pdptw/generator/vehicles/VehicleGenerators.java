package rinde.sim.pdptw.generator.vehicles;

import static java.util.Collections.nCopies;
import static rinde.sim.util.SupplierRngs.constant;

import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.VehicleDTO;
import rinde.sim.util.SupplierRng;
import rinde.sim.util.TimeWindow;

import com.google.common.collect.ImmutableList;

public final class VehicleGenerators {
  static SupplierRng<Integer> DEFAULT_NUMBER_OF_VEHICLES = constant(10);
  static SupplierRng<Point> DEFAULT_START_POSITION = constant(new Point(0, 0));
  static SupplierRng<Double> DEFAULT_SPEED = constant(50d);
  static Unit<Velocity> DEFAULT_SPEED_UNIT = NonSI.KILOMETERS_PER_HOUR;
  static SupplierRng<Integer> DEFAULT_CAPACITY = constant(1);
  // TODO what about time unit?
  static SupplierRng<TimeWindow> DEFAULT_TIME_WINDOW = constant(TimeWindow.ALWAYS);
  static SupplierRng<Long> DEFAULT_TIME = constant(-1L);

  private VehicleGenerators() {}

  public static HomogenousBuilder homogenous(VehicleDTO dto) {
    return new HomogenousBuilder(dto);
  }

  public static class HomogenousBuilder {
    private final VehicleDTO dto;
    private int numberOfVehicles;
    private Unit<Velocity> speedUnit;

    HomogenousBuilder(VehicleDTO d) {
      dto = d;
      numberOfVehicles = 10;
      speedUnit = DEFAULT_SPEED_UNIT;
    }

    public HomogenousBuilder numberOfVehicles(int num) {
      numberOfVehicles = num;
      return this;
    }

    public HomogenousBuilder speedUnit(Unit<Velocity> unit) {
      speedUnit = unit;
      return this;
    }

    public VehicleGenerator build() {
      return new HomogenousVehicleGenerator(numberOfVehicles, dto, speedUnit);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
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

  private static class HomogenousVehicleGenerator implements VehicleGenerator {
    private final VehicleDTO vehicleDto;
    private final int n;
    private final Unit<Velocity> speedUnit;

    HomogenousVehicleGenerator(int numberOfVehicles, VehicleDTO dto,
        Unit<Velocity> su) {
      vehicleDto = dto;
      n = numberOfVehicles;
      speedUnit = su;
    }

    @Override
    public ImmutableList<AddVehicleEvent> generate(RandomGenerator rng) {
      return ImmutableList
          .copyOf(nCopies(n, new AddVehicleEvent(-1, vehicleDto)));
    }

    @Override
    public Unit<Velocity> getSpeedUnit() {
      return speedUnit;
    }
  }
}
