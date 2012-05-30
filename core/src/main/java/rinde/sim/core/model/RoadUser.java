/**
 * 
 */
package rinde.sim.core.model;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public interface RoadUser {
	/**
	 * 
	 * This is called by {@link RoadModel#register(RoadUser)}
	 * 
	 * @param model
	 * @see RoadModel#unregister(RoadUser)
	 */
	void initRoadUser(RoadModel model);
}
