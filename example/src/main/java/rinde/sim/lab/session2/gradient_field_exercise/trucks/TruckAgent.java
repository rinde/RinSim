package rinde.sim.lab.session2.gradient_field_exercise.trucks;

import java.util.LinkedList;
import java.util.Queue;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;

public class TruckAgent implements TickListener, SimulatorUser {

	private SimulatorAPI simulator;
	private Queue<Point> path;
	private final Truck truck;

	private final boolean isEmitting;

	public TruckAgent(Truck truck, int timerInterval) {
		isEmitting = true;
		this.truck = truck;
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
	public void tick(long currentTime, long timeStep) {
		// TODO exercise
		if (path == null || path.isEmpty()) {
			truck.tryPickup();
			truck.tryDelivery();
			Point destination = truck.getRoadModel().getRandomPosition(simulator.getRandomGenerator());
			path = new LinkedList<Point>(truck.getRoadModel().getShortestPathTo(truck, destination));
		} else {
			truck.drive(path, timeStep);
		}
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		// TODO Auto-generated method stub

	}

}
