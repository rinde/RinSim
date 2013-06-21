/**
 * 
 */
package rinde.sim.core.model.time;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import rinde.sim.event.EventDispatcher;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class TimeModel {

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
        checkArgument(step > 0, "Step must be a positive number.");
        timeStep = step;
        tickListeners = Collections
                .synchronizedSet(new LinkedHashSet<TickListener>());

        time = 0L;
        // time lapse is reused in a Flyweight kind of style
        timeLapse = new TimeLapse();

        dispatcher = new EventDispatcher(TimeModelEventType.values());
    }

}
