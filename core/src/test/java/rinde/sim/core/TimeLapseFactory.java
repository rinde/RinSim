/**
 * 
 */
package rinde.sim.core;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class TimeLapseFactory {

	// this should only be used in tests!

	public static TimeLapse create(long start, long end) {
		return new TimeLapse(start, end);
	}

}
