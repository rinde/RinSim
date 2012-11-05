/**
 * 
 */
package rinde.sim.problem.common;

import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.road.RoadModel;

/**
 * Default implementation of {@link Vehicle}, it initializes the vehicle based
 * on a {@link VehicleDTO} but it does not move.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class DefaultVehicle extends Vehicle {

	protected final VehicleDTO dto;
	protected RoadModel roadModel;
	protected PDPModel pdpModel;

	public DefaultVehicle(VehicleDTO pDto) {
		setStartPosition(pDto.startPosition);
		setCapacity(pDto.capacity);
		dto = pDto;
	}

	@Override
	public final double getSpeed() {
		return dto.speed;
	}

	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		roadModel = pRoadModel;
		pdpModel = pPdpModel;
	}

	public VehicleDTO getDTO() {
		return dto;
	}

}
