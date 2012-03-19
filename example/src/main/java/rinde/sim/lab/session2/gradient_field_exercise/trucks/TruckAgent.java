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
	private Truck truck;
	
	private boolean isEmitting;
	
	public TruckAgent(Truck truck, int timerInterval){
		this.isEmitting = true;
		this.truck = truck;
	}
	
	@Override
	public void setSimulator(SimulatorAPI api) {
		this.simulator = api;
	}
	
	@Override
	public void tick(long currentTime, long timeStep) {
		if(path == null || path.isEmpty()){
			truck.tryPickup();
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

}
