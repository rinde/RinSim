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
import rinde.sim.core.TimeLapse;
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

	private final double speed;
	private final Point startingLocation;
	private RoadModel rm;
	private Queue<Point> path;
	private RandomGenerator rand;
	// reference to the communication API
	private CommunicationAPI cm;
	private final Mailbox mailbox;
	private final String name;
	private long currentFlag;
	private CommunicationUser depot;

	public TruckAgent(String name, double speed, Point startingLocation) {
		this.speed = speed;
		this.startingLocation = startingLocation;
		this.name = name;
		mailbox = new Mailbox();
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
		Queue<Message> messages = mailbox.getMessages();
		for (Message message : messages) {
			if (message instanceof TransportRequest) {
				TransportRequest request = (TransportRequest) message;
				Point flagLocation = request.getLocation();
				depot = request.getSender();
				currentFlag = request.getFlagID();
				path = new LinkedList<Point>(rm.getShortestPathTo(this, flagLocation));
			}
		}

		if (path != null) {
			if (path.isEmpty()) {
				cm.send(depot, new ConfirmationMessage(this, name, currentFlag));
			} else {
				rm.followPath(this, path, timeLapse);
			}
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
	public double getRadius() {
		// distances are ignored in the communication model
		return 0;
	}

	@Override
	public double getReliability() {
		// we do not consider reliability in this example
		return 1;
	}

	@Override
	public void receive(Message message) {
		mailbox.receive(message);
	}

}
