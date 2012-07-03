package rinde.sim.util;

import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * Provides methods for converting time expressed as a <code>long</code> to a
 * nice string representatation: (D) HH:MM:SS.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class TimeFormatter {

    private static PeriodFormatter formatter = new PeriodFormatterBuilder()
            .appendDays().appendSeparator(" ").minimumPrintedDigits(2)
            .printZeroAlways().appendHours().appendLiteral(":").appendMinutes()
            .appendLiteral(":").appendSeconds().toFormatter();

    private TimeFormatter() {}

    /**
     * Converts the specified time in to a string.
     * @param ms The time to format in milliseconds.
     * @return A nice formatted time string.
     */
    public static String format(long ms) {
        return formatter.print(new Period(0, ms, PeriodType.dayTime()));
    }

    /**
     * Converts the specified time to a string.
     * @param ms The time to format in milliseconds.
     * @return A nice formatted time string.
     */
    public static String format(double ms) {
        return format(Math.round(ms));
    }

}
