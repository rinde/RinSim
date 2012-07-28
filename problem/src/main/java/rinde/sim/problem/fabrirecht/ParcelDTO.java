package rinde.sim.problem.fabrirecht;

import rinde.sim.core.graph.Point;

public class ParcelDTO {
	public final Point pickupLocation;
	public final Point destinationLocation;
	public final TimeWindow pickupTimeWindow;
	public final TimeWindow deliveryTimeWindow;
	public final int neededCapacity;
	public final long orderArrivalTime;
	public final long pickupDuration;
	public final long deliveryDuration;

	public ParcelDTO(Point pPickupLocation, Point pDestinationLocation, TimeWindow pPickupTimeWindow,
			TimeWindow pDeliveryTimeWindow, int pNeededCapacity, long pOrderArrivalTime, long pPickupDuration,
			long pDeliveryDuration) {
		pickupLocation = pPickupLocation;
		destinationLocation = pDestinationLocation;
		pickupTimeWindow = pPickupTimeWindow;
		deliveryTimeWindow = pDeliveryTimeWindow;
		neededCapacity = pNeededCapacity;
		orderArrivalTime = pOrderArrivalTime;
		pickupDuration = pPickupDuration;
		deliveryDuration = pDeliveryDuration;
	}
}