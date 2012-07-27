package rinde.sim.lab.session3.cnet;

import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.TimedEvent;

/**
 * 
 */
public class Example {

	public static void main(String[] args) throws Exception {
		final ScenarioBuilder builder = new ScenarioBuilder(ExampleEvent.ADD_TRUCK, ExampleEvent.ADD_PACKAGE);

		builder.addEventGenerator(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(0, // at
																							// time
																							// 0
				3, // amount of trucks to be added
				new ScenarioBuilder.EventTypeFunction(ExampleEvent.ADD_TRUCK)));

		builder.addEventGenerator(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(0, 10,
				new ScenarioBuilder.EventTypeFunction(ExampleEvent.ADD_PACKAGE)));

		final int timeStep = 100000000;

		builder.addEventGenerator(new ScenarioBuilder.TimeSeries<TimedEvent>(0, // start
																				// time
				20 * timeStep, // end time
				timeStep, // step
				new ScenarioBuilder.EventTypeFunction(ExampleEvent.ADD_PACKAGE)));

		final Scenario scenario = builder.build();

		final String MAP_DIR = "../core/files/maps/";

		new SimpleController(scenario, -1, MAP_DIR + "leuven-simple.dot");
	}
}
