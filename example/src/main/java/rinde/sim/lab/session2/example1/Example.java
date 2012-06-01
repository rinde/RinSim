package rinde.sim.lab.session2.example1;

import rinde.sim.event.pdp.StandardType;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.TimedEvent;

public class Example {

	public static void main(String[] args) throws Exception{

		int interval = 50000000;
	
		/*
		 * Building a scenario manually 
		 */
		
		Scenario scenario = new Scenario();
		
		//Add 10 trucks at time 0
		for(int i = 0; i < 10; i++){
			scenario.add(new TimedEvent(StandardType.ADD_TRUCK, 0));
		}	
		
		//Add 20 additional trucks spread over 5 timesteps
		for(int i = 0; i < 20; i++){
			scenario.add(new TimedEvent(StandardType.ADD_TRUCK, i*interval));
		}

		/*
		 * Building a scenario using the scenario builder
		 */
		
		ScenarioBuilder builder = new ScenarioBuilder(StandardType.ADD_TRUCK, StandardType.ADD_PACKAGE);
		
		builder.addEventGenerator(
				new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
						0, //at time 0
						10, //amount of trucks to be added
						new ScenarioBuilder.EventTypeFunction(
								StandardType.ADD_TRUCK
						)
				)
		);
		
		
		builder.addEventGenerator(
				new ScenarioBuilder.TimeSeries<TimedEvent>(
						0, // start time
						20*interval, // end time
						interval, // step
						new ScenarioBuilder.EventTypeFunction(
								StandardType.ADD_TRUCK
						)
				)
		);
	
		//build the scenario
		Scenario scenario2 = builder.build();
	
		/*
		 * Running a scenario
		 */
		
		// run scenario with visualization attached
		final String MAP_DIR = "../core/files/maps/";

	
		//a negative value for number of ticks, means infinite number of ticks
		new SimpleController(scenario, -1, MAP_DIR + "leuven-simple.dot");		
//		new SimpleController(scenario2, -1, MAP_DIR + "leuven-simple.dot");		
		
	}
}
