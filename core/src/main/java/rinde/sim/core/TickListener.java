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
	 * 
	 * The timeLapse object can only be used during the tick, saving the object
	 * for future reference is useless since it will be consumed after the tick
	 * call. TODO improve this documentation
	 * 
	 * @param currentTime The current time.
	 * @param timeStep The time step.
	 */
	public void tick(final TimeLapse timeLapse);

	/**
	 * Allow the tick listener to perform action after all tick listeners were
	 * informed about tick {@link TickListener#tick(long, long)}.
	 * @param currentTime The current time.
	 * @param timeStep The time step.
	 */
	public void afterTick(final TimeLapse timeLapse);
}
