package rinde.sim.scenario;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

/**
 * The ScenarioBuilder is a helper class to easily generate {@link Scenario}
 * instances. It provides a set of easy to use methods and it is extensible by
 * adding {@link EventGenerator}s to the builder. The specific class of
 * {@link TimedEvent} that should be generated can be changed by using
 * {@link EventCreator}.
 * 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ScenarioBuilder {

  final Set<Enum<?>> supportedTypes;
  private final List<EventGenerator<? extends TimedEvent>> generators;
  private final List<TimedEvent> events;

  /**
   * Initializes a new ScenarioBuilder which supports the specified types of
   * events.
   * @param pSupportedTypes The event types this ScenarioBuilder supports.
   */
  public ScenarioBuilder(Set<Enum<?>> pSupportedTypes) {
    this(ImmutableSet.copyOf(pSupportedTypes));
  }

  /**
   * Initializes a new ScenarioBuilder which supports the specified types of
   * events.
   * @param pSupportedTypes The event types this ScenarioBuilder supports.
   */
  public ScenarioBuilder(Enum<?>... pSupportedTypes) {
    this(ImmutableSet.copyOf(pSupportedTypes));
  }

  /**
   * Initializes a new ScenarioBuilder which supports the specified types of
   * events.
   * @param types The event types this ScenarioBuilder supports.
   */
  protected ScenarioBuilder(ImmutableSet<Enum<?>> types) {
    supportedTypes = types;
    generators = newLinkedList();
    events = newArrayList();
  }

  /**
   * Add an {@link EventGenerator} to this ScenarioBuilder.
   * @param generator The generator to add.
   * @return this
   */
  public ScenarioBuilder addEventGenerator(
      EventGenerator<? extends TimedEvent> generator) {
    checkArgument(generator != null, "generator can not be null");
    generators.add(generator);
    return this;
  }

  /**
   * Convenience method for adding a single event.
   * @param event The {@link TimedEvent} to add.
   * @return this
   */
  public ScenarioBuilder addEvent(TimedEvent event) {
    checkArgument(supportedTypes.contains(event.getEventType()), "%s is not a supported event type of this ScenarioBuilder, it should be added in its constructor.", event
        .getEventType());
    events.add(event);
    return this;
  }

  /**
   * Convenience method for adding events.
   * @param es The {@link TimedEvent} to add.
   * @return this
   */
  public ScenarioBuilder addEvents(TimedEvent... es) {
    for (final TimedEvent te : es) {
      addEvent(te);
    }
    return this;
  }

  /**
   * Convenience method for adding events.
   * @param es The {@link TimedEvent} to add.
   * @return this
   */
  public ScenarioBuilder addEvents(Collection<? extends TimedEvent> es) {
    for (final TimedEvent te : es) {
      addEvent(te);
    }
    return this;
  }

  /**
   * Adds multiple events using a creator.
   * @param time The time at which the {@link TimedEvent} will be added.
   * @param amount The amount of events to add.
   * @param eventCreator The {@link EventCreator} that instantiates the events.
   * @return this
   */
  public <T extends TimedEvent> ScenarioBuilder addMultipleEvents(long time,
      int amount, EventCreator<T> eventCreator) {
    generators.add(new MultipleEventGenerator<T>(time, amount, eventCreator));
    return this;
  }

  /**
   * Adds multiple {@link TimedEvent} using the specified type.
   * @param time The time at which the {@link TimedEvent} will be added.
   * @param amount The amount of events to add.
   * @param type The type of event to add.
   * @return this
   */
  public ScenarioBuilder addMultipleEvents(long time, int amount, Enum<?> type) {
    addMultipleEvents(time, amount, new EventTypeFunction(type));
    return this;
  }

  /**
   * Adds a series of events. The first event will be added at
   * <code>startTime</code>, the next events will be added with intervals of
   * size <code>timeStep</code> up to and <strong>including</strong>
   * <code>endTime</code>
   * @param startTime The time of the first event in the series.
   * @param endTime The end time of the series.
   * @param timeStep The interval between events.
   * @param eventCreator The {@link EventCreator} that creates events.
   * @return this
   */
  public <T extends TimedEvent> ScenarioBuilder addTimeSeriesOfEvents(
      long startTime, long endTime, long timeStep, EventCreator<T> eventCreator) {
    generators
        .add(new TimeSeries<T>(startTime, endTime, timeStep, eventCreator));
    return this;
  }

  /**
   * Adds a series of events. The first event will be added at
   * <code>startTime</code>, the next events will be added with intervals of
   * size <code>timeStep</code> up to and <strong>including</strong>
   * <code>endTime</code>
   * @param startTime The time of the first event in the series.
   * @param endTime The end time of the series.
   * @param timeStep The interval between events.
   * @param type The type of event to add.
   * @return this
   */
  public <T extends TimedEvent> ScenarioBuilder addTimeSeriesOfEvents(
      long startTime, long endTime, long timeStep, Enum<?> type) {
    addTimeSeriesOfEvents(startTime, endTime, timeStep, new EventTypeFunction(
        type));
    return this;
  }

  /**
   * @return the current events as a sorted list.
   */
  protected List<TimedEvent> buildEventList() {
    final List<TimedEvent> es = newArrayList(events);
    for (final EventGenerator<? extends TimedEvent> g : generators) {
      final Collection<? extends TimedEvent> collection = g.generate();
      for (final TimedEvent te : collection) {
        checkArgument(supportedTypes.contains(te.getEventType()), "%s is not supported by this ScenarioBuilder", te
            .getEventType());
        es.add(te);
      }
    }
    Collections.sort(es, new TimeComparator());
    return es;
  }

  /**
   * Generates a new {@link Scenario}.
   * @return The new scenario.
   */
  public Scenario build() {
    return build(new ScenarioCreator<Scenario>() {
      @Override
      public Scenario create(List<TimedEvent> eventList, Set<Enum<?>> eventTypes) {
        return new Scenario(eventList, supportedTypes);
      }
    });
  }

  /**
   * Build the scenario using the specified {@link ScenarioCreator}.
   * @param sc ScenarioCreator which instantiates the scenario.
   * @return A scenario.
   */
  public <T extends Scenario> T build(ScenarioCreator<T> sc) {
    return sc.create(buildEventList(), supportedTypes);
  }

  /**
   * Checks whether the specified scenario is time consistent, i.e. all events
   * should be sorted by time.
   * @param scen The scenario to check.
   * @return <code>true</code> if it is consistent, <code>false</code>
   *         otherwise.
   */
  public static boolean isTimeOrderingConsistent(Scenario scen) {
    final List<TimedEvent> es = newArrayList(scen.asList());
    Collections.sort(es, new TimeComparator());
    return scen.asList().equals(es);
  }

  /**
   * A scenario creator can be used to create custom scenarios (subclasses of
   * {@link Scenario}.
   * @param <T> The type of scenario the creator creates.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface ScenarioCreator<T extends Scenario> {
    /**
     * @param eventList
     * @param eventTypes
     * @return The scenario
     */
    T create(List<TimedEvent> eventList, Set<Enum<?>> eventTypes);
  }

  /**
   * Classes that implement this interface can be used to generate a sequence of
   * events.
   * @param <T> The subtype of {@link TimedEvent} that is generated.
   * 
   * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface EventGenerator<T extends TimedEvent> {
    /**
     * @return A sequence of events.
     */
    Collection<T> generate();
  }

  /**
   * An {@link EventGenerator} that generates multiple events.
   * @param <T> The subtype of {@link TimedEvent} to generate.
   * 
   * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static class MultipleEventGenerator<T extends TimedEvent> implements
      EventGenerator<T> {
    private final long time;
    private final int amount;
    private final Function<Long, T> eventCreator;

    /**
     * Instantiate the MultipleEventGenerator.
     * @param pTime The time at which the {@link TimedEvent}s will be added.
     * @param pAmount The amount of events to add.
     * @param pEventCreator The {@link EventCreator} that instantiates the
     *          events.
     */
    public MultipleEventGenerator(long pTime, int pAmount,
        EventCreator<T> pEventCreator) {
      checkArgument(pTime >= 0, "time can not be negative");
      checkArgument(pAmount >= 1, "amount must be at least 1");
      checkArgument(pEventCreator != null, "event creator can not be null");
      time = pTime;
      amount = pAmount;
      eventCreator = pEventCreator;
    }

    @Override
    public Collection<T> generate() {
      final List<T> result = new LinkedList<T>();
      for (int i = 0; i < amount; ++i) {
        result.add(eventCreator.apply(time));
      }
      return result;
    }
  }

  /**
   * An {@link EventGenerator} that generates events using a fixed interval. The
   * first event will be added at <code>startTime</code>, the next events will
   * be added with intervals of size <code>timeStep</code> up to and
   * <strong>including</strong> <code>endTime</code>
   * @param <T> The subtype of {@link TimedEvent} to generate.
   * 
   * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
   * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
   */
  public static class TimeSeries<T extends TimedEvent> implements
      EventGenerator<T> {
    private final long start;
    private final long end;
    private final long step;
    private final Function<Long, T> eventCreator;

    /**
     * Instantiates a new TimeSeries.
     * @param pStartTime The time of the first event in the series.
     * @param pEndTime The end time of the series.
     * @param pTimeStep The interval between events.
     * @param pEventCreator The {@link EventCreator} that creates events.
     */
    public TimeSeries(long pStartTime, long pEndTime, long pTimeStep,
        EventCreator<T> pEventCreator) {
      checkArgument(pStartTime < pEndTime, "start time must be before end time");
      checkArgument(pEndTime > 0, "end time must be greater than 0");
      checkArgument(pTimeStep >= 1, "time step must be >= than 1");
      checkArgument(pEventCreator != null, "event creator can not be null");
      start = pStartTime;
      end = pEndTime;
      step = pTimeStep;
      eventCreator = pEventCreator;
    }

    @Override
    public Collection<T> generate() {
      final List<T> result = new LinkedList<T>();
      for (long t = start; t <= end; t += step) {
        result.add(eventCreator.apply(t));
      }
      return result;
    }
  }

  /**
   * Instances create {@link TimedEvent}s at specified times.
   * @param <T> The subtype of {@link TimedEvent} to create.
   * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
   */
  public static abstract class EventCreator<T extends TimedEvent> implements
      Function<Long, T> {}

  /**
   * An {@link EventCreator} that creates {@link TimedEvent}s with the specified
   * type.
   * 
   * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
   * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
   */
  public static class EventTypeFunction extends EventCreator<TimedEvent> {

    private final Enum<?> typeEvent;

    /**
     * @param type The type which events created by this creator will have.
     */
    public EventTypeFunction(Enum<?> type) {
      checkArgument(type != null);
      typeEvent = type;
    }

    @Override
    public TimedEvent apply(Long input) {
      return new TimedEvent(typeEvent, input);
    }

  }

  /**
   * Comparator for comparing {@link TimedEvent}s on their time.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static class TimeComparator implements Comparator<TimedEvent>,
      Serializable {
    private static final long serialVersionUID = -2711991793346719648L;

    /**
     * Instantiate.
     */
    public TimeComparator() {}

    @Override
    public int compare(TimedEvent o1, TimedEvent o2) {
      return (int) (o1.time - o2.time);
    }
  }
}
