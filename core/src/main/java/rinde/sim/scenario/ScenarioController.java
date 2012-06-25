package rinde.sim.scenario;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;

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
import rinde.sim.event.pdp.StandardType;

/**
 * A scenario controller represents a single simulation run. This class is
 * intended for extension.
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public abstract class ScenarioController implements TickListener, Listener {

	public enum Type {
		SCENARIO_STARTED, SCENARIO_FINISHED;
	}

	protected static final Logger LOGGER = LoggerFactory.getLogger(ScenarioController.class);

	protected final Scenario scenario;
	private int ticks;
	private final EventDispatcher disp;
	public final EventAPI eventAPI;

	Simulator simulator;

	private Type status = null;

	/**
	 * <code>true</code> when user interface was defined. In ui mode
	 */
	private boolean uiMode;

	/**
	 * Create an instance of ScenarioController with defined {@link Scenario}
	 * and number of ticks till end. If the number of ticks is negative the
	 * simulator will run until the {@link Simulator#stop()} method is called.
	 * TODO refine documentation
	 * 
	 * @param scen to realize
	 * @param numberOfTicks when negative the number of tick is infinite
	 */
	public ScenarioController(final Scenario scen, int numberOfTicks) {
		checkArgument(scen != null, "scenario can not be null");
		ticks = numberOfTicks;
		scenario = new Scenario(scen);

		Set<Enum<?>> typeSet = newHashSet(scenario.getPossibleEventTypes());
		typeSet.addAll(asList(Type.values()));
		disp = new EventDispatcher(typeSet);
		eventAPI = disp.getEventAPI();
		disp.addListener(this, scenario.getPossibleEventTypes());
	}

	/**
	 * Method that initializes the simulator using
	 * {@link ScenarioController#createSimulator()} and user interface (if
	 * defined) using {@link ScenarioController#createUserInterface()}. Must be
	 * called from within a constructor of specialized class.
	 * @throws ConfigurationException
	 */
	final protected void initialize() throws ConfigurationException {
		try {
			simulator = createSimulator();
		} catch (Exception e) {
			LOGGER.warn("exception thrown during createSimulator()", e);
			throw new ConfigurationException("An exception was thrown while instantiating the simulator", e);
		}
		checkSimulator();
		simulator.configure();
		LOGGER.info("simulator created");

		simulator.addTickListener(this);

		uiMode = createUserInterface();
	}

	/**
	 * Access the simulator from the subclasses. Method returns simulator only
	 * after calling {@link ScenarioController#initialize()}.
	 * @return simulator or <code>null</code>
	 */
	public Simulator getSimulator() {
		return simulator;
	}

	/**
	 * Create simulator that will run the scenario.
	 * 
	 * @postcondition simulator != null && simulator not configured
	 * @return simulator
	 * @throws Exception
	 */
	protected abstract Simulator createSimulator() throws Exception;

	/**
	 * Create the user interface. By default method is empty and disables uiMode
	 * 
	 * @precondition simulator != null and simulator is configured
	 * @return uiMode. should be <code>true</code> when user interface was
	 *         created.
	 */
	protected boolean createUserInterface() {
		return false;
	}

	/**
	 * Stop the simulation.
	 */
	public void stop() {
		if (!uiMode) {
			simulator.removeTickListener(this);
			simulator.stop();
		}
	}

	/**
	 * Starts the simulation.
	 * @throws ConfigurationException If the scenario controller was not
	 *             configured properly.
	 */
	public void start() throws ConfigurationException {
		checkSimulator();
		if (ticks != 0 && !uiMode) {
			new Thread() {
				@Override
				public void run() {
					simulator.start();
				}
			}.start();
		}

	}

	/**
	 * @return <code>true</code> if all events of this scenario have been
	 *         dispatched, <code>false</code> otherwise.
	 */
	public boolean isScenarioFinished() {
		return scenario.peek() == null;
	}

	@Override
	final public void tick(TimeLapse timeLapse) {
		if (!uiMode && ticks == 0) {
			LOGGER.info("scenario finished at virtual time:" + timeLapse.getTime());
			simulator.stop();
		}
		if (LOGGER.isDebugEnabled() && ticks >= 0) {
			LOGGER.debug("ticks to end: " + ticks);
		}
		if (ticks > 0) {
			ticks--;
		}
		TimedEvent e = null;
		while ((e = scenario.peek()) != null && e.time <= timeLapse.getTime()) {
			scenario.poll();
			if (status == null) {
				LOGGER.info("scenario started at virtual time:" + timeLapse.getTime());
				status = Type.SCENARIO_STARTED;
				disp.dispatchEvent(new Event(status, this));
			}
			e.setIssuer(this);
			disp.dispatchEvent(e);
		}
		if (e == null && status != Type.SCENARIO_FINISHED) {
			LOGGER.info("scenario finished at virtual time:" + timeLapse.getTime());
			status = Type.SCENARIO_FINISHED;
			simulator.removeTickListener(this);
			disp.dispatchEvent(new Event(status, this));
		}

	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// not needed
	}

	@Override
	public void handleEvent(Event e) {
		if (e.getEventType() instanceof StandardType) {
			boolean handled = handleStandard(e);
			if (handled) {
				return;
			}
		}
		if (!handleCustomEvent(e)) {
			LOGGER.warn("event not handled: " + e.toString());
			throw new IllegalArgumentException("event not handled: " + e.toString());
		}
	}

	/**
	 * Can be used to handle additional events not supported by default. Default
	 * implementation lead to the {@link IllegalArgumentException} during event
	 * handling {@link ScenarioController#handleEvent(Event)}
	 * @param e
	 * @return <code>false</code> by default.
	 */
	protected boolean handleCustomEvent(Event e) {
		return false;
	}

	final boolean handleStandard(Event e) {
		StandardType eT = (StandardType) e.getEventType();
		switch (eT) {
		case ADD_PACKAGE:
			return handleAddPackage(e);
		case REMOVE_PACKAGE:
			return handleRemovePackage(e);
		case ADD_TRUCK:
			return handleAddTruck(e);
		case REMOVE_TRUCK:
			return handleRemoveTruck(e);
		default:
			return false;
		}
	}

	/**
	 * Is called when an event of type {@link StandardType#REMOVE_TRUCK} occurs.
	 * This method is normally overridden to add application specific actions to
	 * this event. This method should return <code>true</code> if it has handled
	 * the event, otherwise <code>false</code> should be returned.
	 * @param e
	 * @return <code>false</code>
	 */
	protected boolean handleRemoveTruck(Event e) {
		return false;
	}

	/**
	 * Is called when an event of type {@link StandardType#ADD_TRUCK} occurs.
	 * This method is normally overridden to add application specific actions to
	 * this event. This method should return <code>true</code> if it has handled
	 * the event, otherwise <code>false</code> should be returned.
	 * @param event
	 * @return <code>false</code>
	 */
	protected boolean handleAddTruck(Event event) {
		return false;
	}

	/**
	 * Is called when an event of type {@link StandardType#REMOVE_PACKAGE}
	 * occurs. This method is normally overridden to add application specific
	 * actions to this event. This method should return <code>true</code> if it
	 * has handled the event, otherwise <code>false</code> should be returned.
	 * @param event
	 * @return <code>false</code>
	 */
	protected boolean handleRemovePackage(Event event) {
		return false;
	}

	/**
	 * Is called when an event of type {@link StandardType#ADD_PACKAGE} occurs.
	 * This method is normally overridden to add application specific actions to
	 * this event. This method should return <code>true</code> if it has handled
	 * the event, otherwise <code>false</code> should be returned.
	 * @param event
	 * @return <code>false</code>
	 */
	protected boolean handleAddPackage(Event event) {
		return false;
	}

	private final void checkSimulator() throws ConfigurationException {
		if (simulator == null) {
			throw new ConfigurationException(
					"use createSimulator() to define simulator and make sure initialize() is called before calling start()");
		}
	}
}
