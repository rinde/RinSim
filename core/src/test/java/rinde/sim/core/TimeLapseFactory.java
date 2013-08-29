package rinde.sim.core;

import javax.measure.quantity.Duration;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public final class TimeLapseFactory {

  private TimeLapseFactory() {}

  // this should only be used in tests!

  public static TimeLapse create(Unit<Duration> unit, long start, long end) {
    final TimeLapse tl = new TimeLapse(unit);
    tl.initialize(start, end);
    return tl;
  }

  public static TimeLapse create(long start, long end) {
    return create(SI.MILLI(SI.SECOND), start, end);
  }

  public static TimeLapse time(long start, long end) {
    return create(start, end);
  }

  public static TimeLapse time(long end) {
    return create(0, end);
  }

  public static TimeLapse hour(long start, long end) {
    return create(NonSI.HOUR, start, end);
  }

  public static TimeLapse hour(long end) {
    return create(NonSI.HOUR, 0, end);
  }

  public static TimeLapse ms(long start, long end) {
    return create(SI.MILLI(SI.SECOND), start, end);
  }

  public static TimeLapse ms(long end) {
    return ms(0, end);
  }

}
