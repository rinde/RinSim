/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.road.RoadModel;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class FRVehicle extends Vehicle {

	protected final VehicleDTO dto;
	protected RoadModel roadModel;
	protected PDPModel pdpModel;

	public FRVehicle(VehicleDTO pDto) {
		setStartPosition(pDto.startPosition);
		setCapacity(pDto.capacity);
		System.out.println(pDto);
		dto = pDto;
	}

	@Override
	public final double getSpeed() {
		return dto.speed * 900000.0;
	}

	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		roadModel = pRoadModel;
		pdpModel = pPdpModel;
	}

}
