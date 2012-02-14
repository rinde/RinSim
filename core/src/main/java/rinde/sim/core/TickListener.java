/**
 * 
 */
package rinde.sim.core;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public interface TickListener {

	public void tick(final long currentTime, final long timeStep);
}
