package rinde.sim.examples.rwalk2;

import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;

class Package implements RoadUser {
	public final String name;

	public Package(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public void initRoadUser(RoadModel model) {
		
	}
}