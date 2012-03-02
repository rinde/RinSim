package rinde.sim.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class SpeedConverterTest {
	final double DELTA  = 0.0001; 

	@Test
	public void testSeveralTransformations() {
		SpeedConverter sc = new SpeedConverter();
		//24 meters per day to meters per hour
		assertEquals(1, sc.from(24, TimeUnit.D).to(TimeUnit.H), DELTA);
		//60 m/h == 1 m/minute
		assertEquals(1, sc.from(60, TimeUnit.H).to(TimeUnit.M), DELTA);
		//30 m/h == 0.5 m/minute
		assertEquals(0.5, sc.from(30, TimeUnit.H).to(TimeUnit.M), DELTA);
		//30 km/h == 500 m/minute
		assertEquals(500, sc.from(30 * 1000, TimeUnit.H).to(TimeUnit.M), DELTA);
		//250 meters / minute == 60 * 250 meters per hour 
		assertEquals(60 * 250, sc.from(250, TimeUnit.M).to(TimeUnit.H), DELTA);
	}

}
