/**
 * 
 */
package rinde.sim.core.model.time;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
// FIXME think of name, perhaps TimeControls is better? This should be the thing
// that allows modifying time progress
public interface Time {

    /**
     * @return The current simulation time.
     */
    long getCurrentTime();

    /**
     * @return The time step (in simulation time) which is added to current time
     *         at every tick.
     */
    long getTimeStep();

    /**
     * Start the simulation.
     */
    void start();

    /**
     * Advances the simulator with one step (the size is determined by the time
     * step).
     */
    void tick();

    /**
     * Either starts or stops the simulation depending on the current state.
     */
    void togglePlayPause();

    /**
     * Stops the simulation.
     */
    void stop();

    /**
     * @return true if simulator is playing, false otherwise.
     */
    boolean isPlaying();

}
