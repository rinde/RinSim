/**
 * 
 */
package rinde.sim.problem.common;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static rinde.sim.core.Simulator.SimulatorEventType.STARTED;
import static rinde.sim.core.Simulator.SimulatorEventType.STOPPED;
import static rinde.sim.core.model.pdp.PDPModel.PDPModelEventType.END_DELIVERY;
import static rinde.sim.core.model.pdp.PDPModel.PDPModelEventType.END_PICKUP;
import static rinde.sim.core.model.pdp.PDPModel.PDPModelEventType.NEW_PARCEL;
import static rinde.sim.core.model.pdp.PDPModel.PDPModelEventType.START_DELIVERY;
import static rinde.sim.core.model.pdp.PDPModel.PDPModelEventType.START_PICKUP;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.ADD_DEPOT;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.ADD_PARCEL;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.ADD_VEHICLE;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.TIME_OUT;
import static rinde.sim.core.model.road.AbstractRoadModel.RoadEventType.MOVE;
import static rinde.sim.scenario.ScenarioController.EventType.SCENARIO_FINISHED;
import static rinde.sim.scenario.ScenarioController.EventType.SCENARIO_STARTED;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import rinde.sim.core.Simulator;
import rinde.sim.core.Simulator.SimulatorEventType;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.PDPModelEvent;
import rinde.sim.core.model.pdp.PDPModel.PDPModelEventType;
import rinde.sim.core.model.road.AbstractRoadModel.RoadEventType;
import rinde.sim.core.model.road.MoveEvent;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.event.Event;
import rinde.sim.event.EventAPI;
import rinde.sim.event.EventDispatcher;
import rinde.sim.event.Listener;
import rinde.sim.problem.common.DynamicPDPTWProblem.StatisticsListener;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.scenario.TimedEvent;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class StatsTracker implements StatisticsListener {

	protected final EventDispatcher eventDispatcher;
	protected final TheListener theListener;
	protected Simulator simulator;

	public enum StatisticsEventType {
		PICKUP_TARDINESS, DELIVERY_TARDINESS;
	}

	public StatsTracker() {
		eventDispatcher = new EventDispatcher(StatisticsEventType.values());
		theListener = new TheListener();
	}

	@Override
	public void register(ScenarioController scenContr, Simulator sim) {
		checkState(simulator == null, "StatsTracker can be attached to only one Simulator instance");
		simulator = sim;
		scenContr
				.getEventAPI()
				.addListener(theListener, SCENARIO_STARTED, SCENARIO_FINISHED, ADD_DEPOT, ADD_PARCEL, ADD_VEHICLE, TIME_OUT);
		simulator.getEventAPI().addListener(theListener, STARTED, STOPPED);
		simulator.getModelProvider().getModel(RoadModel.class).getEventAPI().addListener(theListener, MOVE);
		simulator.getModelProvider().getModel(PDPModel.class).getEventAPI()
				.addListener(theListener, START_PICKUP, END_PICKUP, START_DELIVERY, END_DELIVERY, NEW_PARCEL);
	}

	public EventAPI getEventAPI() {
		return eventDispatcher.getEventAPI();
	}

	public StatisticsDTO getStatsDTO() {

		// final RoadModel rm =
		// simulator.getModelProvider().getModel(RoadModel.class);
		// final Set<DefaultVehicle> vehicles =
		// rm.getObjectsOfType(DefaultVehicle.class);
		// int vehicleBack = 0;
		// for (final DefaultVehicle v : vehicles) {
		// if (rm.getPosition(v).equals(v.getDTO().startPosition)) {
		// vehicleBack++;
		// }
		// }

		final int vehicleBack = theListener.lastArrivalTimeAtDepot.size();
		long overTime = 0;
		if (theListener.simFinish) {
			for (final Long time : theListener.lastArrivalTimeAtDepot.values()) {
				if (time - theListener.scenarioEndTime > 0) {
					overTime += time - theListener.scenarioEndTime;
				}
			}
		} else {
			overTime = -1;
		}

		return new StatisticsDTO(theListener.totalDistance, theListener.totalPickups, theListener.totalDeliveries,
				theListener.totalParcels, theListener.acceptedParcels, theListener.pickupTardiness,
				theListener.deliveryTardiness, theListener.computationTime, theListener.simulationTime,
				theListener.simFinish, vehicleBack, overTime, theListener.totalVehicles, theListener.distanceMap.size());
	}

	class TheListener implements Listener {
		// parcels
		protected int totalParcels;
		protected int acceptedParcels;

		// vehicles
		protected int totalVehicles;
		protected final Map<MovingRoadUser, Double> distanceMap;
		protected double totalDistance;
		protected final Map<MovingRoadUser, Long> lastArrivalTimeAtDepot;

		protected int totalPickups;
		protected int totalDeliveries;
		protected long pickupTardiness;
		protected long deliveryTardiness;

		// simulation
		protected long startTimeReal;
		protected long startTimeSim;
		protected long computationTime;
		protected long simulationTime;

		protected boolean simFinish;
		protected long scenarioEndTime;

		public TheListener() {
			totalParcels = 0;
			acceptedParcels = 0;

			totalVehicles = 0;
			distanceMap = newLinkedHashMap();
			totalDistance = 0d;
			lastArrivalTimeAtDepot = newLinkedHashMap();

			totalPickups = 0;
			totalDeliveries = 0;
			pickupTardiness = 0;
			deliveryTardiness = 0;

			simFinish = false;
		}

		@Override
		public void handleEvent(Event e) {
			// System.out.println(e);
			if (e.getEventType() == SimulatorEventType.STARTED) {
				startTimeReal = System.currentTimeMillis();
				startTimeSim = simulator.getCurrentTime();
			} else if (e.getEventType() == SimulatorEventType.STOPPED) {
				computationTime = System.currentTimeMillis() - startTimeReal;
				simulationTime = simulator.getCurrentTime() - startTimeSim;
			} else if (e.getEventType() == RoadEventType.MOVE) {
				final MoveEvent me = (MoveEvent) e;
				increment(me.roadUser, me.pathProgress.distance);
				totalDistance += me.pathProgress.distance;
				// if we are closer than 10 cm to the depot, we say we are 'at'
				// the depot
				if (Point
						.distance(me.roadModel.getPosition(me.roadUser), ((DefaultVehicle) me.roadUser).dto.startPosition) < 0.0001) {
					// only override time if the vehicle did actually move
					if (me.pathProgress.distance > 0.0001) {
						lastArrivalTimeAtDepot.put(me.roadUser, simulator.getCurrentTime());
					}
				} else {
					lastArrivalTimeAtDepot.remove(me.roadUser);
				}

				//
				// if( totalVehicles == lastArrivalTimeAtDepot.size() ){
				//
				// }

			} else if (e.getEventType() == PDPModelEventType.START_PICKUP) {
				final PDPModelEvent pme = (PDPModelEvent) e;
				final long latestBeginTime = pme.parcel.getPickupTimeWindow().end - pme.parcel.getPickupDuration();
				if (pme.time > latestBeginTime) {
					pickupTardiness += pme.time - latestBeginTime;
					eventDispatcher.dispatchEvent(new Event(StatisticsEventType.PICKUP_TARDINESS, this));
				}
			} else if (e.getEventType() == PDPModelEventType.END_PICKUP) {
				totalPickups++;
			} else if (e.getEventType() == PDPModelEventType.START_DELIVERY) {
				final PDPModelEvent pme = (PDPModelEvent) e;
				final long latestBeginTime = pme.parcel.getDeliveryTimeWindow().end - pme.parcel.getDeliveryDuration();
				if (pme.time > latestBeginTime) {
					deliveryTardiness += pme.time - latestBeginTime;
					eventDispatcher.dispatchEvent(new Event(StatisticsEventType.DELIVERY_TARDINESS, this));
				}
			} else if (e.getEventType() == PDPModelEventType.END_DELIVERY) {
				totalDeliveries++;
			} else if (e.getEventType() == ADD_PARCEL) {
				// scenario event
				totalParcels++;
			} else if (e.getEventType() == NEW_PARCEL) {
				// pdp model event
				acceptedParcels++;
			} else if (e.getEventType() == ADD_VEHICLE) {
				totalVehicles++;
			} else if (e.getEventType() == TIME_OUT) {
				simFinish = true;
				scenarioEndTime = ((TimedEvent) e).time;
			} else {
				// System.out.println("fall through: " + e);
			}

		}

		protected void increment(MovingRoadUser mru, double num) {
			if (!distanceMap.containsKey(mru)) {
				distanceMap.put(mru, num);
			} else {
				distanceMap.put(mru, distanceMap.get(mru) + num);
			}
		}
	}

	public static class StatisticsDTO implements Serializable {
		private static final long serialVersionUID = 1968951252238291733L;
		/**
		 * The cummulative distance all vehicle have traveled.
		 */
		public final double totalDistance;
		/**
		 * The total number of parcels that are picked up.
		 */
		public final int totalPickups;
		/**
		 * The total number of parcels that are delivered.
		 */
		public final int totalDeliveries;
		/**
		 * The total number of parcels in the scenario.
		 */
		public final int totalParcels;
		/**
		 * The total number of parcels that were actually added in the model.
		 */
		public final int acceptedParcels;
		/**
		 * The cummulative pickup tardiness of all parcels.
		 */
		public final long pickupTardiness;
		/**
		 * The cummulative delivery tardiness of all parcels.
		 */
		public final long deliveryTardiness;
		/**
		 * The time (ms) it took to compute the simulation.
		 */
		public final long computationTime;
		/**
		 * The time that has elapsed in the simulation.
		 */
		public final long simulationTime;
		/**
		 * Indicates whether the scenario has finished.
		 */
		public final boolean simFinish;
		/**
		 * The number of vehicles that are back at the depot.
		 */
		public final int vehiclesAtDepot;
		/**
		 * The cummulative tardiness of vehicle arrival at depot.
		 */
		public final long overTime;
		/**
		 * Total number of vehicles available.
		 */
		public final int totalVehicles;
		/**
		 * Number of vehicles that have been used, 'used' means has moved.
		 */
		public final int movedVehicles;
		/**
		 * The average cost per demand. Both pickups and deliveries are demands.
		 * It is defined as:
		 * <code>totalDistance / (totalPickups + totalDeliveries)</code>.
		 */
		public final double costPerDemand;

		public StatisticsDTO(double dist, int pick, int del, int parc, int accP, long pickTar, long delTar, long compT,
				long simT, boolean finish, int atDepot, long overT, int total, int moved) {
			totalDistance = dist;
			totalPickups = pick;
			totalDeliveries = del;
			totalParcels = parc;
			acceptedParcels = accP;
			pickupTardiness = pickTar;
			deliveryTardiness = delTar;
			computationTime = compT;
			simulationTime = simT;
			simFinish = finish;
			vehiclesAtDepot = atDepot;
			overTime = overT;
			totalVehicles = total;
			movedVehicles = moved;
			costPerDemand = totalDistance / (totalPickups + totalDeliveries);
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
		}
	}

}
