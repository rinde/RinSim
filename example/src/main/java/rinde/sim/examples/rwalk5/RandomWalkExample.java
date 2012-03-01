/**
 * 
 */
package rinde.sim.examples.rwalk5;

import rinde.sim.event.pdp.StandardType;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class RandomWalkExample {

	public static void main(String[] args) throws Exception {
		// create a new simulator, load map of Leuven
		
		final int simStep = 100; 
		
		// create simple scenario
		
		ScenarioBuilder builder = new ScenarioBuilder(StandardType.ADD_TRUCK);
		
		builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
				0, 10, new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));
		builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
				20000 * simStep, 10, new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));
		builder.add(new ScenarioBuilder.TimeSeries<TimedEvent>(
				990000, 20000000 , 100000, new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));
		
		
		ScenarioBuilder builder2 = new ScenarioBuilder(StandardType.ADD_TRUCK);
		builder2.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(
				0, 30, new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));
		
		Scenario s = builder.build();
		
		// run scenario with visualization attached
		
		new SimpleController(s, "files/leuven.dot");
	}
}
