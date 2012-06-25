package rinde.sim.lab.session1.example2;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.math.random.RandomGenerator;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.lab.common.Package;

public class RandomWalkAgent implements TickListener, MovingRoadUser, SimulatorUser {

	private final double speed;
	private final Point startingLocation;
	private RoadModel rm;
	private Queue<Point> path;
	private RandomGenerator rand;
	private SimulatorAPI simulator;

	private Package currentPackage;

	public RandomWalkAgent(double speed, Point startingLocation) {
		this.speed = speed;
		this.startingLocation = startingLocation;
	}

	@Override
	public void initRoadUser(RoadModel model) {
		rm = model;
		rm.addObjectAt(this, startingLocation);
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		simulator = api;
		rand = api.getRandomGenerator();
	}

	@Override
	public double getSpeed() {
		return speed;
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		if (path == null || path.isEmpty()) {
			if (currentPackage != null) {
				simulator.unregister(currentPackage);
			}
			Point destination = rm.getRandomPosition(rand);
			currentPackage = new Package("Package", destination);
			simulator.register(currentPackage);
			path = new LinkedList<Point>(rm.getShortestPathTo(this, destination));
		} else {
			rm.followPath(this, path, timeLapse);
		}
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// not used
	}

}
