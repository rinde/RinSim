/**
 * 
 */
package rinde.sim.core.model.time;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.model.AbstractModel;
import rinde.sim.core.model.ModelManager;
import rinde.sim.event.Event;
import rinde.sim.event.EventDispatcher;

/**
 * 
 * Two users:
 * <ul>
 * <li>TimeController</li>
 * <li>TickListener</li>
 * </ul>
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class TimeModel extends AbstractModel<TimeController> implements Time {

    protected static final Logger LOGGER = LoggerFactory
            .getLogger(TimeModel.class);

    /**
     * Enum that describes the possible types of events that the time model can
     * dispatch.
     */
    public enum TimeModelEventType {
        /**
         * Indicates that the time model has stopped.
         */
        STOPPED,

        /**
         * Indicates that the time model has started.
         */
        STARTED,

    }

    /**
     * Contains the set of registered {@link TickListener}s.
     */
    protected volatile Set<TickListener> tickListeners;

    /**
     * Reference to dispatcher of simulator events, can be used by subclasses to
     * issue additional events.
     */
    protected final EventDispatcher dispatcher;

    /**
     * @see #isPlaying
     */
    protected volatile boolean isPlaying;

    /**
     * @see #getCurrentTime()
     */
    protected long time;

    private final long timeStep;
    private final TimeLapse timeLapse;

    public TimeModel(long step) {
        super(TimeController.class);
        checkArgument(step > 0, "Step must be a positive number.");
        timeStep = step;
        tickListeners = Collections
                .synchronizedSet(new LinkedHashSet<TickListener>());

        time = 0L;
        // time lapse is reused in a Flyweight kind of style
        timeLapse = new TimeLapse();

        dispatcher = new EventDispatcher(TimeModelEventType.values());
    }

    /**
     * @return The current simulation time.
     */
    @Override
    public long getCurrentTime() {
        return time;
    }

    /**
     * @return The time step (in simulation time) which is added to current time
     *         at every tick.
     */
    @Override
    public long getTimeStep() {
        return timeStep;
    }

    /**
     * Adds a tick listener to the simulator.
     * @param listener The listener to add.
     */
    protected void addTickListener(TickListener listener) {
        tickListeners.add(listener);
    }

    /**
     * Removes the listener specified. Implemented in O(1).
     * @param listener The listener to remove
     */
    // FIXME should not be available -> only via SImulator.unregister
    protected void removeTickListener(TickListener listener) {
        tickListeners.remove(listener);
    }

    /**
     * Start the simulation.
     */
    @Override
    public void start() {

        if (!isPlaying) {
            dispatcher
                    .dispatchEvent(new Event(TimeModelEventType.STARTED, this));
        }
        isPlaying = true;
        while (isPlaying) {
            tick();
        }
        dispatcher.dispatchEvent(new Event(TimeModelEventType.STOPPED, this));
    }

    /**
     * Advances the simulator with one step (the size is determined by the time
     * step).
     */
    @Override
    public void tick() {
        // unregister all pending objects
        // unregisterLock.lock();
        // Set<Object> copy;
        // try {
        // copy = toUnregister;
        // toUnregister = new LinkedHashSet<Object>();
        // } finally {
        // // unregisterLock.unlock();
        // }
        //
        // for (final Object c : copy) {
        // modelManager.unregister(c);
        // }

        // using a copy to avoid concurrent modifications of this set
        // this also means that adding or removing a TickListener is
        // effectively executed after a 'tick'

        final List<TickListener> localCopy = new ArrayList<TickListener>();
        long timeS = System.currentTimeMillis();
        localCopy.addAll(tickListeners);

        for (final TickListener t : localCopy) {
            timeLapse.initialize(time, time + timeStep);
            t.tick(timeLapse);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("tick(): " + (System.currentTimeMillis() - timeS));
            timeS = System.currentTimeMillis();
        }
        timeLapse.initialize(time, time + timeStep);
        // in the after tick the TimeLapse can no longer be consumed
        timeLapse.consumeAll();
        for (final TickListener t : localCopy) {
            t.afterTick(timeLapse);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("aftertick(): " + (System.currentTimeMillis() - timeS));
        }
        time += timeStep;

    }

    /**
     * Either starts or stops the simulation depending on the current state.
     */
    @Override
    public void togglePlayPause() {
        if (!isPlaying) {
            start();
        } else {
            isPlaying = false;
        }
    }

    /**
     * Resets the time to 0.
     */
    // public void resetTime() {
    // time = 0L;
    // }

    /**
     * Stops the simulation.
     */
    @Override
    public void stop() {
        isPlaying = false;
    }

    /**
     * @return true if simulator is playing, false otherwise.
     */
    @Override
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * @return An unmodifiable view on the set of tick listeners.
     */
    // public Set<TickListener> getTickListeners() {
    // return Collections.unmodifiableSet(tickListeners);
    // }

    @Override
    public boolean register(TimeController element) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unregister(TimeController element) {
        // TODO Auto-generated method stub
        return false;
    }

    // forwards calls to TimeModel
    private class TickListenerModel extends AbstractModel<TickListener> {

        private final TimeModel timeModel;

        protected TickListenerModel(TimeModel tm) {
            super(TickListener.class);
            timeModel = tm;
        }

        @Override
        public boolean register(TickListener element) {
            timeModel.addTickListener(element);
            return true;
        }

        @Override
        public boolean unregister(TickListener element) {
            timeModel.removeTickListener(element);
            return true;
        }

        @Override
        public void initModel(ModelManager mm) {}

    }

    @Override
    public void initModel(ModelManager mm) {
        mm.add(new TickListenerModel(this));

    }

}
