/**
 * 
 */
package rinde.sim.examples.fabrirecht;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class AddDepotEvent extends TimedEvent {

	public Point position;

	public AddDepotEvent(long time, Point pPosition) {
		super(PDPScenarioEvent.ADD_DEPOT, time);
		position = pPosition;
	}

}
