package rinde.sim.scenario;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;

import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.event.Event;
import rinde.sim.event.EventAPI;
import rinde.sim.event.EventDispatcher;
import rinde.sim.event.Listener;

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
     * Provides access to the {@link Event} API, allows adding and removing
     * {@link Listener}s that are notified when {@link ScenarioController}
     * dispatches {@link Event}s.
     */
    protected final EventAPI eventAPI;

    /**
     * The scenario that is played.
     */
    protected final Scenario scenario;

    /**
     * The {@link Event} queue.
     */
    protected final Queue<TimedEvent> scenarioQueue;
    protected final EventDispatcher disp;
    protected final Simulator simulator;

    protected UICreator uiCreator;
    protected TimedEventHandler timedEventHandler;

    private int ticks;
    private EventType status;
    private boolean uiMode;

    /**
     * Create an instance of ScenarioController with defined {@link Scenario}
     * and number of ticks till end. If the number of ticks is negative the
     * simulator will run until the {@link Simulator#stop()} method is called.
     * TODO refine documentation
     * 
     * @param scen Scenario which is controlled.
     * @param sim Simulator which is controlled.
     * @param eventHandler Is used to handle scenario events.
     * @param numberOfTicks The number of ticks play, when negative the number
     *            of tick is infinite.
     */
    public ScenarioController(final Scenario scen, Simulator sim,
            TimedEventHandler eventHandler, int numberOfTicks) {

        scenario = scen;
        simulator = sim;
        timedEventHandler = eventHandler;
        ticks = numberOfTicks;

        scenarioQueue = scenario.asQueue();

        final Set<Enum<?>> typeSet = newHashSet(scenario
                .getPossibleEventTypes());
        typeSet.addAll(asList(EventType.values()));
        disp = new EventDispatcher(typeSet);
        eventAPI = disp.getEventAPI();
        disp.addListener(new InternalTimedEventHandler(), scenario
                .getPossibleEventTypes());

        simulator.addTickListener(this);
        simulator.configure();

    }

    /**
     * Enables the UI for this scenario controller. This means that when
     * {@link #start()} is called the UI is fired up. Using {@link UICreator}
     * any kind of UI can be hooked to the simulation.
     * @param creator The creator of the UI.
     */
    public void enableUI(UICreator creator) {
        uiMode = true;
        uiCreator = creator;
    }

    /**
     * @return The event API of the scenario controller.
     */
    public EventAPI getEventAPI() {
        return eventAPI;
    }

    /**
     * Stop the simulation.
     */
    public void stop() {
        if (!uiMode) {
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

            if (!uiMode) {
                simulator.start();
            } else {
                uiCreator.createUI(simulator);
            }
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
    final public void tick(TimeLapse timeLapse) {
        if (!uiMode && ticks == 0) {
            LOGGER.info("scenario finished at virtual time:"
                    + timeLapse.getTime() + "[stopping simulation]");
            simulator.stop();
        }
        if (LOGGER.isDebugEnabled() && ticks >= 0) {
            LOGGER.debug("ticks to end: " + ticks);
        }
        if (ticks > 0) {
            ticks--;
        }
        TimedEvent e = null;

        while ((e = scenarioQueue.peek()) != null
                && e.time <= timeLapse.getTime()) {
            scenarioQueue.poll();
            if (status == null) {
                LOGGER.info("scenario started at virtual time:"
                        + timeLapse.getTime());
                status = EventType.SCENARIO_STARTED;
                disp.dispatchEvent(new Event(status, this));
            }
            e.setIssuer(this);
            disp.dispatchEvent(e);
        }
        if (e == null && status != EventType.SCENARIO_FINISHED) {
            status = EventType.SCENARIO_FINISHED;
            disp.dispatchEvent(new Event(status, this));
        }
        if (ticks == 0 && status == EventType.SCENARIO_FINISHED) {
            LOGGER.info("scenario finished at virtual time:"
                    + timeLapse.getTime() + "[stopping simulation]");
            simulator.stop();
            simulator.removeTickListener(this);
        }

    }

    @Override
    public void afterTick(TimeLapse timeLapse) {} // not needed

    /**
     * A UICreator can be used to dynamically create a UI for the simulation
     * run. It can be used with any kind of GUI imaginable.
     * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
     */
    public interface UICreator {
        /**
         * Should instantiate the UI.
         * @param sim The {@link Simulator} instance for which the UI should be
         *            created.
         */
        void createUI(Simulator sim);
    }

    class InternalTimedEventHandler implements Listener {

        public InternalTimedEventHandler() {}

        @Override
        public final void handleEvent(Event e) {
            if (!timedEventHandler.handleTimedEvent((TimedEvent) e)) {
                throw new IllegalArgumentException("event not handled: "
                        + e.toString());
            }
        }
    }

}
