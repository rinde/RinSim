/**
 * 
 */
package rinde.sim.pdptw.scenario;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import rinde.sim.core.model.Model;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.SimulationInfo;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.StopCondition;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.scenario.TimedEvent.TimeComparator;
import rinde.sim.util.TimeWindow;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * A {@link Scenario} that defines a <i>dynamic pickup-and-delivery problem with
 * time windows</i>. It contains all information needed to instantiate an entire
 * simulation.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class PDPScenario extends Scenario {
  private static final long serialVersionUID = 7258024865764689371L;

  /**
   * New empty instance.
   */
  protected PDPScenario() {
    super();
  }

  /**
   * @param events The events of the scenario.
   * @param supportedTypes The supported event types of the scenario.
   */
  protected PDPScenario(Collection<? extends TimedEvent> events,
      Set<Enum<?>> supportedTypes) {
    super(events, supportedTypes);
  }

  /**
   * @return Should return a list of newly created {@link Model}s which will be
   *         used for simulating this scenario.
   */
  public abstract ImmutableList<? extends Model<?>> createModels();

  /**
   * @return The {@link TimeWindow} of the scenario indicates the start and end
   *         of scenario.
   */
  public abstract TimeWindow getTimeWindow();

  /**
   * @return The size of a tick.
   */
  public abstract long getTickSize();

  /**
   * @return The stop condition indicating when a simulation should end.
   */
  public abstract Predicate<SimulationInfo> getStopCondition();

  /**
   * @return The time unit used in the simulator.
   */
  public abstract Unit<Duration> getTimeUnit();

  /**
   * @return The speed unit used in the
   *         {@link rinde.sim.core.model.road.RoadModel}.
   */
  public abstract Unit<Velocity> getSpeedUnit();

  /**
   * @return The distance unit used in the
   *         {@link rinde.sim.core.model.road.RoadModel}.
   */
  public abstract Unit<Length> getDistanceUnit();

  /**
   * @return The 'class' to which this scenario belongs.
   */
  public abstract ProblemClass getProblemClass();

  /**
   * @return The instance id of this scenario.
   */
  public abstract String getProblemInstanceId();

  /**
   * Create a {@link Builder} to construct {@link PDPScenario} instances. For
   * constructing scenarios using probabilistic distribution see
   * {@link ScenarioGenerator}.
   * @param problemClass The problem class of the instance to construct.
   * @return A new {@link Builder} instance.
   */
  public static Builder builder(ProblemClass problemClass) {
    return new Builder(problemClass);
  }

  static Builder builder(AbstractBuilder<?> base, ProblemClass problemClass) {
    return new Builder(Optional.<AbstractBuilder<?>> of(base), problemClass);
  }

  /**
   * Represents a class of scenarios.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface ProblemClass {
    /**
     * @return The id of this problem class.
     */
    String getId();
  }

  /**
   * A builder for constructing {@link PDPScenario} instances.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static class Builder extends AbstractBuilder<Builder> {
    final List<TimedEvent> eventList;
    final ImmutableSet.Builder<Enum<?>> eventTypeBuilder;
    final ImmutableList.Builder<Supplier<? extends Model<?>>> modelSuppliers;
    final ProblemClass problemClass;
    String instanceId;

    Builder(ProblemClass pc) {
      this(Optional.<AbstractBuilder<?>> absent(), pc);
    }

    Builder(Optional<AbstractBuilder<?>> base, ProblemClass pc) {
      super(base);
      problemClass = pc;
      instanceId = "";
      eventList = newArrayList();
      eventTypeBuilder = ImmutableSet.builder();
      modelSuppliers = ImmutableList.builder();
    }

    /**
     * Add the specified {@link TimedEvent} to the scenario.
     * @param event The event to add.
     * @return This, as per the builder pattern.
     */
    public Builder addEvent(TimedEvent event) {
      eventList.add(event);
      eventTypeBuilder.add(event.getEventType());
      return self();
    }

    /**
     * Add the specified {@link TimedEvent}s to the scenario.
     * @param events The events to add.
     * @return This, as per the builder pattern.
     */
    public Builder addEvents(Iterable<? extends TimedEvent> events) {
      for (final TimedEvent te : events) {
        addEvent(te);
      }
      return self();
    }

    /**
     * The instance id to use for the next scenario that is created.
     * @param id The id to use.
     * @return This, as per the builder pattern.
     */
    public Builder instanceId(String id) {
      instanceId = id;
      return self();
    }

    /**
     * Adds the model supplier. The supplier will be used to create
     * {@link Model}s in the {@link PDPScenario#createModels()} method.
     * @param modelSupplier The model supplier to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModel(Supplier<? extends Model<?>> modelSupplier) {
      modelSuppliers.add(modelSupplier);
      return self();
    }

    /**
     * Adds the model suppliers. The suppliers will be used to create
     * {@link Model}s in the {@link PDPScenario#createModels()} method.
     * @param suppliers The model suppliers to add.
     * @return This, as per the builder pattern.
     */
    public Builder addModels(
        Iterable<? extends Supplier<? extends Model<?>>> suppliers) {
      modelSuppliers.addAll(suppliers);
      return self();
    }

    /**
     * Build a new {@link PDPScenario} instance.
     * @return The new instance.
     */
    public PDPScenario build() {
      final List<TimedEvent> list = newArrayList(eventList);
      Collections.sort(list, TimeComparator.INSTANCE);
      return new DefaultScenario(this, ImmutableList.copyOf(list),
          eventTypeBuilder.build());
    }

    @Override
    protected Builder self() {
      return this;
    }

    ImmutableList<Supplier<? extends Model<?>>> getModelSuppliers() {
      return modelSuppliers.build();
    }
  }

  static abstract class AbstractBuilder<T extends AbstractBuilder<T>> {
    static final Unit<Length> DEFAULT_DISTANCE_UNIT = SI.KILOMETER;
    static final Unit<Velocity> DEFAULT_SPEED_UNIT = NonSI.KILOMETERS_PER_HOUR;
    static final Unit<Duration> DEFAULT_TIME_UNIT = SI.MILLI(SI.SECOND);
    static final long DEFAULT_TICK_SIZE = 1000L;
    static final TimeWindow DEFAULT_TIME_WINDOW = new TimeWindow(0,
        8 * 60 * 60 * 1000);
    static final Predicate<SimulationInfo> DEFAULT_STOP_CONDITION = StopCondition.TIME_OUT_EVENT;

    Unit<Length> distanceUnit;
    Unit<Velocity> speedUnit;
    Unit<Duration> timeUnit;
    long tickSize;
    TimeWindow timeWindow;
    Predicate<SimulationInfo> stopCondition;

    AbstractBuilder() {
      this(Optional.<AbstractBuilder<?>> absent());
    }

    AbstractBuilder(AbstractBuilder<?> copy) {
      this(Optional.<AbstractBuilder<?>> of(copy));
    }

    AbstractBuilder(Optional<AbstractBuilder<?>> copy) {
      if (copy.isPresent()) {
        distanceUnit = copy.get().distanceUnit;
        speedUnit = copy.get().speedUnit;
        timeUnit = copy.get().timeUnit;
        tickSize = copy.get().tickSize;
        timeWindow = copy.get().timeWindow;
        stopCondition = copy.get().stopCondition;
      }
      else {
        distanceUnit = DEFAULT_DISTANCE_UNIT;
        speedUnit = DEFAULT_SPEED_UNIT;
        timeUnit = DEFAULT_TIME_UNIT;
        tickSize = DEFAULT_TICK_SIZE;
        timeWindow = DEFAULT_TIME_WINDOW;
        stopCondition = DEFAULT_STOP_CONDITION;
      }
    }

    /**
     * Should return 'this', the builder.
     * @return 'this'.
     */
    protected abstract T self();

    /**
     * Set the time unit to use. Possible values include: {@link SI#SECOND},
     * {@link NonSI#HOUR}, etc.
     * @param tu The time unit.
     * @return This, as per the builder pattern.
     */
    public T timeUnit(Unit<Duration> tu) {
      timeUnit = tu;
      return self();
    }

    /**
     * Set the tick size.
     * @param ts The tick size, expressed in the time unit as set by
     *          {@link #timeUnit(Unit)}.
     * @return This, as per the builder pattern.
     */
    public T tickSize(long ts) {
      tickSize = ts;
      return self();
    }

    /**
     * Set the speed unit. Possible values include: {@link SI#METERS_PER_SECOND}
     * , {@link NonSI#KILOMETERS_PER_HOUR}.
     * @param su The speed unit.
     * @return This, as per the builder pattern.
     */
    public T speedUnit(Unit<Velocity> su) {
      speedUnit = su;
      return self();
    }

    /**
     * Set the distance unit. Possible values include: {@link SI#METER},
     * {@link NonSI#MILE}.
     * @param du The distance unit.
     * @return This, as per the builder pattern.
     */
    public T distanceUnit(Unit<Length> du) {
      distanceUnit = du;
      return self();
    }

    /**
     * Set the length (duration) of the scenario. Note that the time at which
     * the simulation is stopped is defined by {@link #stopCondition(Predicate)}
     * .
     * @param length The length of the scenario, expressed in the time unit as
     *          set by {@link #timeUnit(Unit)}.
     * @return This, as per the builder pattern.
     */
    public T scenarioLength(long length) {
      timeWindow = new TimeWindow(0, length);
      return self();
    }

    /**
     * Set the condition when the scenario should stop. Some defaults are
     * supplied in {@link StopCondition}.
     * @param condition The stop condition to set.
     * @return This, as per the builder pattern.
     */
    public T stopCondition(Predicate<SimulationInfo> condition) {
      stopCondition = condition;
      return self();
    }
  }

  static class DefaultScenario extends PDPScenario {
    private static final long serialVersionUID = -4662516689920279959L;

    final ImmutableList<? extends Supplier<? extends Model<?>>> modelSuppliers;
    private final Unit<Velocity> speedUnit;
    private final Unit<Length> distanceUnit;
    private final Unit<Duration> timeUnit;
    private final TimeWindow timeWindow;
    private final long tickSize;
    private final Predicate<SimulationInfo> stopCondition;
    private final ProblemClass problemClass;
    private final String instanceId;

    DefaultScenario(Builder b, List<? extends TimedEvent> events,
        Set<Enum<?>> supportedTypes) {
      super(events, supportedTypes);
      modelSuppliers = b.getModelSuppliers();
      speedUnit = b.speedUnit;
      distanceUnit = b.distanceUnit;
      timeUnit = b.timeUnit;
      timeWindow = b.timeWindow;
      tickSize = b.tickSize;
      stopCondition = b.stopCondition;
      problemClass = b.problemClass;
      instanceId = b.instanceId;
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
    public Predicate<SimulationInfo> getStopCondition() {
      return stopCondition;
    }

    @Override
    public ImmutableList<? extends Model<?>> createModels() {
      final ImmutableList.Builder<Model<?>> builder = ImmutableList.builder();
      for (final Supplier<? extends Model<?>> sup : modelSuppliers) {
        builder.add(sup.get());
      }
      return builder.build();
    }

    @Override
    public ProblemClass getProblemClass() {
      return problemClass;
    }

    @Override
    public String getProblemInstanceId() {
      return instanceId;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof DefaultScenario)) {
        return false;
      }
      final DefaultScenario o = (DefaultScenario) other;
      return super.equals(o)
          && Objects.equal(o.modelSuppliers, modelSuppliers)
          && Objects.equal(o.speedUnit, speedUnit)
          && Objects.equal(o.distanceUnit, distanceUnit)
          && Objects.equal(o.timeUnit, timeUnit)
          && Objects.equal(o.timeWindow, timeWindow)
          && Objects.equal(o.tickSize, tickSize)
          && Objects.equal(o.stopCondition, stopCondition)
          && Objects.equal(o.problemClass, problemClass)
          && Objects.equal(o.instanceId, instanceId);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(super.hashCode(), modelSuppliers, speedUnit,
          distanceUnit, timeUnit, timeWindow, tickSize, stopCondition,
          problemClass, instanceId);
    }
  }
}
