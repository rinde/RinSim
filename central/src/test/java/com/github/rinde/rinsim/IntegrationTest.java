/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim;
/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;
import org.junit.Test;

import com.github.rinde.rinsim.central.Central;
import com.github.rinde.rinsim.central.RandomSolver;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.PostProcessor.FailureStrategy;
import com.github.rinde.rinsim.experiment.PostProcessors;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatsStopConditions;
import com.github.rinde.rinsim.pdptw.common.TimeLinePanel;
import com.github.rinde.rinsim.scenario.IScenario;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.gendreau06.Gendreau06ObjectiveFunction;
import com.github.rinde.rinsim.scenario.generator.IntensityFunctions;
import com.github.rinde.rinsim.scenario.generator.Locations;
import com.github.rinde.rinsim.scenario.generator.Parcels;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;
import com.github.rinde.rinsim.scenario.generator.TimeSeries;
import com.github.rinde.rinsim.scenario.generator.Vehicles;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 *
 * @author Rinde van Lon
 */
public class IntegrationTest {
  final long scenarioLengthMs = 4 * 60 * 60 * 1000L;
  final double vehicleSpeedKmh = 33d;

  @Test
  public void scenarioGeneration() {
    final ScenarioGenerator scenGen = ScenarioGenerator.builder()
      .parcels(Parcels.builder()
        .announceTimes(
          TimeSeries.nonHomogenousPoisson(scenarioLengthMs - 30 * 60 * 1000L,
            IntensityFunctions.sineIntensity()
              .period(30 * 60 * 1000L)
              .area(10)
              .build()))
        .locations(Locations.builder()
          .min(new Point(0, 0))
          .max(new Point(10, 10))
          .mean(new Point(2, 3))
          .std(3)
          .redrawWhenOutOfBounds()
          .buildNormal())
        .build())
      .vehicles(
        Vehicles.homogenous(
          VehicleDTO.builder()
            .speed(vehicleSpeedKmh)
            .build(),
          12))
      .scenarioLength(scenarioLengthMs)
      .setStopCondition(StatsStopConditions.timeOutEvent())
      .addModel(PDPRoadModel.builder(RoadModelBuilders.plane()))
      .addModel(DefaultPDPModel.builder())
      .build();

    final IScenario s = scenGen.generate(new MersenneTwister(123L), "test");
    final String generatedJson = ScenarioIO.write(s);

    try {
      final String jsonOnDisk =
        Resources.toString(Resources.getResource("integration-scenario.json"),
          Charsets.UTF_8);
      assertThat(generatedJson).isEqualTo(jsonOnDisk);
    } catch (final IOException e) {
      throw new IllegalStateException();
    }
  }

  @Test
  public void results() {
    final Scenario scenario =
      loadScenarioFromResource("integration-scenario.json");

    final ExperimentResults er = Experiment.builder()
      .addScenario(scenario)
      .addConfiguration(Central.solverConfiguration(RandomSolver.supplier()))
      .withThreads(1)
      .usePostProcessor(
        PostProcessors.statisticsPostProcessor(
          Gendreau06ObjectiveFunction.instance(vehicleSpeedKmh),
          FailureStrategy.INCLUDE))
      .showGui(View.builder()
        .with(PlaneRoadModelRenderer.builder())
        .with(TimeLinePanel.builder())
        .with(
          RoadUserRenderer.builder()
            .withColorAssociation(Vehicle.class, new RGB(0, 0, 255))
            .withColorAssociation(Parcel.class, new RGB(0, 255, 255))
            .withColorAssociation(Depot.class, new RGB(255, 0, 255))))
      .showGui(false)
      .perform();

    final SimulationResult sr = er.getResults().iterator().next();

    final StatisticsDTO stats = (StatisticsDTO) sr.getResultObject();
    assertThat(stats.timeUnit).isEqualTo(SI.MILLI(SI.SECOND));
    assertThat(stats.distanceUnit).isEqualTo(SI.KILOMETER);
    assertThat(stats.speedUnit).isEqualTo(NonSI.KILOMETERS_PER_HOUR);

    // 1.0 = 1 km
    // 0.001 = 1 m
    // 0.00001 = 1 cm
    // 0.000001 = 1 mm
    assertThat(stats.totalDistance).isWithin(0.000001).of(600.5151350163192);
    assertThat(stats.totalPickups).isEqualTo(74);
    assertThat(stats.totalDeliveries).isEqualTo(69);
    assertThat(stats.totalParcels).isEqualTo(78);
    assertThat(stats.acceptedParcels).isEqualTo(78);
    assertThat(stats.pickupTardiness).isEqualTo(22236719);
    assertThat(stats.deliveryTardiness).isEqualTo(51806677);
    assertThat(stats.simulationTime).isEqualTo(14401000);
    assertThat(stats.simFinish).isTrue();
    assertThat(stats.vehiclesAtDepot).isEqualTo(0);
    assertThat(stats.overTime).isEqualTo(0);
    assertThat(stats.totalVehicles).isEqualTo(12);
    assertThat(stats.movedVehicles).isEqualTo(12);
  }

  static Scenario loadScenarioFromResource(String resource) {
    try {
      final Scenario scen =
        ScenarioIO.read(
          Paths.get(
            Resources.getResource(resource).toURI()));
      return scen;
    } catch (final IOException | URISyntaxException e) {
      throw new IllegalArgumentException("Couldn't find " + resource, e);
    }
  }
}
