package rinde.sim.lab.common;

import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Message;

public class ConfirmationMessage extends Message{

	private String truck;
	private long flagID;
	
	public ConfirmationMessage(CommunicationUser sender, String truck, long flagID) {
		super(sender);
		this.truck = truck;
		this.flagID = flagID;
	}
	
	public String getTruck(){
		return this.truck;
	}
	
	public long getFlagID(){
		return flagID;
	}
	
}
