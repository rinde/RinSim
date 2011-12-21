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

	public ScenarioController(Scenario scen, Simulator<?> sim) {
		scenario = scen;
		simulator = sim;
		index = 0;
		disp = new EventDispatcher(ScenarioEvent.Type.SCENARIO_EVENT);
		sim.addTickListener(this);
	}

	public void addListener(Listener l) {
		disp.addListener(l, ScenarioEvent.Type.SCENARIO_EVENT);
	}

	public void stop() {
		simulator.removeTickListener(this);
	}

	/**
	 * Returns true if all events of this scenario have been dispatched.
	 * @return
	 */
	public boolean isFinished() {
		return index == scenario.getEvents().size();
	}

	/**
	 * Dispatch all events which should happen before the scenario starts
	 * playing. Should be called before any {@link #tick(long, long)}
	 * invocations.
	 */
	public void init() {
		while (index < scenario.getEvents().size()) {
			if (scenario.getEvents().get(index).time <= 0) {
				disp.dispatchEvent(new ScenarioEvent(scenario, scenario.getEvents().get(index)));
				index++;
			} else {
				break;
			}
		}
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		//		System.out.println("tick: " + index);
		while (index < scenario.getEvents().size()) {
			//			System.out.println(scenario.events.get(index).time);
			if (scenario.getEvents().get(index).time <= currentTime) {
				disp.dispatchEvent(new ScenarioEvent(scenario, scenario.getEvents().get(index)));
				index++;
			} else {
				break;
			}
		}

	}
}
