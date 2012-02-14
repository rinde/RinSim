package rinde.sim.examples.rwalk;

import rinde.sim.core.RoadUser;

class Package implements RoadUser {
	public final String name;

	public Package(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}