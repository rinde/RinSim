package rinde.sim.lab.session2.example1;

import rinde.sim.core.Simulator;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.scenario.TimedEvent;

public class ScenarioControllerTemplate extends ScenarioController {

	public ScenarioControllerTemplate(Scenario scen, int numberOfTicks) throws ConfigurationException {
		super(scen, numberOfTicks);

		// TODO constructor logic

		initialize();
	}

	// Required
	@Override
	protected Simulator createSimulator() throws Exception {
		// TODO logic to create simulator
		return null;
	}

	// Required
	@Override
	protected boolean handleTimedEvent(TimedEvent event) {
		// TODO logic for handling events
		return false;
	}

	// Optional
	@Override
	protected boolean createUserInterface() {
		// TODO logic to create GUI
		return true;
	}

}
