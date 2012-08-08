/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import static com.google.common.collect.Maps.newLinkedHashMap;

import java.util.Map;

import org.apache.commons.math3.random.MersenneTwister;

import rinde.sim.core.Simulator;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.PDPModelEvent;
import rinde.sim.core.model.pdp.PDPModel.PDPModelEventType;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.model.pdp.twpolicy.TardyAllowedPolicy;
import rinde.sim.core.model.road.AbstractRoadModel.RoadEvent;
import rinde.sim.core.model.road.MoveEvent;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class FabriRechtProblem extends ScenarioController {

	protected final FabriRechtScenario fabriRechtScenario;
	protected final StatisticsListener statisticsListener;

	/**
	 * @param scen
	 * @param numberOfTicks
	 */
	public FabriRechtProblem(FabriRechtScenario scen) {
		super(scen, (int) (scen.timeWindow.end - scen.timeWindow.begin));
		fabriRechtScenario = scen;
		statisticsListener = new StatisticsListener();
	}

	// subclasses can override this method to add more models
	@Override
	protected Simulator createSimulator() throws Exception {
		final Simulator sim = new Simulator(new MersenneTwister(123), 1);
		final RoadModel rm = new PlaneRoadModel(fabriRechtScenario.min, fabriRechtScenario.max, false, 1.0);
		final PDPModel pm = new PDPModel(rm, new TardyAllowedPolicy());
		rm.getEventAPI().addListener(statisticsListener, RoadEvent.MOVE);
		pm.getEventAPI()
				.addListener(statisticsListener, PDPModelEventType.START_PICKUP, PDPModelEventType.END_PICKUP, PDPModelEventType.START_DELIVERY, PDPModelEventType.END_DELIVERY);
		sim.register(rm);
		sim.register(pm);
		return sim;
	}

	@Override
	protected final boolean handleTimedEvent(TimedEvent event) {
		if (event.getEventType() == PDPScenarioEvent.ADD_PARCEL) {
			statisticsListener.parcelCount++;
			return handleAddParcel(((AddParcelEvent) event));
		} else if (event.getEventType() == PDPScenarioEvent.ADD_VEHICLE) {
			return handleAddVehicle((AddVehicleEvent) event);
		} else if (event.getEventType() == PDPScenarioEvent.ADD_DEPOT) {
			return handleAddDepot((AddDepotEvent) event);
		} else if (event.getEventType() == PDPScenarioEvent.TIME_OUT) {
			getSimulator().stop();
			return handleTimeOut();
		}
		return false;
	}

	protected abstract boolean handleAddVehicle(AddVehicleEvent event);

	protected abstract boolean handleTimeOut();

	protected boolean handleAddParcel(AddParcelEvent event) {
		return getSimulator().register(new FRParcel(event.parcelDTO));
	}

	protected boolean handleAddDepot(AddDepotEvent event) {
		return getSimulator().register(new FRDepot(event.position));
	}

	protected class StatisticsListener implements Listener {

		protected final Map<MovingRoadUser, Double> distanceMap;
		protected double totalDistance;
		protected int totalPickups;
		protected int totalDeliveries;
		protected long pickupTardiness;
		protected long deliveryTardiness;
		protected int parcelCount;

		public StatisticsListener() {
			distanceMap = newLinkedHashMap();
			totalDistance = 0d;
			totalPickups = 0;
			totalDeliveries = 0;
			parcelCount = 0;
		}

		@Override
		public void handleEvent(Event e) {
			if (e.getEventType() == RoadEvent.MOVE) {
				final MoveEvent me = ((MoveEvent) e);
				increment(me.roadUser, me.pathProgress.distance);
				totalDistance += me.pathProgress.distance;
			} else if (e.getEventType() == PDPModelEventType.START_PICKUP) {
				final PDPModelEvent pme = (PDPModelEvent) e;
				final long latestBeginTime = pme.parcel.getPickupTimeWindow().begin - pme.parcel.getPickupDuration();
				if (pme.time > latestBeginTime) {
					pickupTardiness += pme.time - latestBeginTime;
				}

			} else if (e.getEventType() == PDPModelEventType.END_PICKUP) {
				totalPickups++;
			} else if (e.getEventType() == PDPModelEventType.START_DELIVERY) {
				final PDPModelEvent pme = (PDPModelEvent) e;
				final long latestBeginTime = pme.parcel.getDeliveryTimeWindow().begin
						- pme.parcel.getDeliveryDuration();
				if (pme.time > latestBeginTime) {
					deliveryTardiness += pme.time - latestBeginTime;
				}
			} else if (e.getEventType() == PDPModelEventType.END_DELIVERY) {
				totalDeliveries++;
			}
		}

		protected void increment(MovingRoadUser mru, double num) {
			if (!distanceMap.containsKey(mru)) {
				distanceMap.put(mru, num);
			} else {
				distanceMap.put(mru, distanceMap.get(mru) + num);
			}
		}

		public double getTotalTraveledDistance() {
			return totalDistance;
		}

		public int getTotalPickups() {
			return totalPickups;
		}

		public int getTotalDeliveries() {
			return totalDeliveries;
		}

		public String report() {
			final StringBuilder sb = new StringBuilder();
			sb.append("\t\t\t = Statistics = \n total traveled distance:\t");
			sb.append(totalDistance);
			sb.append("\n pickups:\t\t\t");
			sb.append(totalPickups);
			sb.append(" / ");
			sb.append(parcelCount);
			sb.append("\t");
			sb.append(Math.round((totalPickups / parcelCount) * 100d));
			sb.append("%");
			sb.append("\n deliveries:\t\t\t");
			sb.append(totalDeliveries);
			sb.append(" / ");
			sb.append(parcelCount);
			sb.append("\t");
			sb.append(Math.round((totalDeliveries / parcelCount) * 100d));
			sb.append("%");
			sb.append("\n pickup tardiness:\t\t");
			sb.append(pickupTardiness);
			sb.append("\n delivery tardiness:\t\t");
			sb.append(deliveryTardiness);
			return sb.toString();
		}
	}

}
