package rinde.sim.core.model.communication;

import rinde.sim.core.TickListener;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.RoadUser;

public class CommunicationModel implements Model<CommunicationUser>, TickListener {

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
		// TODO Auto-generated method stub
		
	}

}
