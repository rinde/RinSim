package rinde.sim.lab.session2.example1;

import static com.google.common.collect.Lists.newArrayList;
import static rinde.sim.event.pdp.StandardType.ADD_PACKAGE;
import static rinde.sim.event.pdp.StandardType.ADD_TRUCK;

import java.util.List;

import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.TimedEvent;

public class Example {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {

		int interval = 50000000;

		/*
		 * Building a scenario manually
		 */
		List<TimedEvent> events = newArrayList();
		// Add 10 trucks at time 0
		for (int i = 0; i < 10; i++) {
			events.add(new TimedEvent(ADD_TRUCK, 0));
		}
		// Add 20 additional trucks spread over 5 timesteps
		for (int i = 0; i < 20; i++) {
			events.add(new TimedEvent(ADD_TRUCK, i * interval));
		}
		Scenario scenario = new Scenario(events);

		/*
		 * Building a scenario using the scenario builder
		 */
		ScenarioBuilder builder = new ScenarioBuilder(ADD_TRUCK, ADD_PACKAGE);

		// add at time 0, 10 events of type ADD_TRUCK
		builder.addMultipleEvents(0, 10, ADD_TRUCK);
		// add from time 0 to 20 an event of type ADD_TRUCK at each interval
		builder.addTimeSeriesOfEvents(0, 20 * interval, interval, ADD_TRUCK);

		// build the scenario
		Scenario scenario2 = builder.build();

		/*
		 * Running a scenario
		 */

		// run scenario with visualization attached
		final String MAP_DIR = "../core/files/maps/";

		// a negative value for number of ticks, means infinite number of ticks
		new SimpleController(scenario, -1, MAP_DIR + "leuven-simple.dot");
		// new SimpleController(scenario2, -1, MAP_DIR + "leuven-simple.dot");

	}
}
