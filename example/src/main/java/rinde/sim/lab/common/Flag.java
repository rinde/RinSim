package rinde.sim.lab.common;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;

public class Flag implements RoadUser{

	private Point startLocation;
	
	public Flag(Point startLocation){
		this.startLocation = startLocation;
	}
	
	@Override
	public void initRoadUser(RoadModel model) {
		model.addObjectAt(this, startLocation);
	}
	
}
