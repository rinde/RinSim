package rinde.sim.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeFormatter {

	private static SimpleDateFormat dateFormat = init();

	private static SimpleDateFormat init() {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf;
	}

	private static final long DAY = 24 * 60 * 60 * 1000;

	public static String format(long ms) {
		String day = "";
		if (ms > DAY) {
			day = "" + Math.round(ms / DAY);
		}
		return day + " " + dateFormat.format(new Date(ms));
	}
}