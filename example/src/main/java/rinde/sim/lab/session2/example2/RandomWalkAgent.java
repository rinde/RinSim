package rinde.sim.lab.session2.example2;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.math.random.RandomGenerator;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.MovingRoadUser;
import rinde.sim.core.model.RoadModel;
import rinde.sim.event.Event;
import rinde.sim.event.EventDispatcher;
import rinde.sim.event.Events;
import rinde.sim.event.Listener;

/**
 * Simple agent that walks on random paths
 * @author robrechthaesevoets
 *
 */
public class RandomWalkAgent implements TickListener, MovingRoadUser, SimulatorUser, Events {

	//Types of events this agent can dispatch
	enum Type {
		START_AGENT;
	}

	//We use an event dispatcher to actually dispatch our events
	private final EventDispatcher disp;
	
	private double speed;
	private Point startingLocation;
	private RoadModel rm;
	private Queue<Point> path;
	private RandomGenerator rand;
	
	
	public RandomWalkAgent(double speed, Point startingLocation){
		this.speed = speed;
		this.startingLocation = startingLocation;
		//create a new dispatcher for the possible types of events
		disp = new EventDispatcher(Type.values());
	}
	
	@Override
	public void initRoadUser(RoadModel model) {
		this.rm = model;
		this.rm.addObjectAt(this, startingLocation);
		disp.dispatchEvent(new Event(Type.START_AGENT, this));
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
			//we selected a new path, dispatch a corresponding event
		}else{
			rm.followPath(this, path, timeStep);
		}
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		//not used
	}

	@Override
	public void addListener(Listener l, Enum<?>... eventTypes) {
		//delegate to the event dispatcher
		disp.addListener(l, eventTypes);
	}

	@Override
	public void removeListener(Listener l, Enum<?>... eventTypes) {
		//delegate to the event dispatcher
		disp.removeListener(l, eventTypes);
	}

	@Override
	public boolean containsListener(Listener l, Enum<?> eventType) {
		//delegate to the event dispatcher
		return disp.containsListener(l, eventType);
	}

}
