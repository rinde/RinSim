/**
 * 
 */
package rinde.sim.core;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 */
public interface TickListener {

	/**
	 * Send the tick to the tick listener
	 * @param currentTime The current time.
	 * @param timeStep The time step.
	 */
	public void tick(final long currentTime, final long timeStep);

	/**
	 * Allow the tick listener to perform action after all tick listeners were
	 * informed about tick {@link TickListener#tick(long, long)}.
	 * @param currentTime The current time.
	 * @param timeStep The time step.
	 */
	public void afterTick(final long currentTime, final long timeStep);
}
