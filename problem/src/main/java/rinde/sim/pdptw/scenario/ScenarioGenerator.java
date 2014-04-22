package rinde.sim.pdptw.scenario;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;

import java.util.List;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.pdptw.scenario.Depots.DepotGenerator;
import rinde.sim.pdptw.scenario.Models.ModelSupplierSupplier;
import rinde.sim.pdptw.scenario.PDPScenario.AbstractBuilder;
import rinde.sim.pdptw.scenario.PDPScenario.ProblemClass;
import rinde.sim.pdptw.scenario.Parcels.ParcelGenerator;
import rinde.sim.pdptw.scenario.Vehicles.VehicleGenerator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

public final class ScenarioGenerator {

  // global properties
  final Builder builder;
  final ImmutableList<Supplier<? extends Model<?>>> modelSuppliers;

  private final ParcelGenerator parcelGenerator;
  private final VehicleGenerator vehicleGenerator;
  private final DepotGenerator depotGenerator;

  ScenarioGenerator(Builder b) {
    builder = b;
    parcelGenerator = b.parcelGenerator;
    vehicleGenerator = b.vehicleGenerator;
    depotGenerator = b.depotGenerator;

    final ImmutableList.Builder<Supplier<? extends Model<?>>> modelsBuilder = ImmutableList
        .builder();
    for (final ModelSupplierSupplier<?> sup : builder.modelSuppliers) {
      modelsBuilder.add(sup.get(this));
    }
    modelSuppliers = modelsBuilder.build();
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
    return parcelGenerator.getMin();
  }

  public Point getMax() {
    return parcelGenerator.getMax();
  }

  public ProblemClass getProblemClass() {
    return builder.problemClass;
  }

  public PDPScenario generate(RandomGenerator rng, String id) {
    final ImmutableList.Builder<TimedEvent> b = ImmutableList.builder();
    b.addAll(depotGenerator.generate(rng.nextLong(),
        parcelGenerator.getCenter()));

    b.addAll(vehicleGenerator.generate(rng.nextLong(),
        parcelGenerator.getCenter(), builder.timeWindow.end));

    b.addAll(parcelGenerator.generate(rng.nextLong()));

    b.add(new TimedEvent(PDPScenarioEvent.TIME_OUT, builder.timeWindow.end));
    return PDPScenario.builder(builder, builder.problemClass)
        .addModels(modelSuppliers)
        .addEvents(b.build())
        .instanceId(id)
        .build();
  }

  public static Builder builder(ProblemClass problemClass) {
    return new Builder(problemClass);
  }

  public static class Builder extends AbstractBuilder<Builder> {
    static final VehicleGenerator DEFAULT_VEHICLE_GENERATOR = Vehicles
        .builder().build();
    static final DepotGenerator DEFAULT_DEPOT_GENERATOR = Depots
        .singleCenteredDepot();

    ParcelGenerator parcelGenerator;
    VehicleGenerator vehicleGenerator;
    DepotGenerator depotGenerator;
    final List<ModelSupplierSupplier<?>> modelSuppliers;
    final ProblemClass problemClass;

    Builder(ProblemClass pc) {
      super();
      problemClass = pc;
      vehicleGenerator = DEFAULT_VEHICLE_GENERATOR;
      depotGenerator = DEFAULT_DEPOT_GENERATOR;
      modelSuppliers = newLinkedList();
    }

    // copying constructor
    Builder(Builder b) {
      super(b);
      problemClass = b.problemClass;
      parcelGenerator = b.parcelGenerator;
      vehicleGenerator = b.vehicleGenerator;
      depotGenerator = b.depotGenerator;
      modelSuppliers = newArrayList(b.modelSuppliers);
    }

    @Override
    protected Builder self() {
      return this;
    }

    public Builder vehicles(VehicleGenerator vg) {
      vehicleGenerator = vg;
      return this;
    }

    public Builder parcels(ParcelGenerator pg) {
      parcelGenerator = pg;
      return this;
    }

    public Builder addModel(ModelSupplierSupplier<? extends Model<?>> model) {
      modelSuppliers.add(model);
      return this;
    }

    public Builder addModel(Supplier<? extends Model<?>> model) {
      modelSuppliers.add(Models.adapt(model));
      return this;
    }

    public ScenarioGenerator build() {
      return new ScenarioGenerator(new Builder(this));
    }
  }
}
