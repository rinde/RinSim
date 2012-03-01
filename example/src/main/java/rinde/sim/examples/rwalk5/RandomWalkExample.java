/**
 * 
 */
package rinde.sim.examples.rwalk5;

import rinde.sim.event.pdp.StandardType;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class RandomWalkExample {

	public static void main(String[] args) throws Exception {
		// create a new simulator, load map of Leuven
		
		final int simStep = 1000; 
		
		// create simple scenario
		
		Scenario s = new Scenario() {
			@Override
			public Enum<?>[] getPossibleEventTypes() {
				return new Enum<?>[] { StandardType.ADD_TRUCK};
			}
			
		};
		
		s.add(new TimedEvent(StandardType.ADD_TRUCK, 0));
		s.add(new TimedEvent(StandardType.ADD_TRUCK, 0));
		s.add(new TimedEvent(StandardType.ADD_TRUCK, 3001));
		
		s.add(new TimedEvent(StandardType.ADD_TRUCK, 200 * simStep));
		s.add(new TimedEvent(StandardType.ADD_TRUCK, 200 * simStep));
		s.add(new TimedEvent(StandardType.ADD_TRUCK, 10000 * simStep));
		s.add(new TimedEvent(StandardType.ADD_TRUCK, 10000 * simStep));
		s.add(new TimedEvent(StandardType.ADD_TRUCK, 11000 * simStep));
		
		// run scenario with visualization attached
		
		new SimpleController(s, "files/leuven.dot");
	}
}
