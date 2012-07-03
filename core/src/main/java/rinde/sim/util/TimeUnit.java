package rinde.sim.util;

public enum TimeUnit {
    // TODO use joda-time?
    MS(1), S(MS.toMs(1000)), M(S.toMs(60)), H(M.toMs(60)), D(H.toMs(24));

    private final long perOneUnit;

    private TimeUnit(long perUnit) {
        perOneUnit = perUnit;
    }

    public long toMs(long no) {
        return no * perOneUnit;
    }

    public long toMs() {
        return perOneUnit;
    }
}
