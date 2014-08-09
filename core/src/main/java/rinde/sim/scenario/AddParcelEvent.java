package rinde.sim.scenario;

import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.pdptw.ParcelDTO;

/**
 * Event indicating that a parcel can be created.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class AddParcelEvent extends TimedEvent {

  /**
   * The data which should be used to instantiate a new parcel.
   */
  public final ParcelDTO parcelDTO;

  /**
   * New instance.
   * @param dto {@link #parcelDTO}
   */
  public AddParcelEvent(ParcelDTO dto) {
    super(PDPScenarioEvent.ADD_PARCEL, dto.orderAnnounceTime);
    parcelDTO = dto;
  }
}
