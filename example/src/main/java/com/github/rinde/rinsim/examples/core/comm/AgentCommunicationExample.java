/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
package com.github.rinde.rinsim.examples.core.comm;

import java.io.IOException;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.communication.CommunicationModel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.serializers.DotGraphSerializer;
import com.github.rinde.rinsim.serializers.SelfCycleFilter;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.ui.renderers.UiSchema;

/**
 * This example shows a possible use case of the {@link CommunicationModel}. It
 * is shown how this model can be used to let agents define several
 * communication properties:
 * <ul>
 * <li>Agent communication range</li>
 * <li>Agent communication reliability</li>
 * </ul>
 * Only agents which are both in each others range can communicate. When
 * communication is established a line will show between two agents. The
 * semi-transparent circle indicates the range, the color indicates the
 * reliability. The number under an agent indicates the number of messages that
 * have been received. Note that the messages that are sent contain no
 * information or purpose other than in this example.
 * <p>
 * If this class is run on MacOS it might be necessary to use
 * -XstartOnFirstThread as a VM argument.
 *
 * @author Bartosz Michalik
 * @author Rinde van Lon
 * @since 2.0
 */
public class AgentCommunicationExample {

  private static final String MAP_DIR = "/data/maps/leuven-simple.dot";
  private static final int NUM_AGENTS = 50;

  // vehicle speed
  private static final double MIN_SPEED = 50d;
  private static final double MAX_SPEED = 100d;

  // communication range
  private static final int MIN_RADIUS = 3000;
  private static final int MAX_RADIUS = 12000;

  // communication reliability
  private static final double MIN_RELIABILITY = .01;
  private static final double MAX_RELIABILITY = .6;

  private AgentCommunicationExample() {}

  /**
   * Run the example.
   * @param args This is ignored.
   */
  public static void main(String[] args) {
    run(false);
  }

  /**
   * Run the example.
   * @param testing if <code>true</code> turns on testing mode.
   */
  public static void run(boolean testing) {
    final MersenneTwister rand = new MersenneTwister(123);
    final Simulator simulator = new Simulator(rand, Measure.valueOf(1000L,
        SI.MILLI(SI.SECOND)));
    Graph<LengthData> graph;
    try {
      graph = DotGraphSerializer
          .getLengthGraphSerializer(new SelfCycleFilter()).read(
              AgentCommunicationExample.class.getResourceAsStream(MAP_DIR));
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }

    // create models
    final RoadModel roadModel = new GraphRoadModel(graph);
    final CommunicationModel communicationModel = new CommunicationModel(rand,
        false);
    simulator.register(roadModel);
    simulator.register(communicationModel);
    simulator.configure();

    // add agents
    for (int i = 0; i < NUM_AGENTS; i++) {
      final int radius = MIN_RADIUS + rand.nextInt(MAX_RADIUS - MIN_RADIUS);
      final double speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED)
          * rand.nextDouble();
      final double reliability = MIN_RELIABILITY
          + rand.nextDouble() * (MAX_RELIABILITY - MIN_RELIABILITY);

      final RandomWalkAgent agent = new RandomWalkAgent(speed, radius,
          reliability);
      simulator.register(agent);
    }

    // create GUI
    final UiSchema schema = new UiSchema(false);
    schema
    .add(ExamplePackage.class, "/graphics/perspective/deliverypackage2.png");

    final UiSchema schema2 = new UiSchema();
    schema2.add(Colors.BLACK.name(), Colors.BLACK.color());
    schema2.add(Colors.RED.name(), Colors.RED.color());
    schema2.add(Colors.GREEN.name(), Colors.GREEN.color());

    final View.Builder viewBuilder = View.create(simulator)
        .with(GraphRoadModelRenderer.builder())
        .with(new RoadUserRenderer(schema, false))
        .with(new MessagingLayerRenderer(roadModel, schema2))
        .setSpeedUp(4);

    if (testing) {
      viewBuilder.enableAutoPlay()
      .enableAutoClose()
      .setSpeedUp(64)
      .stopSimulatorAtTime(60 * 60 * 1000);
    }

    viewBuilder.show();
  }

  enum Colors {
    BLACK(new RGB(0x0, 0x0, 0x0)),
    RED(new RGB(0xFF, 0x0, 0x0)),
    GREEN(new RGB(0x0, 0x80, 0x0));

    private final RGB color;

    Colors(RGB c) {
      color = c;
    }

    public RGB color() {
      return color;
    }
  }
}
