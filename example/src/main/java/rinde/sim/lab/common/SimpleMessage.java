package rinde.sim.lab.common;

import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Message;

public class SimpleMessage extends Message{

	private String content;
	
	public SimpleMessage(CommunicationUser sender, String content) {
		super(sender);
		this.content = content;
	}
	
	public String getContent(){
		return this.content;
	}

}
