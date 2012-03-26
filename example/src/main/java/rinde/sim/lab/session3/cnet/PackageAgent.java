package rinde.sim.lab.session3.cnet;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.lab.common.packages.Package;

public class PackageAgent implements TickListener, SimulatorUser, CommunicationUser {

	private SimulatorAPI simulator;
	private Package myPackage;
	private CommunicationAPI communicationAPI;
	private Mailbox mailbox;
	private double radius;
	private double reliability;
	
	
	public PackageAgent(Package myPackage, double radius, double reliability){
		this.myPackage = myPackage;
		this.radius = radius;
		this.reliability = reliability;
		this.mailbox = new Mailbox();
	}
	
	@Override
	public void setSimulator(SimulatorAPI api) {
		this.simulator = api;
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		//TODO exercise
		
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		this.communicationAPI = api;
	}

	@Override
	public Point getPosition() {
		//TODO
		return this.myPackage.getPickupLocation();
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
		this.mailbox.receive(message);
	}

}
