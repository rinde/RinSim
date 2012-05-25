package rinde.sim.lab.session2.gradient_field_exercise.packages;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;
import rinde.sim.lab.model.virtual.FieldData;
import rinde.sim.lab.model.virtual.GradientFieldAPI;
import rinde.sim.lab.model.virtual.VirtualEntity;

public class PackageAgent implements TickListener, SimulatorUser {

	private SimulatorAPI simulator;
	private Package myPackage;
	private double priority;
	
	
	public PackageAgent(Package myPackage){
		this.priority = 1;
		this.myPackage = myPackage;
	}
	
	@Override
	public void setSimulator(SimulatorAPI api) {
		this.simulator = api;
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		// TODO Auto-generated method stub
		if(this.myPackage.delivered())
			this.simulator.unregister(this);
		
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		// TODO Auto-generated method stub
		
	}

}
