package rinde.sim.lab.session1.example3;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.math.random.RandomGenerator;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.MovingRoadUser;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Message;
import rinde.sim.lab.common.SimpleMessage;

public class RandomWalkAgent implements TickListener, MovingRoadUser, SimulatorUser, CommunicationUser {

	private double speed;
	private Point startingLocation;
	private RoadModel rm;
	private Queue<Point> path;
	private RandomGenerator rand;
	//reference to the communication API
	private CommunicationAPI cm;
	private String name;
	
	public RandomWalkAgent(String name, double speed, Point startingLocation){
		this.speed = speed;
		this.startingLocation = startingLocation;
		this.name = name;
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
			cm.broadcast(new SimpleMessage(this, name +" is starting new path."));	
		}else{
			rm.followPath(this, path, timeStep);
		}
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		//not used
	}

	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		this.cm = api;
	}

	@Override
	public Point getPosition() {
		return this.rm.getPosition(this);
	}


	@Override
	public void receive(Message message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getRadius() {
		//distances are ignored in the communication model
		return 0;
	}

	@Override
	public double getReliability() {
		//we do not consider reliability in this example
		return 1;
	}

}
