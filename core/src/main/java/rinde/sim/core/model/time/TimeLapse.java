/**
 * 
 */
package rinde.sim.core.model.time;

import static com.google.common.base.Preconditions.checkArgument;
import rinde.sim.core.Simulator;

/**
 * Represents a consumable interval of time: [start, end). Instances of time
 * lapse are handed out by the {@link Simulator} and can be received by
 * implementing the {@link TickListener} interface.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public final class TimeLapse {

    private long startTime;
    private long endTime;
    private long timeLeft;

    TimeLapse() {}

    TimeLapse(long start, long end) {
        initialize(start, end);
    }

    void initialize(long start, long end) {
        checkArgument(start >= 0, "time must be positive");
        checkArgument(end > start, "end time must be after start time");
        startTime = start;
        endTime = end;
        timeLeft = end - start;
    }

    /**
     * Consumes the specified amount of time, where time must be strictly
     * positive and there must be enough time left as specified by
     * {@link #getTimeLeft()}.
     * @param time The time to consume.
     */
    public void consume(long time) {
        checkArgument(time >= 0, "the time to consume must be a positive value");
        checkArgument(timeLeft - time >= 0, "there is not enough time left to consume %s.", time);
        timeLeft -= time;
    }

    /**
     * Consumes the entire time lapse.
     */
    public void consumeAll() {
        timeLeft = 0;
    }

    /**
     * @return If there is time left to consume.
     */
    public boolean hasTimeLeft() {
        return timeLeft > 0;
    }

    /**
     * @return The amount of time left to consume.
     */
    public long getTimeLeft() {
        return timeLeft;
    }

    /**
     * @return The current time taking into account any time consumption in this
     *         instance. When looking for the time at the start of this time
     *         lapse use {@link #getStartTime()}.
     */
    public long getTime() {
        return endTime - timeLeft;
    }

    /**
     * @return The step (or length) of this time lapse.
     */
    public long getTimeStep() {
        return endTime - startTime;
    }

    /**
     * @return The time that was previously consumed in this time lapse.
     */
    public long getTimeConsumed() {
        return (endTime - startTime) - timeLeft;
    }

    /**
     * @return The start time of this time lapse.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @return The end time of this time lapse.
     */
    public long getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return new StringBuilder("[").append(startTime).append(",")
                .append(endTime).append(")").toString();
    }
}
