package rinde.sim.examples.rwalk5;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.event.Event;
import rinde.sim.event.Listener;

public class StatisticsCollector implements Listener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsCollector.class);
	
	private int totalMSGs = 0;
	private int totalPkgs = 0;
	private int totalTrucks = 0;
	
	@Override
	public void handleEvent(Event e) {
		if(e.getEventType() == RandomWalkAgent.Type.FINISHED_SERVICE) {
			ServiceEndEvent event = (ServiceEndEvent) e;
			totalMSGs += event.communicates;
			totalPkgs += event.pickedUp;
			totalTrucks++;
//			Formatter formatter = new Formatter();
//			formatter.format("trucks: %", args)
//			StringWriter w = new StringWriter();
//			PrintWriter pw = new PrintWriter(w);
//			pw.printf(format, args)
			
			LOGGER.info("total trucks: {} | avgMsgs: {}", new Object[] {totalTrucks, ((double)totalMSGs / totalTrucks)}); 
		}
	}

}
