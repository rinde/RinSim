package rinde.sim.lab.session2.example2;

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
import rinde.sim.event.Event;
import rinde.sim.event.EventAPI;
import rinde.sim.event.EventDispatcher;

/**
 * Simple agent that walks on random paths
 * @author robrechthaesevoets
 * 
 */
public class RandomWalkAgent implements TickListener, MovingRoadUser, SimulatorUser {

	// Types of events this agent can dispatch
	enum Type {
		START_AGENT;
	}

	// We use an event dispatcher to actually dispatch our events
	private final EventDispatcher disp;
	public final EventAPI eventAPI;

	private final double speed;
	private final Point startingLocation;
	private RoadModel rm;
	private Queue<Point> path;
	private RandomGenerator rand;

	public RandomWalkAgent(double speed, Point startingLocation) {
		this.speed = speed;
		this.startingLocation = startingLocation;
		// create a new dispatcher for the possible types of events
		disp = new EventDispatcher(Type.values());
		eventAPI = disp.getEventAPI();
	}

	@Override
	public void initRoadUser(RoadModel model) {
		rm = model;
		rm.addObjectAt(this, startingLocation);
		disp.dispatchEvent(new Event(Type.START_AGENT, this));
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
			// we selected a new path, dispatch a corresponding event
		} else {
			rm.followPath(this, path, timeLapse);
		}
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// not used
	}

}
