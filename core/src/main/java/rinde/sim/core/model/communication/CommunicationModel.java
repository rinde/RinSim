package rinde.sim.core.model.communication;

import rinde.sim.core.TickListener;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.RoadUser;

/**
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class CommunicationModel implements Model<CommunicationUser>, TickListener, CommunicationAPI {

	/** 
	 * Register communication user {@link CommunicationUser}. Communication user is registered only 
	 * when it is also {@link RoadUser}. This is required as communication model depends on elements positions.
	 */
	@Override
	public boolean register(CommunicationUser element) {
		if(! (element instanceof RoadUser)) return false;
		return false;
	}

	@Override
	public Class<CommunicationUser> getSupportedType() {
		return CommunicationUser.class;
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		
	}

	@Override
	public boolean unregister(CommunicationUser element) {
		return false;
	}

	@Override
	public void send(CommunicationUser recipient, Message message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void broadcast(Message message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void broadcast(Message message,
			Class<? extends CommunicationUser> type) {
		// TODO Auto-generated method stub
		
	}



}
