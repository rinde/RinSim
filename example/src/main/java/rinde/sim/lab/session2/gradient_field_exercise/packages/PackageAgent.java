package rinde.sim.lab.session2.gradient_field_exercise.packages;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;

public class PackageAgent implements TickListener, SimulatorUser {

	private SimulatorAPI simulator;
	private final Package myPackage;
	private final double priority;

	public PackageAgent(Package myPackage) {
		priority = 1;
		this.myPackage = myPackage;
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		simulator = api;
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		if (myPackage.delivered()) {
			simulator.unregister(this);
		}

	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub

	}

}
