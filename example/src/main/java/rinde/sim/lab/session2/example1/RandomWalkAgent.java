package rinde.sim.lab.session2.example1;

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

/**
 * Simple agent that walks on random paths.
 * @author robrechthaesevoets
 * 
 */
public class RandomWalkAgent implements TickListener, MovingRoadUser, SimulatorUser {

	private final double speed;
	private final Point startingLocation;
	private RoadModel rm;
	private Queue<Point> path;
	private RandomGenerator rand;

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
		rand = api.getRandomGenerator();
	}

	@Override
	public double getSpeed() {
		return speed;
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		if (path == null || path.isEmpty()) {
			Point destination = rm.getRandomPosition(rand);
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
