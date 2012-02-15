package rinde.sim.util;

import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;

/**
 * 
 * Ignores the model registration.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 *
 */
public class TrivialRoadUser implements RoadUser {

	@Override
	public void initRoadUser(RoadModel model) {
		// XXX use if the registration can be ignored in tests [bm]
	}

}
