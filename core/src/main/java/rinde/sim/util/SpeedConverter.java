package rinde.sim.util;

/**
 * Simple speed converter
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class SpeedConverter {
	double value = 0;
	
	public SpeedConverter from(double meters, TimeUnit per) {
		value = meters / per.toMs();
		return this;
	}
	
	public double to(TimeUnit per) {
		double result = value;
		value = 0;
		return result * per.toMs();
	}
}
