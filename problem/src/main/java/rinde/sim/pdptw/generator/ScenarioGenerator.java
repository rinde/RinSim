package rinde.sim.pdptw.generator;

import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.SimulationInfo;
import rinde.sim.pdptw.common.DynamicPDPTWScenario;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.generator.loc.LocationGenerator;
import rinde.sim.pdptw.generator.times.ArrivalTimeGenerator;
import rinde.sim.pdptw.generator.tw.TimeWindowGenerator;
import rinde.sim.pdptw.generator.vehicles.VehicleGenerator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.SupplierRng;
import rinde.sim.util.SupplierRngs;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;

public final class ScenarioGenerator {

  // TODO
  // add depot
  // add requirements, i.e. based on measurements?

  // global properties
  final Unit<Velocity> speedUnit;
  final Unit<Length> distanceUnit;
  final Unit<Duration> timeUnit;
  final TimeWindow timeWindow;
  final long tickSize;

  // parcel properties -> move to separate generator?
  private final ArrivalTimeGenerator arrivalTimeGenerator;
  private final LocationGenerator locationGenerator;
  private final TimeWindowGenerator timeWindowGenerator;
  private final SupplierRng<Long> pickupDurationGenerator;
  private final SupplierRng<Long> deliveryDurationGenerator;
  private final SupplierRng<Integer> neededCapacityGenerator;

  // vehicle properties
  private final VehicleGenerator vehicleGenerator;

  ScenarioGenerator(Builder b) {
    speedUnit = b.speedUnit;
    distanceUnit = b.distanceUnit;
    timeUnit = b.timeUnit;
    timeWindow = b.timeWindow;
    tickSize = b.tickSize;

    arrivalTimeGenerator = b.arrivalTimeGenerator;
    locationGenerator = b.locationGenerator;
    timeWindowGenerator = b.timeWindowGenerator;
    pickupDurationGenerator = b.pickupDurationGenerator;
    deliveryDurationGenerator = b.deliveryDurationGenerator;
    neededCapacityGenerator = b.neededCapacityGenerator;

    vehicleGenerator = b.vehicleGenerator;
  }

  public DynamicPDPTWScenario generate(RandomGenerator rng) {
    final ImmutableList.Builder<TimedEvent> b = ImmutableList.builder();
    b.addAll(vehicleGenerator.generate(rng.nextLong(),
        locationGenerator.getCenter(), timeWindow.end));

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
    b.add(new TimedEvent(PDPScenarioEvent.TIME_OUT, timeWindow.end));
    final Set<Enum<?>> eventTypes = ImmutableSet.<Enum<?>> of(
        PDPScenarioEvent.TIME_OUT,
        PDPScenarioEvent.ADD_DEPOT,
        PDPScenarioEvent.ADD_VEHICLE,
        PDPScenarioEvent.ADD_PARCEL);

    return new GeneratedScenario(this, b.build(), eventTypes);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    static final Unit<Length> DEFAULT_DISTANCE_UNIT = SI.KILOMETER;
    static final Unit<Velocity> DEFAULT_SPEED_UNIT = NonSI.KILOMETERS_PER_HOUR;
    static final Unit<Duration> DEFAULT_TIME_UNIT = SI.MILLI(SI.SECOND);
    static final long DEFAULT_TICK_SIZE = 1000L;
    static final TimeWindow DEFAULT_TIME_WINDOW = new TimeWindow(0,
        8 * 60 * 60 * 1000);

    static final SupplierRng<Long> DEFAULT_SERVICE_DURATION = SupplierRngs
        .constant(5 * 60 * 1000L);
    static final SupplierRng<Integer> DEFAULT_CAPACITY = SupplierRngs
        .constant(0);

    Unit<Length> distanceUnit;
    Unit<Velocity> speedUnit;
    Unit<Duration> timeUnit;
    long tickSize;
    TimeWindow timeWindow;

    ArrivalTimeGenerator arrivalTimeGenerator;
    TimeWindowGenerator timeWindowGenerator;
    LocationGenerator locationGenerator;
    VehicleGenerator vehicleGenerator;

    SupplierRng<Long> pickupDurationGenerator;
    SupplierRng<Long> deliveryDurationGenerator;
    SupplierRng<Integer> neededCapacityGenerator;

    // problem class
    // problem instance id
    // stop conditions
    // models

    Builder() {
      distanceUnit = DEFAULT_DISTANCE_UNIT;
      speedUnit = DEFAULT_SPEED_UNIT;
      timeUnit = DEFAULT_TIME_UNIT;
      tickSize = DEFAULT_TICK_SIZE;
      timeWindow = DEFAULT_TIME_WINDOW;

      pickupDurationGenerator = DEFAULT_SERVICE_DURATION;
      deliveryDurationGenerator = DEFAULT_SERVICE_DURATION;
      neededCapacityGenerator = DEFAULT_CAPACITY;
    }

    public Builder timeUnit(Unit<Duration> tu) {
      timeUnit = tu;
      return this;
    }

    public Builder tickSize(long ts) {
      tickSize = ts;
      return this;
    }

    public Builder speedUnit(Unit<Velocity> su) {
      speedUnit = su;
      return this;
    }

    public Builder distanceUnit(Unit<Length> du) {
      distanceUnit = du;
      return this;
    }

    public Builder scenarioLength(long length) {
      timeWindow = new TimeWindow(0, length);
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
  }

  private static final class GeneratedScenario extends DynamicPDPTWScenario {
    private final Unit<Velocity> speedUnit;
    private final Unit<Length> distanceUnit;
    private final Unit<Duration> timeUnit;
    private final TimeWindow timeWindow;
    private final long tickSize;

    GeneratedScenario(ScenarioGenerator sg, List<? extends TimedEvent> events,
        Set<Enum<?>> supportedTypes) {
      super(events, supportedTypes);
      timeUnit = sg.timeUnit;
      timeWindow = sg.timeWindow;
      tickSize = sg.tickSize;
      speedUnit = sg.speedUnit;
      distanceUnit = sg.distanceUnit;
    }

    @Override
    public Unit<Duration> getTimeUnit() {
      return timeUnit;
    }

    @Override
    public TimeWindow getTimeWindow() {
      return timeWindow;
    }

    @Override
    public long getTickSize() {
      return tickSize;
    }

    @Override
    public Unit<Velocity> getSpeedUnit() {
      return speedUnit;
    }

    @Override
    public Unit<Length> getDistanceUnit() {
      return distanceUnit;
    }

    @Override
    public RoadModel createRoadModel() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public PDPModel createPDPModel() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Predicate<SimulationInfo> getStopCondition() {
      // TODO Auto-generated method stub
      return null;
    }

    // FIXME create one interface for ProblemInstance
    // with:
    // getInstanceId()
    // getProblemClass()

    @Override
    public ProblemClass getProblemClass() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getProblemInstanceId() {
      // TODO Auto-generated method stub
      return null;
    }
  }
}
