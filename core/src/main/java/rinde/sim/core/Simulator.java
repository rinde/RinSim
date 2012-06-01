/**
 * 
 */
package rinde.sim.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelManager;
import rinde.sim.event.Event;
import rinde.sim.event.EventDispatcher;
import rinde.sim.event.EventAPI;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be> - simulator API
 *         changes
 * 
 */
public class Simulator implements SimulatorAPI {

	protected static final Logger LOGGER = LoggerFactory.getLogger(Simulator.class);

	/**
	 * Enum that describes the possible events from simulator itself
	 * 
	 */
	public enum EventTypes {
		STOPPED, STARTED
	}

	protected volatile Set<TickListener> tickListeners;

	private final RandomGenerator rand;
	public final EventAPI events;
	protected final EventDispatcher dispatcher;
	protected final long timeStep;
	protected volatile boolean isPlaying;
	protected long time;

	protected ModelManager modelManager;
	private boolean configured;

	private Set<Object> toUnregister;
	private final ReentrantLock unregisterLock;

	/**
	 * @param r The random number generator that is used in this simulator.
	 * @param step The time that passes each tick. This can be in any unit the
	 *            programmer prefers.
	 */
	public Simulator(RandomGenerator r, long step) {
		timeStep = step;
		tickListeners = Collections.synchronizedSet(new LinkedHashSet<TickListener>());

		unregisterLock = new ReentrantLock();
		toUnregister = new LinkedHashSet<Object>();

		rand = r;
		time = 0L;

		modelManager = new ModelManager();

		dispatcher = new EventDispatcher(EventTypes.STOPPED, EventTypes.STARTED);
		events = dispatcher.getEventAPI();
	}

	public void configure() {
		modelManager.configure();
		configured = true;
	}

	/**
	 * Register a model to the simulator.
	 * @param model The {@link Model} instance to register.
	 * @return true if succesful, false otherwise
	 */
	public boolean register(Model<?> model) {
		if (model == null) {
			throw new IllegalArgumentException("parameter cannot be null");
		}
		if (configured) {
			throw new IllegalStateException("cannot add model after calling configure()");
		}
		boolean result = modelManager.add(model);
		if (result) {
			LOGGER.info("registering model :" + model.getClass().getName() + " for type:"
					+ model.getSupportedType().getName());
			if (model instanceof TickListener) {
				LOGGER.info("adding " + model.getClass().getName() + " as a tick listener");
				addTickListener((TickListener) model);
			}
		}
		return result;
	}

	/**
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public boolean register(Object obj) {
		if (obj == null) {
			throw new IllegalArgumentException("parameter can not be null");
		}
		if (obj instanceof Model<?>) {
			return register((Model<?>) obj);
		}
		if (!configured) {
			throw new IllegalStateException("can not add object before calling configure()");
		}
		injectDependencies(obj);
		if (obj instanceof TickListener) {
			addTickListener((TickListener) obj);
		}
		return modelManager.register(obj);
	}

	/**
	 * Unregistration from the models is delayed until all ticks are processed
	 * 
	 * @see rinde.sim.core.SimulatorAPI#unregister(java.lang.Object)
	 */
	@Override
	public boolean unregister(Object o) {
		if (o == null) {
			throw new IllegalArgumentException("parameter cannot be null");
		}
		if (o instanceof Model<?>) {
			throw new IllegalArgumentException("can not unregister a model");
		}
		if (!configured) {
			throw new IllegalStateException("can not unregister object before calling configure()");
		}
		if (o instanceof TickListener) {
			removeTickListener((TickListener) o);
		}
		unregisterLock.lock();
		toUnregister.add(o);
		unregisterLock.unlock();
		return true;
	}

	/**
	 * Inject all required dependecies basing on the declared types of the
	 * object
	 * @param o object that need to have dependecies injected
	 */
	protected void injectDependencies(Object o) {
		if (o instanceof SimulatorUser) {
			((SimulatorUser) o).setSimulator(this);
		}
	}

	/**
	 * Returns a safe to modify list of all models registered in the simulator
	 * @return list of models
	 */
	public List<Model<?>> getModels() {
		return modelManager.getModels();
	}

	public long getCurrentTime() {
		return time;
	}

	public long getTimeStep() {
		return timeStep;
	}

	public void addTickListener(TickListener listener) {
		tickListeners.add(listener);
	}

	/**
	 * O(1)
	 * @param listener The listener to remove
	 */
	public void removeTickListener(TickListener listener) {
		tickListeners.remove(listener);
	}

	/**
	 * Start the simulation
	 */
	public void start() {
		if (!configured) {
			throw new IllegalStateException("Simulator can not be started when it is not configured.");
		}
		if (!isPlaying) {
			dispatcher.dispatchEvent(new Event(EventTypes.STARTED, this));
		}
		isPlaying = true;
		while (isPlaying) {
			tick();
		}
		dispatcher.dispatchEvent(new Event(EventTypes.STOPPED, this));
	}

	public void tick() {
		// unregister all pending objects
		unregisterLock.lock();
		Set<Object> copy = toUnregister;
		toUnregister = new LinkedHashSet<Object>();
		unregisterLock.unlock();

		for (Object c : copy) {
			modelManager.unregister(c);
		}

		// using a copy to avoid concurrent modifications of this set
		// this also means that adding or removing a TickListener is
		// effectively executed after a 'tick'

		List<TickListener> localCopy = new ArrayList<TickListener>();
		long timeS = System.currentTimeMillis();
		localCopy.addAll(tickListeners);
		for (TickListener t : localCopy) {
			t.tick(time, timeStep);
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("tick(): " + (System.currentTimeMillis() - timeS));
			timeS = System.currentTimeMillis();
		}
		for (TickListener t : tickListeners) {
			t.afterTick(time, timeStep);
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("aftertick(): " + (System.currentTimeMillis() - timeS));
		}

		time += timeStep;

	}

	/**
	 * Either starts or stops the simulation depending on the current state.
	 */
	public void togglePlayPause() {
		isPlaying = !isPlaying;
		if (isPlaying) {
			start();
		}
	}

	public void resetTime() {
		time = 0L;
	}

	/**
	 * Stops the simulation
	 */
	public void stop() {
		isPlaying = false;
	}

	/**
	 * @return true if simulator is playing, false otherwise.
	 */
	public boolean isPlaying() {
		return isPlaying;
	}

	public Set<TickListener> getTickListeners() {
		return Collections.unmodifiableSet(tickListeners);
	}

	@Override
	public RandomGenerator getRandomGenerator() {
		return rand;
	}
}
