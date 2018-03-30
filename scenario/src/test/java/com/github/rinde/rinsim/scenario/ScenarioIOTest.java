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
package com.github.rinde.rinsim.scenario;

import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.RealtimeClockController.ClockMode;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.testutil.TestUtil;
import com.google.common.base.Suppliers;

/**
 *
 * @author Rinde van Lon
 */
public class ScenarioIOTest {

  /**
   * Tests unreachable code.
   */
  @BeforeClass
  public static void setUpClass() {
    TestUtil.testPrivateConstructor(ScenarioIO.class);
    TestUtil.testEnum(ScenarioIO.ClassIO.class);
    TestUtil.testEnum(ScenarioIO.EnumIO.class);
    TestUtil.testEnum(ScenarioIO.ImmutableListIO.class);
    TestUtil.testEnum(ScenarioIO.ImmutableSetIO.class);
    TestUtil.testEnum(ScenarioIO.MeasureIO.class);
    TestUtil.testEnum(ScenarioIO.ModelBuilderIO.class);
    TestUtil.testEnum(ScenarioIO.ParcelIO.class);
    TestUtil.testEnum(ScenarioIO.PredicateIO.class);
    TestUtil.testEnum(ScenarioIO.ProblemClassIO.class);
    TestUtil.testEnum(ScenarioIO.ScenarioObjIO.class);
    TestUtil.testEnum(ScenarioIO.StopConditionIO.class);
    TestUtil.testEnum(ScenarioIO.TimeWindowHierarchyIO.class);
    TestUtil.testEnum(ScenarioIO.UnitIO.class);
    TestUtil.testEnum(ScenarioIO.VehicleIO.class);
  }

  /**
   * Tests {@link ScenarioIO#readerAdapter(com.google.common.base.Function)}.
   * @throws IOException When IO fails.
   */
  @Test
  public void testReaderAdapter() throws IOException {
    final Scenario s = Scenario.builder()
      .addModel(TimeModel.builder().withTickLength(7L))
      .build();

    final Path tmpDir = Files.createTempDirectory("rinsim-scenario-io-test");
    final Path file = Paths.get(tmpDir.toString(), "test.scen");
    ScenarioIO.write(s, file);

    final Scenario out = ScenarioIO.reader().apply(file);
    final Scenario convertedOut =
      verifyNotNull(ScenarioIO.readerAdapter(ScenarioConverters.toRealtime())
        .apply(file));

    assertThat(s).isEqualTo(out);
    assertThat(s).isNotEqualTo(convertedOut);
    assertThat(convertedOut.getModelBuilders())
      .contains(TimeModel.builder()
        .withRealTime()
        .withStartInClockMode(ClockMode.SIMULATED)
        .withTickLength(7L));

    Files.delete(file);
    Files.delete(tmpDir);
  }

  /**
   * Tests correct serializing of GraphRoadModel with graph in a separate file.
   * @throws IOException If something goes wrong with the filesystem.
   */
  @Test
  public void testGraphRmbIO() throws IOException {
    final String ser =
      "{\"events\":[],\"modelBuilders\":[{\"class\":\"com.github.rinde.rinsim.core.model.time.AutoValue_TimeModel_Builder\",\"value\":{\"tickLength\":7,\"timeUnit\":\"ms\",\"provTypes\":[{\"class\":\"java.lang.Class\",\"value\":\"com.github.rinde.rinsim.core.model.time.Clock\"},{\"class\":\"java.lang.Class\",\"value\":\"com.github.rinde.rinsim.core.model.time.ClockController\"}],\"deps\":[],\"modelType\":\"com.github.rinde.rinsim.core.model.time.TimeModel\",\"associatedType\":\"com.github.rinde.rinsim.core.model.time.TickListener\"}},{\"class\":\"com.github.rinde.rinsim.core.model.road.AutoValue_RoadModelBuilders_StaticGraphRMB\",\"value\":{\"distanceUnit\":\"km\",\"speedUnit\":\"km/h\",\"graphSupplier\":{\"class\":\"com.github.rinde.rinsim.geom.io.AutoValue_DotGraphIO_LengthDataSup\",\"value\":{\"path\":\"tmp.json\"}},\"provTypes\":[{\"class\":\"java.lang.Class\",\"value\":\"com.github.rinde.rinsim.core.model.road.RoadModel\"},{\"class\":\"java.lang.Class\",\"value\":\"com.github.rinde.rinsim.core.model.road.GraphRoadModel\"}],\"deps\":[],\"modelType\":\"com.github.rinde.rinsim.core.model.road.GraphRoadModel\",\"associatedType\":\"com.github.rinde.rinsim.core.model.road.RoadUser\"}}],\"timeWindow\":\"0,28800000\",\"stopCondition\":{\"class\":\"com.github.rinde.rinsim.scenario.StopConditions$Default\",\"value\":\"ALWAYS_FALSE\"},\"problemClass\":{\"class\":\"com.github.rinde.rinsim.scenario.AutoValue_Scenario_SimpleProblemClass\",\"value\":{\"id\":\"DEFAULT\"}},\"problemInstanceId\":\"\"}";

    final Graph<LengthData> g = new TableGraph<>();
    g.addConnection(new Point(0, 0), new Point(1, 0));
    g.addConnection(new Point(1, 1), new Point(1, 0));

    final Path p = Paths.get("tmp.json");

    DotGraphIO.getLengthGraphIO().write(g, p);

    final Scenario s = Scenario.builder()
      .addModel(TimeModel.builder().withTickLength(7L))
      .addModel(RoadModelBuilders.staticGraph(
        DotGraphIO.getLengthDataGraphSupplier(p)))
      .build();

    final String serialized = ScenarioIO.write(s);
    assertThat(serialized).isEqualTo(ser);

    final Scenario deserialized = ScenarioIO.read(serialized);
    assertThat(s).isEqualTo(deserialized);

    final Simulator sim = Simulator.builder()
      .addModels(deserialized.getModelBuilders())
      .build();

    // check that the graph that is in the simulator equals the one we defined
    // above
    final Graph<?> graphInSim =
      sim.getModelProvider().getModel(GraphRoadModel.class).getGraph();
    assertThat(graphInSim).isEqualTo(g);

    Files.delete(p);
  }

  /**
   * Tests detection and correct error message.
   * @throws IOException If something goes wrong with the filesystem.
   */
  @Test
  public void testGraphRmbDirectIO() throws IOException {
    final Graph<LengthData> g = new TableGraph<>();
    g.addConnection(new Point(0, 0), new Point(1, 0));
    g.addConnection(new Point(1, 1), new Point(1, 0));

    final Scenario s = Scenario.builder()
      .addModel(TimeModel.builder().withTickLength(7L))
      .addModel(RoadModelBuilders.staticGraph(Suppliers.ofInstance(g)))
      .build();

    boolean fail = false;
    try {
      ScenarioIO.write(s);
    } catch (final IllegalArgumentException e) {
      fail = true;
      assertThat(e.getMessage())
        .isEqualTo("A graph cannot be serialized embedded in a scenario.");
    }
    assertThat(fail).isTrue();
  }
}
