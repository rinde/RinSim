package rinde.sim.core;


/**
 * Limited simulator API that provides an API for simulation elements (e.g.,
 * agents).
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @since 2.0
 * 
 */
public interface SimulatorAPI {

    /**
     * Register a given entity in the simulator. During registration the object
     * is provided all features it requires (declared by interfaces) and bound
     * to the required models (if they were registered in the simulator before).
     * @param o object to register
     * @return <code>true</code> when registration of the object in the
     *         simulator was successful
     * @throws IllegalStateException when simulator is not configured (by
     *             calling {@link Simulator#configure()}
     */
    boolean register(Object o);

    /**
     * Unregister an object from simulator.
     * @param o The object to be unregistered.
     * @return True if the object could be unregistered, false otherwise.
     */
    boolean unregister(Object o);

}
