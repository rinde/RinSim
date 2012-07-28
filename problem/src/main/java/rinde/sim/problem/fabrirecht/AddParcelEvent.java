package rinde.sim.problem.fabrirecht;

import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.scenario.TimedEvent;

public class AddParcelEvent extends TimedEvent {

	public final ParcelDTO parcelDTO;

	public AddParcelEvent(ParcelDTO dto) {
		super(PDPScenarioEvent.ADD_PARCEL, dto.orderArrivalTime);
		parcelDTO = dto;
	}
}