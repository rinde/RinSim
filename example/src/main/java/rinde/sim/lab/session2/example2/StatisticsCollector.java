package rinde.sim.lab.session2.example2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.event.Event;
import rinde.sim.event.Listener;

public class StatisticsCollector implements Listener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsCollector.class);
	
	@Override
	public void handleEvent(Event e) {
		if(e.getEventType() == RandomWalkAgent.Type.START_AGENT){
			LOGGER.info(e.getIssuer().toString() + " started.");
		}
	}

}
