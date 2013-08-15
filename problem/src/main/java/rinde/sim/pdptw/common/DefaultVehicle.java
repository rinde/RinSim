/**
 * 
 */
package rinde.sim.pdptw.common;

import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.road.RoadModel;

/**
 * Default implementation of {@link Vehicle}, it initializes the vehicle based
 * on a {@link VehicleDTO} but it does not move.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class DefaultVehicle extends Vehicle {

	/**
	 * The data transfer object which holds the immutable properties of this
	 * vehicle.
	 */
	protected final VehicleDTO dto;

	/**
	 * A reference to the {@link RoadModel}, it is <code>null</code> until
	 * {@link #initRoadPDP(RoadModel, PDPModel)} is called.
	 */
	protected RoadModel roadModel;

	/**
	 * A reference to the {@link PDPModel}, it is <code>null</code> until
	 * {@link #initRoadPDP(RoadModel, PDPModel)} is called.
	 */
	protected PDPModel pdpModel;

	/**
	 * Instantiate a new vehicle using the specified properties.
	 * @param pDto {@link #dto}
	 */
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

	/**
	 * @return The {@link #dto}.
	 */
	public VehicleDTO getDTO() {
		return dto;
	}

}
