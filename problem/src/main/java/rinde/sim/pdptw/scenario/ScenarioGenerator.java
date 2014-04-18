package rinde.sim.pdptw.scenario;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;

import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.scenario.Depots.DepotGenerator;
import rinde.sim.pdptw.scenario.Locations.LocationGenerator;
import rinde.sim.pdptw.scenario.Models.ModelSupplierSupplier;
import rinde.sim.pdptw.scenario.PDPScenario.AbstractBuilder;
import rinde.sim.pdptw.scenario.PDPScenario.DefaultScenario;
import rinde.sim.pdptw.scenario.Vehicles.VehicleGenerator;
import rinde.sim.pdptw.scenario.times.ArrivalTimeGenerator;
import rinde.sim.pdptw.scenario.tw.TimeWindowGenerator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.SupplierRng;
import rinde.sim.util.SupplierRngs;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;

public final class ScenarioGenerator {

  // global properties
  final Builder builder;
  final ImmutableList<Supplier<? extends Model<?>>> modelSuppliers;

  // parcel properties -> move to separate generator?
  private final ArrivalTimeGenerator arrivalTimeGenerator;
  private final LocationGenerator locationGenerator;
  private final TimeWindowGenerator timeWindowGenerator;
  private final SupplierRng<Long> pickupDurationGenerator;
  private final SupplierRng<Long> deliveryDurationGenerator;
  private final SupplierRng<Integer> neededCapacityGenerator;

  // vehicles and depots
  private final VehicleGenerator vehicleGenerator;
  private final DepotGenerator depotGenerator;

  ScenarioGenerator(Builder b) {
    builder = b;
    arrivalTimeGenerator = b.arrivalTimeGenerator;
    locationGenerator = b.locationGenerator;
    timeWindowGenerator = b.timeWindowGenerator;
    pickupDurationGenerator = b.pickupDurationGenerator;
    deliveryDurationGenerator = b.deliveryDurationGenerator;
    neededCapacityGenerator = b.neededCapacityGenerator;

    vehicleGenerator = b.vehicleGenerator;
    depotGenerator = b.depotGenerator;

    final ImmutableList.Builder<Supplier<? extends Model<?>>> builder = ImmutableList
        .builder();
    for (final ModelSupplierSupplier<?> sup : b.modelSuppliers) {
      builder.add(sup.get(this));
    }
    modelSuppliers = builder.build();
  }

  public Unit<Velocity> getSpeedUnit() {
    return builder.speedUnit;
  }

  public Unit<Length> getDistanceUnit() {
    return builder.distanceUnit;
  }

  public Unit<Duration> getTimeUnit() {
    return builder.timeUnit;
  }

  public TimeWindow getTimeWindow() {
    return builder.timeWindow;
  }

  public long getTickSize() {
    return builder.tickSize;
  }

  public Point getMin() {
    return locationGenerator.getMin();
  }

  public Point getMax() {
    return locationGenerator.getMax();
  }

  public PDPScenario generate(RandomGenerator rng) {
    final ImmutableList.Builder<TimedEvent> b = ImmutableList.builder();
    b.addAll(depotGenerator.generate(rng.nextLong(),
        locationGenerator.getCenter()));

    b.addAll(vehicleGenerator.generate(rng.nextLong(),
        locationGenerator.getCenter(), builder.timeWindow.end));

    final List<Double> times = arrivalTimeGenerator.generate(rng.nextLong());
    final Iterator<Point> locs = locationGenerator.generate(rng.nextLong(),
        times.size() * 2).iterator();

    for (final double time : times) {
      final long arrivalTime = DoubleMath.roundToLong(time,
          RoundingMode.HALF_DOWN);
      final Point origin = locs.next();
      final Point destination = locs.next();

      final List<TimeWindow> tws = timeWindowGenerator.generate(rng.nextLong(),
          arrivalTime, origin, destination);

      final TimeWindow pickupTW = tws.get(0);
      final TimeWindow deliveryTW = tws.get(1);

      final ParcelDTO dto = ParcelDTO.builder(origin, destination)
          .arrivalTime(arrivalTime)
          .pickupTimeWindow(pickupTW)
          .deliveryTimeWindow(deliveryTW)
          .pickupDuration(pickupDurationGenerator.get(rng.nextLong()))
          .deliveryDuration(deliveryDurationGenerator.get(rng.nextLong()))
          .neededCapacity(neededCapacityGenerator.get(rng.nextLong()))
          .build();

      b.add(new AddParcelEvent(dto));
    }
    b.add(new TimedEvent(PDPScenarioEvent.TIME_OUT, builder.timeWindow.end));
    final Set<Enum<?>> eventTypes = ImmutableSet.<Enum<?>> of(
        PDPScenarioEvent.TIME_OUT,
        PDPScenarioEvent.ADD_DEPOT,
        PDPScenarioEvent.ADD_VEHICLE,
        PDPScenarioEvent.ADD_PARCEL);

    return new DefaultScenario(builder, b.build(), eventTypes);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends AbstractBuilder<Builder> {
    static final SupplierRng<Long> DEFAULT_SERVICE_DURATION = SupplierRngs
        .constant(5 * 60 * 1000L);
    static final SupplierRng<Integer> DEFAULT_CAPACITY = SupplierRngs
        .constant(0);

    static final VehicleGenerator DEFAULT_VEHICLE_GENERATOR = Vehicles
        .builder().build();
    static final DepotGenerator DEFAULT_DEPOT_GENERATOR = Depots
        .singleCenteredDepot();

    ArrivalTimeGenerator arrivalTimeGenerator;
    TimeWindowGenerator timeWindowGenerator;
    LocationGenerator locationGenerator;
    SupplierRng<Long> pickupDurationGenerator;
    SupplierRng<Long> deliveryDurationGenerator;
    SupplierRng<Integer> neededCapacityGenerator;

    VehicleGenerator vehicleGenerator;
    DepotGenerator depotGenerator;
    final List<ModelSupplierSupplier<?>> modelSuppliers;

    Builder() {
      super();
      pickupDurationGenerator = DEFAULT_SERVICE_DURATION;
      deliveryDurationGenerator = DEFAULT_SERVICE_DURATION;
      neededCapacityGenerator = DEFAULT_CAPACITY;
      vehicleGenerator = DEFAULT_VEHICLE_GENERATOR;
      depotGenerator = DEFAULT_DEPOT_GENERATOR;
      modelSuppliers = newLinkedList();
    }

    // copying constructor
    Builder(Builder b) {
      super(b);
      arrivalTimeGenerator = b.arrivalTimeGenerator;
      timeWindowGenerator = b.timeWindowGenerator;
      locationGenerator = b.locationGenerator;
      pickupDurationGenerator = b.pickupDurationGenerator;
      deliveryDurationGenerator = b.deliveryDurationGenerator;
      neededCapacityGenerator = b.neededCapacityGenerator;
      vehicleGenerator = b.vehicleGenerator;
      depotGenerator = b.depotGenerator;
      modelSuppliers = newArrayList(b.modelSuppliers);
    }

    @Override
    protected Builder self() {
      return this;
    }

    public Builder arrivalTimes(ArrivalTimeGenerator atg) {
      arrivalTimeGenerator = atg;
      return this;
    }

    public Builder timeWindows(TimeWindowGenerator twg) {
      timeWindowGenerator = twg;
      return this;
    }

    public Builder locations(LocationGenerator lg) {
      locationGenerator = lg;
      return this;
    }

    public Builder vehicles(VehicleGenerator vg) {
      vehicleGenerator = vg;
      return this;
    }

    public Builder pickupDurations(SupplierRng<Long> durations) {
      pickupDurationGenerator = durations;
      return this;
    }

    public Builder deliveryDurations(SupplierRng<Long> durations) {
      deliveryDurationGenerator = durations;
      return this;
    }

    public Builder serviceDurations(SupplierRng<Long> durations) {
      return pickupDurations(durations).deliveryDurations(durations);
    }

    public Builder neededCapacities(SupplierRng<Integer> capacities) {
      neededCapacityGenerator = capacities;
      return this;
    }

    public Builder addModel(ModelSupplierSupplier<? extends Model<?>> model) {
      modelSuppliers.add(model);
      return this;
    }

    @Override
    public Builder addModel(Supplier<? extends Model<?>> model) {
      modelSuppliers.add(Models.adapt(model));
      return this;
    }

    public ScenarioGenerator build() {
      return new ScenarioGenerator(new Builder(this));
    }

  }
}
