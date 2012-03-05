package rinde.sim.lab.session1.example2;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.math.random.RandomGenerator;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.MovingRoadUser;
import rinde.sim.core.model.RoadModel;
import rinde.sim.lab.common.Package;

public class RandomWalkAgent implements TickListener, MovingRoadUser, SimulatorUser {

	private double speed;
	private Point startingLocation;
	private RoadModel rm;
	private Queue<Point> path;
	private RandomGenerator rand;
	private SimulatorAPI simulator;
	
	private Package currentPackage;
	
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
		this.simulator = api;
		this.rand = api.getRandomGenerator();
	}

	@Override
	public double getSpeed() {
		return this.speed;
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		if(path == null || path.isEmpty()){
			if(currentPackage != null)
				simulator.unregister(currentPackage);
			Point destination = rm.getGraph().getRandomNode(rand);
			currentPackage = new Package("Package", destination);
			simulator.register(currentPackage);
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
