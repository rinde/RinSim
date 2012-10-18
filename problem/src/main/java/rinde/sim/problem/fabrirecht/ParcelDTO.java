package rinde.sim.problem.fabrirecht;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import rinde.sim.core.graph.Point;
import rinde.sim.util.TimeWindow;

public class ParcelDTO implements Serializable {
	private static final long serialVersionUID = -6128057042614968652L;
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

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}