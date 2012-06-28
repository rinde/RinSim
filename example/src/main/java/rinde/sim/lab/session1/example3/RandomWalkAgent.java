package rinde.sim.lab.session1.example3;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Message;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.lab.common.SimpleMessage;

public class RandomWalkAgent implements TickListener, MovingRoadUser, SimulatorUser, CommunicationUser {

	private final double speed;
	private final Point startingLocation;
	private RoadModel rm;
	private Queue<Point> path;
	private RandomGenerator rand;
	// reference to the communication API
	private CommunicationAPI cm;
	private final String name;

	public RandomWalkAgent(String name, double speed, Point startingLocation) {
		this.speed = speed;
		this.startingLocation = startingLocation;
		this.name = name;
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
			cm.broadcast(new SimpleMessage(this, name + " is starting new path."));
		} else {
			rm.followPath(this, path, timeLapse);
		}
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// not used
	}

	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		cm = api;
	}

	@Override
	public Point getPosition() {
		return rm.getPosition(this);
	}

	@Override
	public void receive(Message message) {
		// TODO Auto-generated method stub

	}

	@Override
	public double getRadius() {
		// distances are ignored in the communication model
		return 0;
	}

	@Override
	public double getReliability() {
		// we do not consider reliability in this example
		return 1;
	}

}
