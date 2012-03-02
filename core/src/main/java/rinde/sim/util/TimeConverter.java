package rinde.sim.util;

/**
 * Simple utility class to convert 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class TimeConverter {
	
	private static final long SEC = 1000;
	private static final long MIN = 60 * SEC;
	private static final long HOUR = 60 * MIN;
	private static final long DAY = 24 * HOUR;
	
	private long time = 0;
	private final long tickLength;
	
	public TimeConverter(long tickLength) {
		this.tickLength = tickLength; 
	}
	
	public TimeConverter day(int days) {
		time += days * DAY;
		return this;
	}
	
	public TimeConverter hour(int hours) {
		time += hours * HOUR;
		return this;
	}
	
	public TimeConverter min(int minutes) {
		time += minutes * MIN;
		return this;
	}
	
	public TimeConverter sec(int seconds) {
		time += seconds * SEC;
		return this;
	}
	
	public TimeConverter mili(long milisec) {
		time += milisec;
		return this;
	}
	
	public TimeConverter tick(int ticks) {
		time += ticks * tickLength;
		return this;
	}
	
	/**
	 * Get the time.
	 * @return
	 */
	public long toTime() {
		long res = time;
		time = 0;
		return res;
	}
}
