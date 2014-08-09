package com.github.rinde.rinsim.scenario;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;

import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.Simulator.SimulatorEventType;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.event.Listener;
import com.google.common.base.Optional;

/**
 * A scenario controller represents a single simulation run using a
 * {@link Scenario}. The scenario controller makes sure that all events in the
 * scenario are dispatched at their respective time and it checks whether they
 * are handled.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class ScenarioController implements TickListener {
  // rename to event scheduler?
  /**
   * Logger for this class.
   */
  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ScenarioController.class);

  /**
   * The {@link Event} types which can be dispatched by this class.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public enum EventType {
    /**
     * Dispatched when the scenario starts playing.
     */
    SCENARIO_STARTED,
    /**
     * Dispatched when the scenario has finished playing.
     */
    SCENARIO_FINISHED;
  }

  /**
   * The scenario that is played.
   */
  protected final Scenario scenario;

  /**
   * The {@link Event} queue.
   */
  protected final Queue<TimedEvent> scenarioQueue;

  /**
   * The {@link EventDispatcher} that is used for dispatching all events.
   */
  protected final EventDispatcher disp;

  /**
   * A reference to the simulator.
   */
  protected final Simulator simulator;

  /**
   * A reference to the {@link UICreator} that is responsible for creating the
   * UI.
   */
  protected Optional<UICreator> uiCreator;

  /**
   * A handler for the TimedEvents.
   */
  protected TimedEventHandler timedEventHandler;

  private int ticks;
  @Nullable
  private EventType status;

  // TODO ScenarioController should be added to Simulator not other way
  // around.
  /**
   * Create an instance of ScenarioController with defined {@link Scenario} and
   * number of ticks till end. If the number of ticks is negative the simulator
   * will run until the {@link Simulator#stop()} method is called. TODO refine
   * documentation
   * 
   * @param scen Scenario which is controlled.
   * @param sim Simulator which is controlled.
   * @param eventHandler Is used to handle scenario events.
   * @param numberOfTicks The number of ticks play, when negative the number of
   *          tick is infinite.
   */
  public ScenarioController(final Scenario scen, Simulator sim,
      TimedEventHandler eventHandler, int numberOfTicks) {

    scenario = scen;
    simulator = sim;
    timedEventHandler = eventHandler;
    ticks = numberOfTicks;
    uiCreator = Optional.absent();

    scenarioQueue = scenario.asQueue();

    final Set<Enum<?>> typeSet = newHashSet(scenario.getPossibleEventTypes());
    typeSet.addAll(asList(EventType.values()));
    disp = new EventDispatcher(typeSet);
    disp.addListener(new InternalTimedEventHandler(),
        scenario.getPossibleEventTypes());

    simulator.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        if (simulator.getCurrentTime() == 0) {
          dispatchSetupEvents();
        }

      }
    }, SimulatorEventType.STARTED);
    simulator.addTickListener(this);

  }

  // TODO add UICreator directly to Simulator?
  /**
   * Enables the UI for this scenario controller. This means that when
   * {@link #start()} is called the UI is fired up. Using {@link UICreator} any
   * kind of UI can be hooked to the simulation.
   * @param creator The creator of the UI.
   */
  public void enableUI(UICreator creator) {
    uiCreator = Optional.of(creator);
  }

  /**
   * Provides access to the {@link Event} API, allows adding and removing
   * {@link Listener}s that are notified when {@link ScenarioController}
   * dispatches {@link Event}s.
   * @return The event API of the scenario controller.
   */
  public EventAPI getEventAPI() {
    return disp.getPublicEventAPI();
  }

  /**
   * Stop the simulation.
   */
  public void stop() {
    if (!uiCreator.isPresent()) {
      simulator.stop();
    }
  }

  /**
   * Starts the simulation, if UI is enabled it will start the UI instead.
   * @see #enableUI(UICreator)
   * @see #stop()
   */
  public void start() {
    if (ticks != 0) {
      if (uiCreator.isPresent()) {
        uiCreator.get().createUI(simulator);
      } else {
        simulator.start();
      }
    }
  }

  /**
   * Dispatch all setup events (the ones that define initial settings). For
   * example, a vehicle that is added during setup (at time < 0) will receive
   * its first tick at time 0. If the vehicle is added at the beginning of the
   * simulation (time 0) the first tick it will receive will be the second
   * (globally) tick.
   */
  protected void dispatchSetupEvents() {
    TimedEvent e = null;
    while ((e = scenarioQueue.peek()) != null && e.time < 0) {
      scenarioQueue.poll();
      disp.dispatchEvent(e);
    }
  }

  /**
   * @return <code>true</code> if all events of this scenario have been
   *         dispatched, <code>false</code> otherwise.
   */
  public boolean isScenarioFinished() {
    return scenarioQueue.isEmpty();
  }

  @Override
  public final void tick(TimeLapse timeLapse) {
    if (!uiCreator.isPresent() && ticks == 0) {
      LOGGER.info("scenario finished at virtual time:" + timeLapse.getTime()
          + "[stopping simulation]");
      simulator.stop();
    }
    if (LOGGER.isDebugEnabled() && ticks >= 0) {
      LOGGER.debug("ticks to end: " + ticks);
    }
    if (ticks > 0) {
      ticks--;
    }
    TimedEvent e = null;

    while ((e = scenarioQueue.peek()) != null && e.time <= timeLapse.getTime()) {
      scenarioQueue.poll();
      if (status == null) {
        LOGGER.info("scenario started at virtual time:" + timeLapse.getTime());
        status = EventType.SCENARIO_STARTED;
        disp.dispatchEvent(new Event(status, this));
      }
      disp.dispatchEvent(e);
    }
    if (e == null && status != EventType.SCENARIO_FINISHED) {
      status = EventType.SCENARIO_FINISHED;
      disp.dispatchEvent(new Event(status, this));
    }
    if (ticks == 0 && status == EventType.SCENARIO_FINISHED) {
      LOGGER.info("scenario finished at virtual time:" + timeLapse.getTime()
          + "[stopping simulation]");
      simulator.stop();
      simulator.removeTickListener(this);
    }

  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  /**
   * A UICreator can be used to dynamically create a UI for the simulation run.
   * It can be used with any kind of GUI imaginable.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public interface UICreator {
    // TODO convert to use View.Builder ?
    /**
     * Should instantiate the UI.
     * @param sim The {@link Simulator} instance for which the UI should be
     *          created.
     */
    void createUI(Simulator sim);
  }

  class InternalTimedEventHandler implements Listener {

    public InternalTimedEventHandler() {}

    @Override
    public final void handleEvent(Event e) {
      checkState(timedEventHandler.handleTimedEvent((TimedEvent) e),
          "The event %s is not handled.", e.getEventType());
    }
  }

}
