package rinde.sim.core;

import rinde.sim.core.model.RoadUser;

/**
 * An interface that declares that a given simulation entity (e.g. agent) 
 * requires the ability to get access to Simulator API
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 *
 */
public interface SimulatorUser {
	void setSimulator(SimulatorAPI api);
}
