package rinde.sim.lab.session1.catch_the_flag_solution;

import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.Simulator;
import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.lab.common.ConfirmationMessage;
import rinde.sim.lab.common.Flag;
import rinde.sim.lab.common.TransportRequest;

public class DepotAgent implements TickListener, RoadUser, SimulatorUser, CommunicationUser {
	protected static final Logger LOGGER = LoggerFactory.getLogger(Simulator.class); 
	
	private Point startingLocation;

	private RoadModel rm;
	private CommunicationAPI cm;
	private SimulatorAPI simulator;
	
	private Mailbox mailbox;
	private Flag flag;
	private long flagID = 0;
	
	public DepotAgent(Point startingLocation){
		LOGGER.info("DepotAgent created.");
		this.startingLocation = startingLocation;
		this.mailbox = new Mailbox();
	}
	
	@Override
	public void initRoadUser(RoadModel model) {
		this.rm = model;
		this.rm.addObjectAt(this, startingLocation);
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		//if there is no flag create a flag
		if(flag == null){
			placeFlag();
		}
	
		//get all received messages (this empties the mailbox)
		Queue<Message> messages = mailbox.getMessages();
		for(Message message: messages){
			if(message instanceof ConfirmationMessage){
				ConfirmationMessage m = (ConfirmationMessage) message;
				//check if confirmation is for current flag
				if(m.getFlagID() == this.flagID){
					simulator.unregister(flag);
					placeFlag();
					LOGGER.info("Depot: "+ m.getTruck() + " got the flag first");
				}
			}
		}
	}
	
	private void placeFlag(){
		Point flagLocation = rm.getGraph().getRandomNode(simulator.getRandomGenerator());
		flag = new Flag(flagLocation);
		simulator.register(flag);
		flagID ++;
		cm.broadcast(new TransportRequest(this, flagLocation, flagID));
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

	@Override
	public void setSimulator(SimulatorAPI api) {
		this.simulator = api;
	}

}
