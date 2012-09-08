package rinde.sim.examples.fabrirecht.gradientField;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.RoadModels;
import rinde.sim.core.model.road.RoadUser;
import rinde.sim.examples.fabrirecht.FRParcel;
import rinde.sim.examples.fabrirecht.FRVehicle;
import rinde.sim.examples.fabrirecht.VehicleDTO;

import com.google.common.base.Predicate;

class Truck extends FRVehicle implements FieldEmitter {
	private GradientModel gradientModel;

	public Truck(VehicleDTO pDto) {
		super(pDto);
	}

	@Override
	protected void tickImpl(TimeLapse time) {

		//Check if we can deliver nearby
		Parcel delivery = getDelivery(time, 5);

		if( delivery != null ){
			if( delivery.getDestination().equals(getPosition())
					&& pdpModel.getVehicleState(this) == VehicleState.IDLE){
				pdpModel.deliver(this, delivery, time);	
			}
			else{
				roadModel.moveTo(this, delivery.getDestination(), time);
			}
			return;
		}


		//Otherwise, Check if we can pickup nearby
		FRParcel closest = (FRParcel) RoadModels
				.findClosestObject(roadModel.getPosition(this), roadModel,
						new Predicate<RoadUser>() {
					@Override
					public boolean apply(RoadUser input) {
						return input instanceof FRParcel
								&& pdpModel.getParcelState(((FRParcel) input)) == ParcelState.AVAILABLE;
					}
				}
						);


		if (closest != null && Point.distance(pdpModel.getPosition(closest), getPosition()) < 10){
			if(	roadModel.equalPosition(closest, this) &&
					pdpModel.getTimeWindowPolicy()
					.canPickup(closest.getPickupTimeWindow(), time.getTime(), closest.getPickupDuration())) {
				double newSize = getPDPModel().getContentsSize(this) + closest.getMagnitude();

				if( newSize <= this.getCapacity() )
					pdpModel.pickup(this, closest, time);
			}
			else {
				roadModel.moveTo(this, pdpModel.getPosition(closest), time);
			}
			return;
		}

		//If none of the above, let the gradient field guide us!
		roadModel.moveTo(this, gradientModel.getTargetFor(this), time);
	}

	public Parcel getDelivery(TimeLapse time, int distance){
		Parcel target = null;
		double closest = distance;

		for(Parcel p:pdpModel.getContents(this)){
			
			double dist = Point.distance(pdpModel.getPosition(this), p.getDestination());
			if( dist < closest && pdpModel.getTimeWindowPolicy().canDeliver(p.getDeliveryTimeWindow(), time.getTime(), p.getPickupDuration())){
				closest = dist;
				target = p;
			}
		}

		return target;
	}

	@Override
	public void setModel(GradientModel model) {
		gradientModel = model;
	}

	@Override
	public Point getPosition() {
		return roadModel.getPosition(this);
	}

	@Override
	public float getStrength() {
		return -1;
	}
}
