/**
 * 
 */
package rinde.sim.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.random.RandomGenerator;

import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelManager;
import rinde.sim.event.Event;
import rinde.sim.event.EventDispatcher;
import rinde.sim.event.Events;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class Simulator {

	/**
	 * Enum that describes the possible events from simulator itself
	 *
	 */
	public enum EventTypes {
		STOPPED, STARTED
	}

	protected volatile Set<TickListener> tickListeners;
	protected List<TickListener> afterTickListeners;
	
	public final RandomGenerator rand;
	public final Events events;
	protected final EventDispatcher dispatcher;
	protected final long timeStep;
	protected boolean isPlaying;
	protected long time;
	
	protected ModelManager modelManager;

	public void configure() {
		modelManager.configure();
	}
	
	

	public boolean register(Model<?> model) {
		if(model instanceof TickListener) {
			addTickListener((TickListener) model);
		}
		return modelManager.register(model);
	}

	public boolean register(Object o) {
		if(o instanceof TickListener) {
			//FIXME refactor the TickListener interface
			addTickListener((TickListener) o);
		}
		return modelManager.register(o);
	}

	/**
	 * Returns a safe to modify list of all models registered in the simulator
	 * @return list of models
	 */
	public List<Model<?>> getModels() {
		return modelManager.getModels();
	}

	/**
	 * @param model The model that this simulator instance is using
	 * @param r The random number generator that is used in this simulator.
	 * @param timeStep The time that passes each tick. This can be in any unit
	 *            the programmer prefers.
	 */
	public Simulator(RandomGenerator r, long timeStep) {
		this.timeStep = timeStep;
		tickListeners = Collections.synchronizedSet(new LinkedHashSet<TickListener>());
		afterTickListeners = new ArrayList<TickListener>();

		rand = r;
		time = 0L;

		modelManager = new ModelManager();
		
		dispatcher = new EventDispatcher(EventTypes.STOPPED, EventTypes.STARTED);
		events = dispatcher.getEvents();
	}

	public long getCurrentTime() {
		return time;
	}

	public long getTimeStep() {
		return timeStep;
	}

	public void addAfterTickListener(TickListener t) {
		afterTickListeners.add(t);
	}

	public void addTickListener(TickListener listener) {
		tickListeners.add(listener);
	}

	/**
	 * O(1)
	 */
	public void removeTickListener(TickListener listener) {
		tickListeners.remove(listener);
	}

	/**
	 * Start the simulation
	 */
	public void start() {
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
		// using a copy to avoid concurrent modifications of this set
		// this also means that adding or removing a TickListener is 
		// effectively executed after a 'tick'

		List<TickListener> localCopy = new ArrayList<TickListener>();
		localCopy.addAll(tickListeners);
		for (TickListener t : localCopy) {
			t.tick(time, timeStep);
		}

		for (TickListener t : afterTickListeners) {
			t.tick(time, timeStep);
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
}