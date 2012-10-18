package rinde.sim.util;

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
        begin = pBegin;
        end = pEnd;
    }

    // is time in [begin,end) ?
    public boolean isIn(long time) {
        return time >= begin && time < end;
    }

    public boolean isAfterStart(long time) {
        return time >= begin;
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
