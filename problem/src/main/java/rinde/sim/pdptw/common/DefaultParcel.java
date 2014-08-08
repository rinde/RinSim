/**
 * 
 */
package rinde.sim.pdptw.common;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.pdptw.ParcelDTO;

/**
 * Default {@link Parcel} implementation. It is instantiated using a
 * {@link ParcelDTO}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class DefaultParcel extends Parcel {

  /**
   * A data object which describes the immutable properties of this parcel.
   */
  public final ParcelDTO dto;

  /**
   * Instantiate a new parcel using the data transfer object.
   * @param pDto {@link #dto}
   */
  public DefaultParcel(ParcelDTO pDto) {
    super(pDto.deliveryLocation, pDto.pickupDuration, pDto.pickupTimeWindow,
        pDto.deliveryDuration, pDto.deliveryTimeWindow, pDto.neededCapacity);
    setStartPosition(pDto.pickupLocation);
    dto = pDto;
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

  @Override
  public String toString() {
    return "[DefaultParcel " + dto + "]";
  }

  /**
   * @return The pickup location of this parcel.
   */
  public Point getPickupLocation() {
    return dto.pickupLocation;
  }
}
