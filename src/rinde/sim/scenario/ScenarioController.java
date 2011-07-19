/**
 * 
 */
package rinde.sim.scenario;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.event.EventDispatcher;
import rinde.sim.event.Listener;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class ScenarioController implements TickListener {

	private final Scenario scenario;
	private final Simulator<?> simulator;
	private int index;
	private final EventDispatcher disp;

	public ScenarioController(Scenario scen, Simulator<?> sim, Listener l) {
		scenario = scen;
		simulator = sim;
		index = 0;
		disp = new EventDispatcher(ScenarioEvent.Type.SCENARIO_EVENT);
		disp.addListener(l, ScenarioEvent.Type.SCENARIO_EVENT);
		sim.addTickListener(this);
	}

	public void stop() {
		simulator.removeTickListener(this);
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		//		System.out.println("tick: " + index);
		while (index < scenario.events.size()) {
			//			System.out.println(scenario.events.get(index).time);
			if (scenario.events.get(index).time <= currentTime) {
				disp.dispatchEvent(new ScenarioEvent(scenario, scenario.events.get(index)));
				index++;
			} else {
				break;
			}
		}

	}
}
