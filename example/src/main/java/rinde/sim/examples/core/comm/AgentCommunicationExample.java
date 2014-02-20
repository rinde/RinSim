/**
 * 
 */
package rinde.sim.examples.core.comm;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.LengthData;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.GraphRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

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
 * 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
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

  public static void main(String[] args) throws Exception {
    final MersenneTwister rand = new MersenneTwister(123);
    final Simulator simulator = new Simulator(rand, Measure.valueOf(1000L,
        SI.MILLI(SI.SECOND)));
    final Graph<LengthData> graph = DotGraphSerializer
        .getLengthGraphSerializer(new SelfCycleFilter()).read(
            AgentCommunicationExample.class.getResourceAsStream(MAP_DIR));

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
          + (rand.nextDouble() * (MAX_RELIABILITY - MIN_RELIABILITY));

      final RandomWalkAgent agent = new RandomWalkAgent(speed, radius,
          reliability);
      simulator.register(agent);
    }

    // create GUI
    final UiSchema schema = new UiSchema(false);
    schema
        .add(ExamplePackage.class, "/graphics/perspective/deliverypackage2.png");

    final UiSchema schema2 = new UiSchema();
    schema2.add(RandomWalkAgent.C_BLACK, new RGB(0, 0, 0));
    schema2.add(RandomWalkAgent.C_YELLOW, new RGB(0xff, 0, 0));
    schema2.add(RandomWalkAgent.C_GREEN, new RGB(0x0, 0x80, 0));

    View.create(simulator)
        .with(new GraphRoadModelRenderer())
        .with(new RoadUserRenderer(schema, false))
        .with(new MessagingLayerRenderer(roadModel, schema2))
        .setSpeedUp(4)
        .show();
  }
}
