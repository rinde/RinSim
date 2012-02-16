package rinde.sim.core;

import rinde.sim.core.model.RoadUser;

/**
 * Limited simulator API that provides an API for simulation elements (e.g., agents)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
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
}
