package rinde.sim.lab.common;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Message;

public class TransportRequest extends Message{

	private long flagID;
	private Point location;
	
	public TransportRequest(CommunicationUser sender, Point location, long flagID) {
		super(sender);
		this.location = location;
		this.flagID = flagID;
	}

	public long getFlagID(){
		return flagID;
	}
	
	public Point getLocation(){
		return location;
	}

}
