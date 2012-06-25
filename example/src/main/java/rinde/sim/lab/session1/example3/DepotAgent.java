package rinde.sim.lab.session1.example3;

import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;
import rinde.sim.lab.common.SimpleMessage;

public class DepotAgent implements TickListener, RoadUser, CommunicationUser {
	protected static final Logger LOGGER = LoggerFactory.getLogger(Simulator.class);

	private final Point startingLocation;

	private RoadModel rm;
	// reference to the communication API
	private CommunicationAPI cm;

	// mailbox to store incoming messages
	private final Mailbox mailbox;

	public DepotAgent(Point startingLocation) {
		LOGGER.info("DepotAgent created.");
		this.startingLocation = startingLocation;
		mailbox = new Mailbox();
	}

	@Override
	public void initRoadUser(RoadModel model) {
		rm = model;
		rm.addObjectAt(this, startingLocation);
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		Queue<Message> messages = mailbox.getMessages();

		for (Message message : messages) {
			if (message instanceof SimpleMessage) {
				SimpleMessage m = (SimpleMessage) message;
				LOGGER.info("Depot: " + m.getContent());
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
