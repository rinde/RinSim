package com.github.rinde.rinsim.scenario;

import java.io.Serializable;
import java.math.RoundingMode;
import java.util.Collections;

import javax.measure.Measure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Predicate;
import com.google.common.math.DoubleMath;

public class ScenarioTestUtil {

	public static Scenario create(long seed) {
		final int endTime = 3 * 60 * 60 * 1000;
		Scenario.Builder b = Scenario
				.builder()
				.addModel(
						PlaneRoadModel.supplier(new Point(0, 0), new Point(10,
								10), SI.KILOMETER, Measure.valueOf(50d,
								NonSI.KILOMETERS_PER_HOUR)))
				.addModel(DefaultPDPModel.supplier(TimeWindowPolicies.LIBERAL))
				.addEvents(
						Collections
								.nCopies(
										10,
										new AddVehicleEvent(-1, VehicleDTO
												.builder()
												.startPosition(new Point(5, 5))
												.build())));

		RandomGenerator rng = new MersenneTwister(seed);
		for (int i = 0; i < 20; i++) {
			long announceTime = (long) rng.nextInt(DoubleMath.roundToInt(
					endTime * .8, RoundingMode.FLOOR));
			b.addEvent(new AddParcelEvent(ParcelDTO
					.builder(
							new Point(rng.nextDouble() * 10,
									rng.nextDouble() * 10),
							new Point(rng.nextDouble() * 10,
									rng.nextDouble() * 10))
					.orderAnnounceTime(announceTime)
					.pickupTimeWindow(new TimeWindow(announceTime, endTime))
					.deliveryTimeWindow(new TimeWindow(announceTime, endTime))
					.neededCapacity(0).build()));
		}

		b.addEvent(new TimedEvent(PDPScenarioEvent.TIME_OUT, endTime))
				.scenarioLength(endTime)
				.stopCondition(new EndTimeStopCondition(endTime));

		b.addEventType(PDPScenarioEvent.ADD_DEPOT);

		return b.build();
	}

	static class EndTimeStopCondition implements Predicate<Simulator>,
			Serializable {
		private final long endTime;
		public EndTimeStopCondition(long time){
			endTime  = time;
		}
		
		@Override
		public boolean apply(Simulator simulator) {
			return simulator.getCurrentTime() >= endTime;
		}
	}
}
