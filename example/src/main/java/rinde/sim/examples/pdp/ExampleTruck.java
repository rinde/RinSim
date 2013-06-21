/**
 * 
 */
package rinde.sim.examples.pdp;

import java.util.Collection;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.time.TimeLapse;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ExampleTruck extends Vehicle {

	protected RoadModel roadModel;
	protected PDPModel pdpModel;

	protected Parcel curr;

	public ExampleTruck(Point startPosition, double capacity) {
		setStartPosition(startPosition);
		setCapacity(capacity);
	}

	@Override
	public double getSpeed() {
		return 1000;
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {}

	@Override
	protected void tickImpl(TimeLapse time) {
		final Collection<Parcel> parcels = pdpModel.getAvailableParcels();

		if (pdpModel.getContents(this).isEmpty()) {
			if (!parcels.isEmpty() && curr == null) {
				double dist = Double.POSITIVE_INFINITY;
				for (final Parcel p : parcels) {
					final double d = Point.distance(roadModel.getPosition(this), roadModel.getPosition(p));
					if (d < dist) {
						dist = d;
						curr = p;
					}
				}
			}

			if (curr != null && roadModel.containsObject(curr)) {
				roadModel.moveTo(this, curr, time);

				if (roadModel.equalPosition(this, curr)) {
					pdpModel.pickup(this, curr, time);
				}
			} else {
				curr = null;
			}
		} else {
			roadModel.moveTo(this, curr.getDestination(), time);
			if (roadModel.getPosition(this).equals(curr.getDestination())) {
				pdpModel.deliver(this, curr, time);
			}
		}
	}

	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		roadModel = pRoadModel;
		pdpModel = pPdpModel;
	}
}
