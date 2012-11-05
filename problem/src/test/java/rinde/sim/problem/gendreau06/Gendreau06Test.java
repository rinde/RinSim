/**
 * 
 */
package rinde.sim.problem.gendreau06;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.problem.common.AddDepotEvent;
import rinde.sim.problem.common.AddParcelEvent;
import rinde.sim.problem.common.AddVehicleEvent;
import rinde.sim.problem.common.DefaultVehicle;
import rinde.sim.problem.common.DynamicPDPTWProblem;
import rinde.sim.problem.common.DynamicPDPTWProblem.Creator;
import rinde.sim.problem.common.DynamicPDPTWProblem.TimeOutHandler;
import rinde.sim.problem.common.ObjectiveFunction;
import rinde.sim.problem.common.ParcelDTO;
import rinde.sim.problem.common.StatsTracker;
import rinde.sim.problem.common.StatsTracker.StatisticsDTO;
import rinde.sim.problem.common.VehicleDTO;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Gendreau06Test {

	protected static final double EPSILON = 0.0001;

	@Test
	public void simpleScenario() throws IOException {
		final Gendreau06Scenario scenario = create(2, minutes(15), new AddParcelEvent(new ParcelDTO(new Point(2, 1),
				new Point(4, 1), new TimeWindow(0, 720000), new TimeWindow(5, 720000), 0, 0, 0, 0)));
		final StatisticsDTO dto = runProblem(scenario);

		// the second truck will turn around just one tick distance before
		// reaching the package. the reason is that it is too late since the
		// first truck will pickup the parcel.
		final double distInOneTick = 30.0 / 3600.0;
		assertEquals(9 - (2.0 * distInOneTick), dto.totalDistance, EPSILON);
		assertEquals(1, dto.totalParcels);
		assertEquals(0, dto.overTime);
		assertEquals(0, dto.pickupTardiness);
		assertEquals(0, dto.deliveryTardiness);
		assertEquals(2, dto.totalVehicles);
		assertEquals(2, dto.movedVehicles);
	}

	@Test
	public void overtimeScenario() {
		final Gendreau06Scenario scenario = create(1, minutes(6), new AddParcelEvent(new ParcelDTO(new Point(2, 1),
				new Point(4, 1), new TimeWindow(0, minutes(12)), new TimeWindow(5, minutes(12)), 0, 0, 0, 0)));
		final StatisticsDTO dto = runProblem(scenario);

		assertEquals(6, dto.totalDistance, EPSILON);
		assertEquals(1, dto.totalDeliveries);
		assertEquals(minutes(6) - 1000, dto.overTime);
		assertEquals(0, dto.pickupTardiness);
		assertEquals(0, dto.deliveryTardiness);
	}

	@Test
	public void tardinessScenario() {
		final Gendreau06Scenario scenario = create(1, minutes(12), /* */
				parcelEvent(2, 3, 2, 1, 0, seconds(15), 0, minutes(9)), /* */
				parcelEvent(3, 3, 3, 1, 0, minutes(3), 0, minutes(4)));
		final StatisticsDTO dto = runProblem(scenario);

		assertEquals(6, dto.totalDistance, EPSILON);
		assertEquals(2, dto.totalDeliveries);
		assertEquals(0, dto.overTime);
		assertEquals(seconds(45), dto.pickupTardiness);
		assertEquals(minutes(3), dto.deliveryTardiness);

	}

	static long minutes(long n) {
		return n * seconds(60);
	}

	static long seconds(long n) {
		return n * 1000;
	}

	static AddParcelEvent parcelEvent(double x1, double y1, double x2, double y2, long tw1b, long tw1e, long tw2b,
			long tw2e) {
		return new AddParcelEvent(new ParcelDTO(new Point(x1, y1), new Point(x2, y2), new TimeWindow(tw1b, tw1e),
				new TimeWindow(tw2b, tw2e), 0, 0, 0, 0));
	}

	static StatisticsDTO runProblem(Gendreau06Scenario s) {
		final DynamicPDPTWProblem problem = new DynamicPDPTWProblem(s, 123);
		problem.addCreator(AddVehicleEvent.class, new Creator<AddVehicleEvent>() {
			@Override
			public boolean create(Simulator sim, AddVehicleEvent event) {
				return sim.register(new SimpleTruck(event.vehicleDTO, new ClosestParcelStrategy()));
			}
		});
		final StatsTracker st = new StatsTracker();
		final ObjectiveFunction of = new Gendreau06ObjectiveFunction();

		// st.getEventAPI().addListener(new Listener() {
		// @Override
		// public void handleEvent(Event e) {
		// System.out.println(e);
		// }
		// });

		// problem.enableUI();

		problem.setTimeOutHandler(new TimeOutHandler() {
			@Override
			public void handleTimeOut(final Simulator sim) {
				if (of.isValidResult(st.getStatsDTO())) {
					sim.stop();
				} else {
					sim.addTickListener(new TickListener() {
						@Override
						public void tick(TimeLapse timeLapse) {}

						@Override
						public void afterTick(TimeLapse timeLapse) {
							if (of.isValidResult(st.getStatsDTO())) {
								sim.stop();
							}
						}
					});
				}
			}
		});
		problem.addStatisticsListener(st);
		problem.simulate();
		return st.getStatsDTO();
	}

	static Gendreau06Scenario create(int numVehicles, long endTime, AddParcelEvent... parcelEvents) {
		final List<TimedEvent> events = newArrayList();

		final Point depotPosition = new Point(2.0, 2.5);
		final double truckSpeed = 30;
		events.add(new AddDepotEvent(-1, depotPosition));
		for (int i = 0; i < numVehicles; i++) {
			events.add(new AddVehicleEvent(-1, new VehicleDTO(depotPosition, truckSpeed, 0, TimeWindow.ALWAYS)));
		}

		events.addAll(asList(parcelEvents));

		events.add(new TimedEvent(PDPScenarioEvent.TIME_OUT, endTime));

		final Set<Enum<?>> eventTypes = new HashSet<Enum<?>>(asList(PDPScenarioEvent.values()));
		return new Gendreau06Scenario(events, eventTypes);
	}

	static class SimpleTruck extends DefaultVehicle {
		protected VehicleStrategy strategy;

		public SimpleTruck(VehicleDTO dto, VehicleStrategy vs) {
			super(dto);
			strategy = vs;
		}

		@Override
		protected void tickImpl(TimeLapse time) {
			strategy.execute(time);
		}

		@Override
		public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
			super.initRoadPDP(pRoadModel, pPdpModel);
			strategy.init(this, pRoadModel, pPdpModel);
		}
	}

	interface VehicleStrategy {
		void init(DefaultVehicle vehicle, RoadModel rm, PDPModel pm);

		void execute(TimeLapse time);
	}

	static class ClosestParcelStrategy implements VehicleStrategy {

		protected DefaultVehicle vehicle;
		protected RoadModel roadModel;
		protected PDPModel pdpModel;
		protected Parcel target;

		public ClosestParcelStrategy() {}

		@Override
		public void init(DefaultVehicle v, RoadModel rm, PDPModel pm) {
			checkState(vehicle == null && roadModel == null && pdpModel == null, "init can be called only once!");
			vehicle = v;
			roadModel = rm;
			pdpModel = pm;
		}

		@Override
		public void execute(TimeLapse time) {
			// System.out.println(time);
			while (time.hasTimeLeft()) {

				final Set<Parcel> parcels = newHashSet(pdpModel.getParcels(ParcelState.AVAILABLE));
				if (!pdpModel.getContents(vehicle).isEmpty()) {
					parcels.addAll(pdpModel.getContents(vehicle));
				}

				double dist = Double.POSITIVE_INFINITY;
				Parcel closest = null;
				for (final Parcel p : parcels) {
					final Point pos = pdpModel.containerContains(vehicle, p) ? p.getDestination() : roadModel
							.getPosition(p);
					final double d = Point.distance(roadModel.getPosition(vehicle), pos);
					if (d < dist) {
						dist = d;
						closest = p;
					}
				}

				if (closest != null) {
					final boolean isDelivery = pdpModel.containerContains(vehicle, closest);
					final Point dest = isDelivery ? closest.getDestination() : roadModel.getPosition(closest);

					roadModel.moveTo(vehicle, dest, time);
					// System.out.println(Point.distance(dest,
					// roadModel.getPosition(vehicle)) + " " + time);
					if (roadModel.getPosition(vehicle).equals(dest)) {
						if (isDelivery) {
							// System.out.println("deliver " + time + " " +
							// time.getTime() + " "
							// + closest.getDeliveryTimeWindow());
							pdpModel.deliver(vehicle, closest, time);
						} else {
							// System.out.println("pickup " + time + " " +
							// time.getTime() + " "
							// + closest.getPickupTimeWindow());
							pdpModel.pickup(vehicle, closest, time);
						}
					}
				} else {
					roadModel.moveTo(vehicle, vehicle.getDTO().startPosition, time);
					// System.out.println(Point.distance(vehicle.getDTO().startPosition,
					// roadModel.getPosition(vehicle)));

					if (roadModel.getPosition(vehicle).equals(vehicle.getDTO().startPosition)) {
						// System.out.println("arrived at depot: " + time + " "
						// + time.getTime());
						time.consumeAll();
					}
				}
			}
		}
	}
}
