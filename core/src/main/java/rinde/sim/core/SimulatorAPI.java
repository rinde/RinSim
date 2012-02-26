package rinde.sim.core;

import org.apache.commons.math.random.RandomGenerator;


/**
 * Limited simulator API that provides an API for simulation elements (e.g., agents)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 *
 */
public interface SimulatorAPI {
	
	/**
	 * Register a given entity in the simulator. 
	 * During registration the object is provided all features it requires (declared by interfaces)
	 * and bound to the required models (if they were registered in the simulator before)  
	 * @param o object to register
	 * @return <code>true</code> when registration of the object in the simulator was successful
	 * @throws IllegalStateException when simulator is not configured (by calling {@link Simulator#configure()}
	 */
	public boolean register(Object o);
	
	/**
	 * TODO add comment
	 * @param o
	 * @return
	 */
	public boolean unregister(Object o);
	
	/**
	 * Get access to the main random generator used in the simulator
	 * @return the random generator of the simulatsor
	 */
	public RandomGenerator getRandomGenerator();
}

