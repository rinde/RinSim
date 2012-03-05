package rinde.sim.lab.session1.example1;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.math.random.RandomGenerator;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.MovingRoadUser;
import rinde.sim.core.model.RoadModel;

public class RandomWalkAgent implements TickListener, MovingRoadUser, SimulatorUser {

	private double speed;
	private Point startingLocation;
	private RoadModel rm;
	private Queue<Point> path;
	private RandomGenerator rand;
	
	
	public RandomWalkAgent(double speed, Point startingLocation){
		this.speed = speed;
		this.startingLocation = startingLocation;
	}
	
	@Override
	public void initRoadUser(RoadModel model) {
		this.rm = model;
		this.rm.addObjectAt(this, startingLocation);
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		this.rand = api.getRandomGenerator();
	}

	@Override
	public double getSpeed() {
		return this.speed;
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		if(path == null || path.isEmpty()){
			Point destination = rm.getGraph().getRandomNode(rand);
			this.path = new LinkedList<Point>(rm.getShortestPathTo(this, destination));
		}else{
			rm.followPath(this, path, timeStep);
		}
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		//not used
	}

}
