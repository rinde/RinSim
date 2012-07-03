/**
 * 
 */
package rinde.sim.util;

import static org.junit.Assert.assertEquals;
import static rinde.sim.util.TimeFormatter.format;

import org.junit.Test;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
@SuppressWarnings("javadoc")
public class TimeFormatterTest {

    static final long s = 1000;
    static final long m = 60 * s;
    static final long h = 60 * m;
    static final long d = 24 * h;

    @Test
    public void testFormatLong() {
        assertEquals("00:00:00", format(123));
        assertEquals("00:00:02", format(2123));
        assertEquals("00:00:22", format(22123));
        assertEquals("00:06:00", format(6 * m));
        assertEquals("00:11:02", format(11 * m + 2123));
        assertEquals("02:11:02", format(2 * h + 11 * m + 2123));
        assertEquals("20:11:02", format(20 * h + 11 * m + 2623));
        assertEquals("1 04:11:02", format(28 * h + 11 * m + 2623));
        assertEquals("11 11:11:02", format((11 * 24 + 11) * h + 11 * m + 2623));
    }

    @Test
    public void testFormatDouble() {
        assertEquals("00:00:00", format(123.0));
        assertEquals("00:00:02", format(2123.0));
        assertEquals("00:00:22", format(22123.0));
        assertEquals("00:00:22", format(22123.75));
        assertEquals("00:00:00", format(999));
        assertEquals("00:00:01", format(999.5));
    }
}
