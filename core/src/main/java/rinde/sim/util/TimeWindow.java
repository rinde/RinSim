package rinde.sim.util;

// half open interval: [begin, end)
public class TimeWindow {

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
