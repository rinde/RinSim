package rinde.sim.util;

import rinde.sim.core.model.MovingRoadUser;
import rinde.sim.core.model.RoadModel;

/**
 * 
 * Ignores the model registration.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 */
public class TrivialRoadUser implements MovingRoadUser {

	@Override
	public void initRoadUser(RoadModel model) {
		// XXX use if the registration can be ignored in tests [bm]
	}

	@Override
	public double getSpeed() {
		return 1;
	}
}
