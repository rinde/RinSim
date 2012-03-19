package rinde.sim.lab.session2.gradient_field_exercise.packages;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;

public class DeliveryLocation implements RoadUser{

	protected RoadModel rm;
	
	private Point position;
	
	public DeliveryLocation(Point position){
		this.position = position;
	}

	public Point getPosition(){
		return rm.getPosition(this);
	}
	
	@Override
	public void initRoadUser(RoadModel model) {
		this.rm = model;
		model.addObjectAt(this, position);
	}
	
}


