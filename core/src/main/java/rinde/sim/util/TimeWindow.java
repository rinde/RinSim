package rinde.sim.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;

/**
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
// half open interval: [begin, end)
public class TimeWindow implements Serializable {
    private static final long serialVersionUID = 7548761538022038612L;

    public static TimeWindow ALWAYS = new TimeWindow(0, Long.MAX_VALUE);

    public final long begin;
    public final long end;

    public TimeWindow(long pBegin, long pEnd) {
        checkArgument(pBegin >= 0, "Time must be positive.");
        checkArgument(pBegin <= pEnd, "Begin can not be later than end.");
        begin = pBegin;
        end = pEnd;
    }

    // is time in [begin,end) ?
    public boolean isIn(long time) {
        return isAfterStart(time) && isBeforeEnd(time);
    }

    public boolean isAfterStart(long time) {
        return time >= begin;
    }

    public boolean isBeforeEnd(long time) {
        return time < end;
    }

    public boolean isBeforeStart(long time) {
        return time < begin;
    }

    public boolean isAfterEnd(long time) {
        return time >= end;
    }

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
