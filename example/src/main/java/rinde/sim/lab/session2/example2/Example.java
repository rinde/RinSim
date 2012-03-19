package rinde.sim.lab.session2.example2;

import rinde.sim.event.pdp.StandardType;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.TimedEvent;

public class Example {

	public static void main(String[] args) throws Exception{
		ScenarioBuilder builder = new ScenarioBuilder(StandardType.ADD_TRUCK, StandardType.ADD_PACKAGE);
		
		builder.add(
				new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
						0, //at time 0
						10, //amount of trucks to be added
						new ScenarioBuilder.EventTypeFunction(
								StandardType.ADD_TRUCK
						)
				)
		);
		
		int timeStep = 50000000;
		
		builder.add(
				new ScenarioBuilder.TimeSeries<TimedEvent>(
						0, // start time
						20*timeStep, // end time
						timeStep, // step
						new ScenarioBuilder.EventTypeFunction(
								StandardType.ADD_TRUCK
						)
				)
		);
		
		
		Scenario scenario = builder.build();
		
		// run scenario with visualization attached
		final String MAP_DIR = "../core/files/maps/";

		new SimpleController(scenario, -1, MAP_DIR + "leuven-simple.dot");		
		
	}
}
