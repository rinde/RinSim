/**
 * 
 */
package rinde.sim.core;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a consumable interval of time.
 * 
 * [start, end)
 * 
 * TODO should this class be final or not?
 * 
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class TimeLapse {

	protected final long startTime;
	protected long endTime;
	protected long timeLeft;

	TimeLapse(long start, long end) {
		checkArgument(start > 0, "time must be positive");
		checkArgument(end > start, "end time must be after start time");
		startTime = start;
		endTime = end;
		timeLeft = end - start;
	}

	public void consume(long time) {
		checkArgument(time > 0, "the time to consume must be a positive value");
		checkArgument(timeLeft - time >= 0, "there must be enough time left to consume it");
		timeLeft -= time;
	}

	public void consumeAll() {
		timeLeft = 0;
	}

	public boolean hasTimeLeft() {
		return timeLeft > 0;
	}

	public long getTimeLeft() {
		return timeLeft;
	}

	public long getTime() {
		return endTime - timeLeft;
	}

	public long getPeriod() {
		return endTime - startTime;
	}

	public long getTimeConsumed() {
		return (startTime - endTime) - timeLeft;
	}

	// splits the lapse in two parts. this part will be the earlier part, the
	// returned path is later part.
	public TimeLapse split(long length) {
		checkArgument(length > 0, "the split length must be positive");
		checkArgument(timeLeft - length >= 0, "the time to split must be available");
		long end = endTime;
		endTime -= length;
		timeLeft -= length;
		return new TimeLapse(endTime, end);
	}
}
