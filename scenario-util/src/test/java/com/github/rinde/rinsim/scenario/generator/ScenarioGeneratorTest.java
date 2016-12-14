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
package com.github.rinde.rinsim.scenario.generator;

import static org.junit.Assert.assertEquals;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.pdptw.common.PDPDynamicGraphRoadModel;

public class ScenarioGeneratorTest {

  @Before
  public void setUp() throws Exception {}

  @Test
  public void createPDPScenarioGeneratorTest() {

    final ModelBuilder<? extends RoadModel, ? extends RoadUser> roadModelBuilder =
      RoadModelBuilders
        .dynamicGraph(ListenableGraph.supplier(TableGraph.supplier()))
        .withSpeedUnit(NonSI.KILOMETERS_PER_HOUR)
        .withDistanceUnit(SI.METER);

    final ScenarioGenerator sg = ScenarioGenerator.builder().addModel(
      PDPDynamicGraphRoadModel.builderForDynamicGraphRm(
        (ModelBuilder<? extends DynamicGraphRoadModel, ? extends RoadUser>) roadModelBuilder)
        .withAllowVehicleDiversion(true))
      .build();

    assertEquals(SI.METER, sg.getDistanceUnit());
    assertEquals(NonSI.KILOMETERS_PER_HOUR, sg.getSpeedUnit());
  }

  @Test
  public void createGraphScenarioGeneratorTest() {
    final ModelBuilder<? extends RoadModel, ? extends RoadUser> roadModelBuilder =
      RoadModelBuilders
        .dynamicGraph(ListenableGraph.supplier(TableGraph.supplier()))
        .withSpeedUnit(NonSI.KILOMETERS_PER_HOUR)
        .withDistanceUnit(SI.METER);

    final ScenarioGenerator sg = ScenarioGenerator.builder().addModel(
      roadModelBuilder)
      .build();

    assertEquals(SI.METER, sg.getDistanceUnit());
    assertEquals(NonSI.KILOMETERS_PER_HOUR, sg.getSpeedUnit());
  }

  @Test
  public void createPlaneScenarioGeneratorTest() {
    final ModelBuilder<? extends RoadModel, ? extends RoadUser> roadModelBuilder =
      RoadModelBuilders
        .plane()
        .withSpeedUnit(NonSI.KILOMETERS_PER_HOUR)
        .withDistanceUnit(SI.METER);

    final ScenarioGenerator sg = ScenarioGenerator.builder().addModel(
      roadModelBuilder)
      .build();

    assertEquals(SI.METER, sg.getDistanceUnit());
    assertEquals(NonSI.KILOMETERS_PER_HOUR, sg.getSpeedUnit());
  }
}
