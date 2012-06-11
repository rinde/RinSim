package rinde.sim.lab.session1.catch_the_flag_solution;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.math.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.Simulator;
import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.lab.common.ConfirmationMessage;
import rinde.sim.lab.common.TransportRequest;

public class TruckAgent implements TickListener, MovingRoadUser, SimulatorUser, CommunicationUser {
	protected static final Logger LOGGER = LoggerFactory.getLogger(Simulator.class); 

	private double speed;
	private Point startingLocation;
	private RoadModel rm;
	private Queue<Point> path;
	private RandomGenerator rand;
	//reference to the communication API
	private CommunicationAPI cm;
	private Mailbox mailbox;
	private String name;
	private long currentFlag;
	private CommunicationUser depot;
	
	public TruckAgent(String name, double speed, Point startingLocation){
		this.speed = speed;
		this.startingLocation = startingLocation;
		this.name = name;
		this.mailbox = new Mailbox();
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
		Queue<Message> messages = mailbox.getMessages();
		for(Message message: messages){
			if(message instanceof TransportRequest){
				TransportRequest request = (TransportRequest) message;
				Point flagLocation = request.getLocation();
				depot = request.getSender();
				currentFlag = request.getFlagID();
				this.path = new LinkedList<Point>(rm.getShortestPathTo(this, flagLocation));
			}
		}
		
		if(path != null){
			if(path.isEmpty()){
				cm.send(depot,new ConfirmationMessage(this, name, currentFlag));
			}else{
				rm.followPath(this, path, timeStep);
			}
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
	public double getRadius() {
		//distances are ignored in the communication model
		return 0;
	}

	@Override
	public double getReliability() {
		//we do not consider reliability in this example
		return 1;
	}

	@Override
	public void receive(Message message) {
		this.mailbox.receive(message);
	}

}
