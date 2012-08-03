/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class FRParcel extends Parcel {

	public final ParcelDTO dto;

	public FRParcel(ParcelDTO pDto) {
		super(pDto.destinationLocation, pDto.pickupDuration, pDto.pickupTimeWindow, pDto.deliveryDuration,
				pDto.deliveryTimeWindow, pDto.neededCapacity);
		setStartPosition(pDto.pickupLocation);
		dto = pDto;
	}

	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

	@Override
	public boolean canBePickedUp(Vehicle v, long time) {
		return time >= dto.pickupTimeWindow.begin && time < dto.pickupTimeWindow.end;
	}

	@Override
	public boolean canBeDelivered(Vehicle v, long time) {
		return time >= dto.pickupTimeWindow.begin && time < dto.pickupTimeWindow.end;
	}

}
