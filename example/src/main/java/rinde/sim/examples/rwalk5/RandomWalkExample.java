/**
 * 
 */
package rinde.sim.examples.rwalk5;

import rinde.sim.event.pdp.StandardType;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeConverter;

/**
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class RandomWalkExample {

	public static void main(String[] args) throws Exception {
		// create a new simulator, load map of Leuven

		final int simStep = 100;

		// create simple scenario

		TimeConverter conv = new TimeConverter(simStep);

		ScenarioBuilder builder = new ScenarioBuilder(StandardType.ADD_TRUCK);

		builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(0, 10,
				new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));
		builder.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(conv.hour(3).min(30).toTime(), 10,
				new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));
		builder.add(new ScenarioBuilder.TimeSeries<TimedEvent>(conv.day(1).toTime(), conv.day(4).toTime(), conv
				.hour(12).min(17).toTime(), new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));

		builder.add(new ScenarioBuilder.TimeSeries<TimedEvent>(conv.day(3).toTime(), conv.day(15).toTime(), conv
				.hour(1).min(1).toTime(), new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));

		builder.add(new ScenarioBuilder.TimeSeries<TimedEvent>(conv.tick(2000).toTime(), conv.tick(20000).toTime(),
				conv.tick(1000).toTime(), new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));

		ScenarioBuilder builder2 = new ScenarioBuilder(StandardType.ADD_TRUCK);
		builder2.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(0, 30,
				new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));
		builder2.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(200000000, 140,
				new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));
		builder2.add(new ScenarioBuilder.MultipleEventGenerator<TimedEvent>(200000, 30,
				new ScenarioBuilder.EventTypeFunction(StandardType.ADD_TRUCK)));

		Scenario s = builder.build();

		// run scenario with visualization attached
		final String MAP_DIR = "../core/files/maps/";

		new SimpleController(s, MAP_DIR + "leuven-simple.dot");
	}
}
