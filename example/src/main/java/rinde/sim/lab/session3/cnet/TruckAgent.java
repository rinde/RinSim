package rinde.sim.lab.session3.cnet;

import java.util.LinkedList;
import java.util.Queue;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.lab.common.trucks.Truck;

public class TruckAgent implements TickListener, SimulatorUser, CommunicationUser {

	private SimulatorAPI simulator;
	private Queue<Point> path;
	private final Truck truck;
	private CommunicationAPI communicationAPI;
	private final double reliability, radius;
	private final Mailbox mailbox;

	public TruckAgent(Truck truck, double radius, double reliability) {
		this.truck = truck;
		this.radius = radius;
		this.reliability = reliability;
		mailbox = new Mailbox();
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		simulator = api;
	}

	/**
	 * Very dumb agent, that chooses paths randomly and tries to pickup stuff
	 * and deliver stuff at the end of his paths
	 */
	@Override
	public void tick(TimeLapse timeLapse) {
		// TODO exercise
		if (path == null || path.isEmpty()) {
			truck.tryPickup();
			truck.tryDelivery();
			Point destination = truck.getRoadModel().getRandomPosition(simulator.getRandomGenerator());
			path = new LinkedList<Point>(truck.getRoadModel().getShortestPathTo(truck, destination));
		} else {
			truck.drive(path, timeLapse);
		}
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
		return truck.getPosition();
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
