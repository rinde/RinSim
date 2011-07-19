/**
 * 
 */
package rinde.sim.scenario;

import rinde.sim.event.Event;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ScenarioEvent extends Event {

	public enum Type {
		SCENARIO_EVENT
	}

	public final SerializedScenarioEvent data;

	public ScenarioEvent(Object issuer, SerializedScenarioEvent data) {
		super(Type.SCENARIO_EVENT, issuer);
		this.data = data;
	}

}
