package rinde.sim.lab.session2.gradient_field_exercise.trucks;

import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.PathProgress;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.lab.session2.gradient_field_exercise.packages.Package;

public class Truck implements MovingRoadUser {

	private RoadModel rm;
	private final Point startLocation;
	private final String truckID;
	private final double speed;
	private Package load;
	protected static final Logger LOGGER = LoggerFactory.getLogger(Truck.class);

	public Truck(String truckID, Point startLocation, double speed) {
		this.truckID = truckID;
		this.startLocation = startLocation;
		this.speed = speed;
	}

	public String getTruckID() {
		return truckID;
	}

	@Override
	public void initRoadUser(RoadModel model) {
		rm = model;
		rm.addObjectAt(this, startLocation);
	}

	@Override
	public double getSpeed() {
		return speed;
	}

	public RoadModel getRoadModel() {
		return rm;
	}

	public PathProgress drive(Queue<Point> path, long time) {
		return rm.followPath(this, path, time);
	}

	public Point getPosition() {
		return rm.getPosition(this);
	}

	public boolean hasLoad() {
		return load != null;
	}

	public Package getLoad() {
		return load;
	}

	public boolean tryPickup() {
		if (load == null) {
			Set<Package> packages = rm.getObjectsAt(this, Package.class);
			if (!packages.isEmpty()) {
				Package p = (Package) packages.toArray()[0];
				load = p;
				p.pickup();
				LOGGER.info(truckID + " picked up " + p);
				return true;
			}
		}
		return false;
	}

	public boolean tryDelivery() {
		if (load != null) {
			if (load.getDeliveryLocation().equals(getPosition())) {
				LOGGER.info(truckID + " delivered " + load);
				load.deliver();
				load = null;
				return true;
			}
		}
		return false;
	}
}
