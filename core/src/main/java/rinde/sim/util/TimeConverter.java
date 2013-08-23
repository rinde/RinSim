package rinde.sim.util;

import static rinde.sim.util.TimeUnit.D;
import static rinde.sim.util.TimeUnit.H;
import static rinde.sim.util.TimeUnit.M;
import static rinde.sim.util.TimeUnit.S;

/**
 * 
 * Simple utility class to convert
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class TimeConverter {
  // TODO use joda time for this?

  private long time = 0;
  private final long tickLength;

  public TimeConverter(long tickLength) {
    this.tickLength = tickLength;
  }

  public TimeConverter day(int days) {
    time += D.toMs(days);
    return this;
  }

  public TimeConverter hour(int hours) {
    time += H.toMs(hours);
    return this;
  }

  public TimeConverter min(int minutes) {
    time += M.toMs(minutes);
    return this;
  }

  public TimeConverter sec(int seconds) {
    time += S.toMs(seconds);
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
    final long res = time;
    time = 0;
    return res;
  }
}
