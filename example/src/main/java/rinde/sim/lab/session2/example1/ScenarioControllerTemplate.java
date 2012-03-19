package rinde.sim.lab.session2.example1;

import rinde.sim.core.Simulator;
import rinde.sim.event.Event;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioController;

public class ScenarioControllerTemplate extends ScenarioController{

	public ScenarioControllerTemplate(Scenario scen, int numberOfTicks) throws ConfigurationException {
		super(scen, numberOfTicks);
		
		//TODO constructor logic 

		initialize();
	}

	// Required
	@Override 
	protected Simulator createSimulator() throws Exception {
		//TODO logic to create simulator
		return null;	
	}
	
	// Optional
	@Override 
	protected boolean createUserInterface() {
		//TODO logic to create GUI
		return true;
	}

	// Optional
	@Override
	protected boolean handleCustomEvent(Event e){
		//TODO logic to handle custom events
		return false;
	}
	
	// Optional
	@Override
	protected boolean handleRemoveTruck(Event e) {
		//TODO logic to remove truck
		return false;
	}

	// Optional
	@Override
	protected boolean handleAddTruck(Event e) {
		
		//TODO logic to add truck
		return false;
	}

	// Optional
	@Override
	protected boolean handleRemovePackage(Event e) {
		//TODO logic to remove package
		return false;
	}

	// Optional
	@Override
	protected boolean handleAddPackage(Event e) {
		//TODO logic to add package
		return false;
	}

}
