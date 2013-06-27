/**
 * 
 */
package rinde.sim.core.model.time;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.IBuilder;
import rinde.sim.core.model.AbstractModelLink;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelLink;
import rinde.sim.event.Event;
import rinde.sim.event.EventDispatcher;

import com.google.common.collect.ImmutableList;

/**
 * It is responsible for managing time which it does by periodically providing
 * {@link TimeLapse} instances to registered {@link TickListener}s. Further it
 * provides methods to start and stop simulations.
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
public class TimeModel implements Model, Time {

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

    protected Set<TickListener> tickListeningModels;

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

    protected TimeModel(long step) {
        checkArgument(step > 0, "Step must be a positive number.");
        timeStep = step;
        tickListeners = Collections
                .synchronizedSet(new LinkedHashSet<TickListener>());

        tickListeningModels = newLinkedHashSet();

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
    protected boolean addTickListener(TickListener listener) {
        if (listener instanceof Model) {
            return tickListeningModels.add(listener);
        }
        return tickListeners.add(listener);
    }

    /**
     * Removes the listener specified. Implemented in O(1).
     * @param listener The listener to remove
     */
    protected boolean removeTickListener(TickListener listener) {
        checkArgument(!(listener instanceof Model), "Models can not be removed");
        return tickListeners.remove(listener);
    }

    protected boolean registerTimeController(TimeController tc) {
        tc.receiveTime(this);
        return true;
    }

    protected boolean unregisterTimeController(TimeController tc) {
        return true;
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

        // final List<TickListener> localCopy = new ArrayList<TickListener>();
        long timeS = System.currentTimeMillis();
        // localCopy.addAll(tickListeners);

        for (final TickListener t : tickListeningModels) {
            timeLapse.initialize(time, time + timeStep);
            timeLapse.consumeAll();
            t.tick(timeLapse);
        }

        for (final TickListener t : tickListeners) {
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
        for (final TickListener t : tickListeners) {
            t.afterTick(timeLapse);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("aftertick(): " + (System.currentTimeMillis() - timeS));
        }

        for (final TickListener t : tickListeningModels) {
            timeLapse.initialize(time, time + timeStep);
            timeLapse.consumeAll();
            t.afterTick(timeLapse);
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

    @Override
    public List<ModelLink<?>> getModelLinks() {
        return ImmutableList
                .of((ModelLink<?>) new TimeControllerLink(), new TickListenerLink());
    }

    /**
     * @return An unmodifiable view on the set of tick listeners.
     */
    // public Set<TickListener> getTickListeners() {
    // return Collections.unmodifiableSet(tickListeners);
    // }
    private class TimeControllerLink extends AbstractModelLink<TimeController> {

        protected TimeControllerLink() {
            super(TimeController.class);
        }

        @Override
        public boolean register(TimeController element) {
            return registerTimeController(element);
        }

        @Override
        public boolean unregister(TimeController element) {
            return unregisterTimeController(element);
        }
    }

    // forwards calls to TimeModel
    private class TickListenerLink extends AbstractModelLink<TickListener> {

        protected TickListenerLink() {
            super(TickListener.class);
        }

        @Override
        public boolean register(TickListener element) {
            return addTickListener(element);
        }

        @Override
        public boolean unregister(TickListener element) {
            return removeTickListener(element);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static TimeModel build(long ts) {
        return builder().withTimeStep(ts).build();
    }

    public static class Builder implements IBuilder<TimeModel> {

        long timeStep;

        public Builder withTimeStep(long ts) {
            timeStep = ts;
            return this;
        }

        @Override
        public TimeModel build() {
            return new TimeModel(timeStep);
        }
    }

}
