package rinde.sim.pdptw.generator.vehicles;

import static java.util.Collections.nCopies;
import static rinde.sim.util.SupplierRngs.constant;

import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.VehicleDTO;
import rinde.sim.util.SupplierRng;
import rinde.sim.util.TimeWindow;

import com.google.common.collect.ImmutableList;

public final class VehicleGenerators {
  static final SupplierRng<Integer> DEFAULT_NUMBER_OF_VEHICLES = constant(10);
  static final SupplierRng<Point> DEFAULT_START_POSITION = constant(new Point(
      0, 0));
  static final SupplierRng<Double> DEFAULT_SPEED = constant(50d);
  static final Unit<Velocity> DEFAULT_SPEED_UNIT = NonSI.KILOMETERS_PER_HOUR;
  static final SupplierRng<Integer> DEFAULT_CAPACITY = constant(1);
  static final SupplierRng<TimeWindow> DEFAULT_TIME_WINDOW = constant(TimeWindow.ALWAYS);
  static final SupplierRng<Long> DEFAULT_TIME = constant(-1L);

  private VehicleGenerators() {}

  public static HomogenousBuilder homogenous(VehicleDTO dto) {
    return new HomogenousBuilder(dto);
  }

  public static class HomogenousBuilder {
    private final VehicleDTO dto;
    private int numberOfVehicles;

    HomogenousBuilder(VehicleDTO d) {
      dto = d;
      numberOfVehicles = 10;
    }

    public HomogenousBuilder numberOfVehicles(int num) {
      numberOfVehicles = num;
      return this;
    }

    public VehicleGenerator build() {
      return new HomogenousVehicleGenerator(numberOfVehicles, dto);
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

      // FIXME use scenario length? time unit?
      timeWindowGenerator = DEFAULT_TIME_WINDOW;
      creationTimeGenerator = DEFAULT_TIME;
    }
  }

  private static class HomogenousVehicleGenerator implements VehicleGenerator {
    private final VehicleDTO vehicleDto;
    private final int n;
    private final RandomGenerator rng;

    HomogenousVehicleGenerator(int numberOfVehicles, VehicleDTO dto) {
      vehicleDto = dto;
      n = numberOfVehicles;
      rng = new MersenneTwister();
    }

    @Override
    public ImmutableList<AddVehicleEvent> generate(long seed) {
      rng.setSeed(seed);
      return ImmutableList
          .copyOf(nCopies(n, new AddVehicleEvent(-1, vehicleDto)));
    }
  }
}
