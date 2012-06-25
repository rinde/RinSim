package rinde.sim.lab.session1.catch_the_flag;

import java.util.Queue;

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
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;
import rinde.sim.lab.common.ConfirmationMessage;
import rinde.sim.lab.common.Flag;
import rinde.sim.lab.common.TransportRequest;

public class DepotAgent implements TickListener, RoadUser, SimulatorUser, CommunicationUser {
	protected static final Logger LOGGER = LoggerFactory.getLogger(Simulator.class);

	private final Point startingLocation;

	private RoadModel rm;
	private CommunicationAPI cm;
	private SimulatorAPI simulator;

	private final Mailbox mailbox;
	private Flag flag;
	private long flagID = 0;

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
		// if there is no flag create a flag
		if (flag == null) {
			placeFlag();
		}

		// get all received messages (this empties the mailbox)
		Queue<Message> messages = mailbox.getMessages();
		for (Message message : messages) {
			if (message instanceof ConfirmationMessage) {
				ConfirmationMessage m = (ConfirmationMessage) message;
				// check if confirmation is for current flag
				if (m.getFlagID() == flagID) {
					simulator.unregister(flag);
					placeFlag();
					LOGGER.info("Depot: " + m.getTruck() + " got the flag first");
				}
			}
		}
	}

	private void placeFlag() {
		Point flagLocation = rm.getRandomPosition(simulator.getRandomGenerator());
		flag = new Flag(flagLocation);
		simulator.register(flag);
		flagID++;
		cm.broadcast(new TransportRequest(this, flagLocation, flagID));
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

	@Override
	public void setSimulator(SimulatorAPI api) {
		simulator = api;
	}

}
