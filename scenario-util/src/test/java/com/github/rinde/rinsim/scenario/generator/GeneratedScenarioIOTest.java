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
package com.github.rinde.rinsim.scenario.generator;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy.TimeWindowPolicies;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.StatsStopConditions;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.scenario.ScenarioIO;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.testutil.TestUtil;

/**
 * @author Rinde van Lon
 *
 */
public class GeneratedScenarioIOTest {

  private static final String SERIALIZED_SCENARIO =
    "{\"events\":[{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddDepotEvent\",\"value\":{\"time\":-1,\"position\":\"2.5,2.5\"}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddVehicleEvent\",\"value\":{\"time\":-1,\"vehicleDTO\":[\"0,14400000\",1,50.0,\"2.5,2.5\"]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddVehicleEvent\",\"value\":{\"time\":-1,\"vehicleDTO\":[\"0,14400000\",1,50.0,\"2.5,2.5\"]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddVehicleEvent\",\"value\":{\"time\":-1,\"vehicleDTO\":[\"0,14400000\",1,50.0,\"2.5,2.5\"]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddVehicleEvent\",\"value\":{\"time\":-1,\"vehicleDTO\":[\"0,14400000\",1,50.0,\"2.5,2.5\"]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddVehicleEvent\",\"value\":{\"time\":-1,\"vehicleDTO\":[\"0,14400000\",1,50.0,\"2.5,2.5\"]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddVehicleEvent\",\"value\":{\"time\":-1,\"vehicleDTO\":[\"0,14400000\",1,50.0,\"2.5,2.5\"]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddVehicleEvent\",\"value\":{\"time\":-1,\"vehicleDTO\":[\"0,14400000\",1,50.0,\"2.5,2.5\"]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddVehicleEvent\",\"value\":{\"time\":-1,\"vehicleDTO\":[\"0,14400000\",1,50.0,\"2.5,2.5\"]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddVehicleEvent\",\"value\":{\"time\":-1,\"vehicleDTO\":[\"0,14400000\",1,50.0,\"2.5,2.5\"]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddVehicleEvent\",\"value\":{\"time\":-1,\"vehicleDTO\":[\"0,14400000\",1,50.0,\"2.5,2.5\"]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddParcelEvent\",\"value\":{\"time\":1538241,\"parcelDTO\":[\"3.426286595644047,2.545578407820278\",\"4.534410727381912,4.62680166790263\",\"2738241,3338241\",\"3208005,3808005\",0.0,1538241,300000,300000]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddParcelEvent\",\"value\":{\"time\":2701757,\"parcelDTO\":[\"0.19068361667614275,3.41383138534056\",\"2.454164007028191,0.3612771340631282\",\"3901757,4501757\",\"4475370,5075370\",0.0,2701757,300000,300000]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddParcelEvent\",\"value\":{\"time\":5818552,\"parcelDTO\":[\"0.7720473375003711,0.9358782332380478\",\"3.7180552420229462,0.25487678656078816\",\"7018552,7618552\",\"7536257,8136257\",0.0,5818552,300000,300000]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddParcelEvent\",\"value\":{\"time\":6646867,\"parcelDTO\":[\"1.0945236192912489,1.798721928795054\",\"2.097162482429159,0.24470207886175377\",\"7846867,8446867\",\"8280023,8880023\",0.0,6646867,300000,300000]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddParcelEvent\",\"value\":{\"time\":9449656,\"parcelDTO\":[\"1.1755562146644183,2.9552663990987704\",\"2.9181223796465803,2.8072937805009133\",\"10649656,11249656\",\"11075572,11675572\",0.0,9449656,300000,300000]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddParcelEvent\",\"value\":{\"time\":11603342,\"parcelDTO\":[\"1.5804984243511189,0.6894840545725611\",\"0.160828554032304,1.1157646936756083\",\"12803342,13403342\",\"13210066,13810066\",0.0,11603342,300000,300000]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddParcelEvent\",\"value\":{\"time\":11805579,\"parcelDTO\":[\"2.023372802354988,4.939284531437204\",\"2.587833126953616,3.817871192636646\",\"13005579,13605579\",\"13395972,13995972\",0.0,11805579,300000,300000]}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_AddParcelEvent\",\"value\":{\"time\":13598242,\"parcelDTO\":[\"0.43087776072615824,4.651222229955684\",\"0.8078058253530007,2.9661804325516905\",\"13598242,13598242\",\"14022563,14022563\",0.0,13598242,300000,300000]}},{\"class\":\"com.github.rinde.rinsim.scenario.AutoValue_TimeOutEvent\",\"value\":{\"time\":14400000}}],\"modelBuilders\":[{\"class\":\"com.github.rinde.rinsim.pdptw.common.AutoValue_PDPRoadModel_Builder\",\"value\":{\"delegateModelBuilder\":{\"class\":\"com.github.rinde.rinsim.core.model.road.AutoValue_RoadModelBuilders_PlaneRMB\",\"value\":{\"distanceUnit\":\"km\",\"speedUnit\":\"km/h\",\"min\":\"0.0,0.0\",\"max\":\"10.0,10.0\",\"maxSpeed\":50.0,\"provTypes\":[{\"class\":\"java.lang.Class\",\"value\":\"com.github.rinde.rinsim.core.model.road.RoadModel\"},{\"class\":\"java.lang.Class\",\"value\":\"com.github.rinde.rinsim.core.model.road.PlaneRoadModel\"}],\"deps\":[],\"modelType\":\"com.github.rinde.rinsim.core.model.road.PlaneRoadModel\",\"associatedType\":\"com.github.rinde.rinsim.core.model.road.RoadUser\"}},\"allowVehicleDiversion\":true,\"createGraphRM\":false,\"provTypes\":[{\"class\":\"java.lang.Class\",\"value\":\"com.github.rinde.rinsim.core.model.road.RoadModel\"},{\"class\":\"java.lang.Class\",\"value\":\"com.github.rinde.rinsim.pdptw.common.PDPRoadModel\"}],\"deps\":[],\"modelType\":\"com.github.rinde.rinsim.pdptw.common.PDPRoadModel\",\"associatedType\":\"com.github.rinde.rinsim.core.model.road.RoadUser\"}},{\"class\":\"com.github.rinde.rinsim.core.model.pdp.AutoValue_DefaultPDPModel_Builder\",\"value\":{\"policy\":{\"class\":\"com.github.rinde.rinsim.core.model.pdp.TimeWindowPolicy$TimeWindowPolicies\",\"value\":\"TARDY_ALLOWED\"},\"provTypes\":[{\"class\":\"java.lang.Class\",\"value\":\"com.github.rinde.rinsim.core.model.pdp.PDPModel\"}],\"deps\":[{\"class\":\"java.lang.Class\",\"value\":\"com.github.rinde.rinsim.core.model.road.RoadModel\"}],\"modelType\":\"com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel\",\"associatedType\":\"com.github.rinde.rinsim.core.model.pdp.PDPObject\"}}],\"timeWindow\":\"0,14400000\",\"stopCondition\":{\"class\":\"com.github.rinde.rinsim.scenario.AutoValue_StopConditions_And\",\"value\":{\"getTypes\":[{\"class\":\"java.lang.Class\",\"value\":\"com.github.rinde.rinsim.pdptw.common.StatisticsProvider\"}],\"stopConditions\":[{\"class\":\"com.github.rinde.rinsim.pdptw.common.StatsStopConditions$Instances$3\",\"value\":{\"class\":\"com.github.rinde.rinsim.pdptw.common.StatsStopConditions$Instances\",\"value\":\"ANY_TARDINESS\"}},{\"class\":\"com.github.rinde.rinsim.pdptw.common.StatsStopConditions$Instances$1\",\"value\":{\"class\":\"com.github.rinde.rinsim.pdptw.common.StatsStopConditions$Instances\",\"value\":\"TIME_OUT_EVENT\"}}]}},\"problemClass\":{\"class\":\"com.github.rinde.rinsim.scenario.generator.GeneratedScenarioIOTest$TestPC\",\"value\":\"CLASS_A\"},\"problemInstanceId\":\"id123\"}";

  enum TestPC implements ProblemClass {
    CLASS_A;

    @Override
    public String getId() {
      return name();
    }
  }

  /**
   * Test reading and writing a generated scenario.
   * @throws IOException when something IO related went wrong.
   */
  @Test
  public void testIO() throws IOException {
    TestUtil.testPrivateConstructor(Vehicles.class);
    TestUtil.testPrivateConstructor(Depots.class);
    final ScenarioGenerator generator = ScenarioGenerator
      .builder(TestPC.CLASS_A)
      .scenarioLength(4 * 60 * 60 * 1000L)
      .setStopCondition(
        StopConditions.and(
          StatsStopConditions.anyTardiness(),
          StatsStopConditions.timeOutEvent()))
      .parcels(
        Parcels
          .builder()
          .announceTimes(
            TimeSeries.homogenousPoisson(4 * 60 * 60 * 1000L, 10))
          .locations(Locations.builder().square(5).buildUniform())
          .timeWindows(TimeWindows.builder().build())
          .build())
      // .deliveryDurations(constant(10L))
      .addModel(
        PDPRoadModel.builder(
          RoadModelBuilders.plane()
            .withMaxSpeed(50d))
          .withAllowVehicleDiversion(true))
      .addModel(
        DefaultPDPModel.builder()
          .withTimeWindowPolicy(TimeWindowPolicies.TARDY_ALLOWED))
      .build();

    final Scenario scenario = generator
      .generate(new MersenneTwister(123), "id123");

    // if this call fails, something has changed in the scenario format.
    final Scenario originalScenario = ScenarioIO.read(SERIALIZED_SCENARIO);
    assertEquals("Change in scenario format detected.", originalScenario,
      scenario);

    final String output = ScenarioIO.write(scenario);
    final Scenario converted = ScenarioIO.read(output);

    assertEquals(scenario, converted);
  }
}
