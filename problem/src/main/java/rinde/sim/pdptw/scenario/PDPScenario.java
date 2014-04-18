/**
 * 
 */
package rinde.sim.pdptw.scenario;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import rinde.sim.core.model.Model;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.SimulationInfo;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.StopCondition;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

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

  protected PDPScenario(Collection<? extends TimedEvent> events,
      Set<Enum<?>> supportedTypes) {
    super(events, supportedTypes);
  }

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
   * @return The speed unit used in the {@link RoadModel}.
   */
  public abstract Unit<Velocity> getSpeedUnit();

  /**
   * @return The distance unit used in the {@link RoadModel}.
   */
  public abstract Unit<Length> getDistanceUnit();

  public abstract ProblemClass getProblemClass();

  // used to distinguish between two instances from the same class
  public abstract String getProblemInstanceId();

  public interface ProblemClass {

    // creation date?
    // author?
    // class
    // instance

    String getId();
  }

  public static class DefaultScenario extends PDPScenario {
    private final Unit<Velocity> speedUnit;
    private final Unit<Length> distanceUnit;
    private final Unit<Duration> timeUnit;
    private final TimeWindow timeWindow;
    private final long tickSize;
    private final Predicate<SimulationInfo> stopCondition;
    private final ImmutableList<? extends Supplier<? extends Model<?>>> modelSuppliers;

    DefaultScenario(AbstractBuilder<?> b, List<? extends TimedEvent> events,
        Set<Enum<?>> supportedTypes) {
      super(events, supportedTypes);
      timeUnit = b.timeUnit;
      timeWindow = b.timeWindow;
      tickSize = b.tickSize;
      speedUnit = b.speedUnit;
      distanceUnit = b.distanceUnit;
      stopCondition = b.stopCondition;
      modelSuppliers = ImmutableList.copyOf(b.modelSuppliers);
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
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String getProblemInstanceId() {
      // TODO Auto-generated method stub
      return null;
    }

  }

  public static class Builder extends AbstractBuilder<Builder> {
    final ImmutableList.Builder<TimedEvent> eventBuilder;
    final ImmutableSet.Builder<Enum<?>> eventTypeBuilder;

    Builder() {
      super();
      eventBuilder = ImmutableList.builder();
      eventTypeBuilder = ImmutableSet.builder();
    }

    public Builder addEvent(TimedEvent event) {
      eventBuilder.add(event);
      eventTypeBuilder.add(event.getEventType());
      return this;
    }

    public Builder addEvents(Iterable<? extends TimedEvent> events) {
      for (final TimedEvent te : events) {
        addEvent(te);
      }
      return this;
    }

    public DefaultScenario build() {
      return new DefaultScenario(this, eventBuilder.build(),
          eventTypeBuilder.build());
    }

    @Override
    protected Builder self() {
      return this;
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
    final List<Supplier<? extends Model<?>>> modelSuppliers;

    AbstractBuilder() {
      distanceUnit = DEFAULT_DISTANCE_UNIT;
      speedUnit = DEFAULT_SPEED_UNIT;
      timeUnit = DEFAULT_TIME_UNIT;
      tickSize = DEFAULT_TICK_SIZE;
      timeWindow = DEFAULT_TIME_WINDOW;
      stopCondition = DEFAULT_STOP_CONDITION;
      modelSuppliers = newLinkedList();
    }

    AbstractBuilder(AbstractBuilder<?> copy) {
      distanceUnit = copy.distanceUnit;
      speedUnit = copy.speedUnit;
      timeUnit = copy.timeUnit;
      tickSize = copy.tickSize;
      timeWindow = copy.timeWindow;
      stopCondition = copy.stopCondition;
      modelSuppliers = newArrayList(copy.modelSuppliers);
    }

    protected abstract T self();

    public T timeUnit(Unit<Duration> tu) {
      timeUnit = tu;
      return self();
    }

    public T tickSize(long ts) {
      tickSize = ts;
      return self();
    }

    public T speedUnit(Unit<Velocity> su) {
      speedUnit = su;
      return self();
    }

    public T distanceUnit(Unit<Length> du) {
      distanceUnit = du;
      return self();
    }

    public T scenarioLength(long length) {
      timeWindow = new TimeWindow(0, length);
      return self();
    }

    public T stopCondition(Predicate<SimulationInfo> condition) {
      stopCondition = condition;
      return self();
    }

    public T addModel(Supplier<? extends Model<?>> model) {
      modelSuppliers.add(model);
      return self();
    }
  }
}
