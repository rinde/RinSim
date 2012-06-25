package rinde.sim.lab.session3.cnet;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.lab.common.packages.Package;

public class PackageAgent implements TickListener, SimulatorUser, CommunicationUser {

	private SimulatorAPI simulator;
	private final Package myPackage;
	private CommunicationAPI communicationAPI;
	private final Mailbox mailbox;
	private final double radius;
	private final double reliability;

	public PackageAgent(Package myPackage, double radius, double reliability) {
		this.myPackage = myPackage;
		this.radius = radius;
		this.reliability = reliability;
		mailbox = new Mailbox();
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		simulator = api;
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		// TODO exercise

	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		communicationAPI = api;
	}

	@Override
	public Point getPosition() {
		// TODO
		return myPackage.getPickupLocation();
	}

	@Override
	public double getRadius() {
		return radius;
	}

	@Override
	public double getReliability() {
		return reliability;
	}

	@Override
	public void receive(Message message) {
		mailbox.receive(message);
	}

}
