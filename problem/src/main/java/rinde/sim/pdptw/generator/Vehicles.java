package rinde.sim.pdptw.generator;

import static java.util.Collections.nCopies;
import static rinde.sim.util.SupplierRngs.constant;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.VehicleDTO;
import rinde.sim.util.SupplierRng;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public final class Vehicles {
  static final int DEFAULT_NUM_OF_VEHICLES = 10;
  static final SupplierRng<Integer> DEFAULT_NUMBER_OF_VEHICLES = constant(DEFAULT_NUM_OF_VEHICLES);
  static final SupplierRng<Double> DEFAULT_SPEED = constant(50d);
  static final SupplierRng<Integer> DEFAULT_CAPACITY = constant(1);
  static final SupplierRng<Long> DEFAULT_TIME = constant(-1L);

  private Vehicles() {}

  public static HomogenousBuilder homogenous(VehicleDTO dto) {
    return new HomogenousBuilder(dto);
  }

  public static class HomogenousBuilder {
    private final VehicleDTO dto;
    private int numberOfVehicles;

    HomogenousBuilder(VehicleDTO d) {
      dto = d;
      numberOfVehicles = DEFAULT_NUM_OF_VEHICLES;
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

  public interface VehicleGenerator {
    ImmutableList<AddVehicleEvent> generate(long seed, Point center,
        long scenarioLength);
  }

  public static class Builder {
    SupplierRng<Integer> numberOfVehicles;
    Optional<SupplierRng<Point>> startPositionGenerator;
    SupplierRng<Double> speedGenerator;
    SupplierRng<Integer> capacityGenerator;
    Optional<SupplierRng<TimeWindow>> timeWindowGenerator;
    SupplierRng<Long> creationTimeGenerator;

    Builder() {
      numberOfVehicles = DEFAULT_NUMBER_OF_VEHICLES;
      startPositionGenerator = Optional.absent();
      speedGenerator = DEFAULT_SPEED;
      capacityGenerator = DEFAULT_CAPACITY;
      timeWindowGenerator = Optional.absent();
      creationTimeGenerator = DEFAULT_TIME;
    }

    public Builder numberOfVehicles(SupplierRng<Integer> num) {
      numberOfVehicles = num;
      return this;
    }

    public Builder startPositions(SupplierRng<Point> pos) {
      startPositionGenerator = Optional.of(pos);
      return this;
    }

    public Builder centeredStartPositions() {
      startPositionGenerator = Optional.absent();
      return this;
    }

    public Builder timeWindows(SupplierRng<TimeWindow> tw) {
      timeWindowGenerator = Optional.of(tw);
      return this;
    }

    public Builder timeWindowsAsScenario() {
      timeWindowGenerator = Optional.absent();
      return this;
    }

    public Builder speeds(SupplierRng<Double> sp) {
      speedGenerator = sp;
      return this;
    }

    public Builder capacities(SupplierRng<Integer> cap) {
      capacityGenerator = cap;
      return this;
    }

    public Builder creationTimes(SupplierRng<Long> times) {
      creationTimeGenerator = times;
      return this;
    }

    public VehicleGenerator build() {
      return new DefaultVehicleGenerator(this);
    }
  }

  private static class DefaultVehicleGenerator implements VehicleGenerator {
    private final SupplierRng<Integer> numberOfVehicles;
    private final Optional<SupplierRng<Point>> startPositionGenerator;
    private final SupplierRng<Double> speedGenerator;
    private final SupplierRng<Integer> capacityGenerator;
    private final Optional<SupplierRng<TimeWindow>> timeWindowGenerator;
    private final SupplierRng<Long> creationTimeGenerator;
    private final RandomGenerator rng;

    DefaultVehicleGenerator(Builder b) {
      numberOfVehicles = b.numberOfVehicles;
      startPositionGenerator = b.startPositionGenerator;
      speedGenerator = b.speedGenerator;
      capacityGenerator = b.capacityGenerator;
      timeWindowGenerator = b.timeWindowGenerator;
      creationTimeGenerator = b.creationTimeGenerator;
      rng = new MersenneTwister();
    }

    @Override
    public ImmutableList<AddVehicleEvent> generate(long seed, Point center,
        long scenarioLength) {
      rng.setSeed(seed);

      final ImmutableList.Builder<AddVehicleEvent> builder = ImmutableList
          .builder();
      final int num = numberOfVehicles.get(rng.nextLong());
      for (int i = 0; i < num; i++) {
        final Point pos = startPositionGenerator.isPresent()
            ? startPositionGenerator.get().get(rng.nextLong())
            : center;
        final double speed = speedGenerator.get(rng.nextLong());
        final int capacity = capacityGenerator.get(rng.nextLong());
        final TimeWindow tw = timeWindowGenerator.isPresent()
            ? timeWindowGenerator.get().get(rng.nextLong())
            : new TimeWindow(0L, scenarioLength);
        final long time = creationTimeGenerator.get(rng.nextLong());
        final VehicleDTO dto = VehicleDTO.builder()
            .startPosition(pos)
            .speed(speed)
            .capacity(capacity)
            .availabilityTimeWindow(tw)
            .build();
        builder.add(new AddVehicleEvent(time, dto));
      }
      return builder.build();
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
    public ImmutableList<AddVehicleEvent> generate(long seed, Point center,
        long scenarioLength) {
      rng.setSeed(seed);
      return ImmutableList
          .copyOf(nCopies(n, new AddVehicleEvent(-1, vehicleDto)));
    }
  }
}
