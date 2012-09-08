/**
 * 
 */
package rinde.sim.examples.fabrirecht;

import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
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

}
