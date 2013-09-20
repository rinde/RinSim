package rinde.sim.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

/**
 * A time window is an interval in time. It is defined as a half open interval:
 * [begin, end).
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class TimeWindow implements Serializable {
  private static final long serialVersionUID = 7548761538022038612L;

  /**
   * A time window which represents 'always'.
   */
  public static TimeWindow ALWAYS = new TimeWindow(0, Long.MAX_VALUE);

  /**
   * Begin of the time window (inclusive). Must be a non-negative value.
   */
  public final long begin;

  /**
   * End of the time window (exclusive).
   */
  public final long end;

  /**
   * Create a new time window [begin, end).
   * @param pBegin {@link #begin}.
   * @param pEnd {@link #end}.
   */
  public TimeWindow(long pBegin, long pEnd) {
    checkArgument(pBegin >= 0, "Time must be non-negative.");
    checkArgument(pBegin <= pEnd, "Begin can not be later than end.");
    begin = pBegin;
    end = pEnd;
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time</code> is in [{@link #begin},
   *         {@link #end}), <code>false</code> otherwise.
   */
  public boolean isIn(long time) {
    return isAfterStart(time) && isBeforeEnd(time);
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time >= {@link #begin}</code>,
   *         <code>false</code> otherwise.
   */
  public boolean isAfterStart(long time) {
    return time >= begin;
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time < {@link #end}</code>,
   *         <code>false</code> otherwise.
   */
  public boolean isBeforeEnd(long time) {
    return time < end;
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time < {@link #begin}</code>,
   *         <code>false</code> otherwise.
   */
  public boolean isBeforeStart(long time) {
    return time < begin;
  }

  /**
   * @param time The time to check.
   * @return <code>true</code> when <code>time >= {@link #end}</code>,
   *         <code>false</code> otherwise.
   */
  public boolean isAfterEnd(long time) {
    return time >= end;
  }

  /**
   * @return The length of the time window:
   *         <code>{@link #end} - {@link #begin}</code>.
   */
  public long length() {
    return end - begin;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("TimeWindow{");
    sb.append(begin);
    sb.append(",");
    sb.append(end);
    sb.append("}");
    return sb.toString();
  }
}
