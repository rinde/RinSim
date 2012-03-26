package rinde.sim.lab.session3.cnet;

import java.util.LinkedList;
import java.util.Queue;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.lab.common.trucks.Truck;

public class TruckAgent implements TickListener, SimulatorUser, CommunicationUser {

	private SimulatorAPI simulator;
	private Queue<Point> path;
	private Truck truck;
	private CommunicationAPI communicationAPI;
	private double reliability, radius;
	private Mailbox mailbox;
	
	public TruckAgent(Truck truck, double radius, double reliability){
		this.truck = truck;
		this.radius = radius;
		this.reliability = reliability;
		this.mailbox = new Mailbox();
	}
	
	@Override
	public void setSimulator(SimulatorAPI api) {
		this.simulator = api;
	}

	/**
	 * Very dumb agent, that chooses paths randomly and tries to pickup stuff and deliver stuff at the end of his paths
	 */
	@Override
	public void tick(long currentTime, long timeStep) {
		//TODO exercise
		if(path == null || path.isEmpty()){
			truck.tryPickup();
			truck.tryDelivery();
			Point destination = truck.getRoadModel().getGraph().getRandomNode(simulator.getRandomGenerator());
			this.path = new LinkedList<Point>(truck.getRoadModel().getShortestPathTo(truck, destination));
		}else{
			truck.drive(path, timeStep);
		}
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
		return this.truck.getPosition();
	}

	@Override
	public double getRadius() {
		return this.radius;
	}

	@Override
	public double getReliability() {
		return this.reliability;
	}

	@Override
	public void receive(Message message) {
		this.mailbox.receive(message);
	}

}
