package rinde.sim.examples.rwalk5;

import java.util.Random;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.core.model.communication.CommunicationModel2;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.scenario.ConfigurationException;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioController;
import rinde.sim.scenario.ScenarioController.UICreator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.scenario.TimedEventHandler;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.GraphRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

public class SimpleController {

  private final StatisticsCollector statistics = new StatisticsCollector();

  private final RoadModel roadModel;
  private Random randomizer;

  protected final Simulator simulator;

  /**
   * Simple controller
   * @param scen scenario to realize
   * @param map name of the file with a map
   * @throws ConfigurationException
   */
  public SimpleController(Scenario scen, String map)
      throws ConfigurationException {
    Graph<MultiAttributeData> graph;
    try {
      graph = DotGraphSerializer
          .getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(map);
    } catch (final Exception e) {
      // not the smartest error handling
      throw new ConfigurationException("e:", e);
    }
    roadModel = new GraphRoadModel(graph);

    final MersenneTwister rand = new MersenneTwister(123);
    simulator = new Simulator(rand, Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));
    final CommunicationModel communicationModel = new CommunicationModel2(rand);
    simulator.register(roadModel);
    simulator.register(communicationModel);

    final TimedEventHandler teh = new TimedEventHandler() {
      @Override
      public boolean handleTimedEvent(TimedEvent event) {
        if (event.getEventType() == ScenarioEvent.ADD_TRUCK) {
          final int radius = rand.nextInt(300) + 200;

          final double minSpeed = 30;
          final double maxSpeed = 140;
          final RandomWalkAgent agent = new RandomWalkAgent(minSpeed
              + (maxSpeed - minSpeed) * rand.nextDouble(), radius,
              0.01 + rand.nextDouble() / 2);
          simulator.register(agent);
          agent.getEventAPI()
              .addListener(statistics, RandomWalkAgent.Type.FINISHED_SERVICE);
          // it is important to inform controller that this event was
          // handled
          // to avoid runtime exceptions
          return true;
        }
        // likewise, it is important to inform the controller that other
        // events are *not* handled, this ensures fail-fast behavior in
        // case the
        // scenario contains events which were not anticipated.
        return false;
      }
    };

    final ScenarioController scenarioController = new ScenarioController(scen,
        simulator, teh, -1);
    scenarioController.enableUI(new UICreator() {
      @Override
      public void createUI(Simulator sim) {
        final UiSchema schema = new UiSchema(false);
        schema.add(rinde.sim.examples.common.Package.class, new RGB(0x0, 0x0,
            0xFF));

        final UiSchema schema2 = new UiSchema();
        schema2.add(RandomWalkAgent.C_BLACK, new RGB(0, 0, 0));
        schema2.add(RandomWalkAgent.C_YELLOW, new RGB(0xff, 0, 0));
        schema2.add(RandomWalkAgent.C_GREEN, new RGB(0x0, 0x80, 0));
        View.startGui(sim, 4, new GraphRoadModelRenderer(), new RoadUserRenderer(
            schema, false), new MessagingLayerRenderer(schema2));
      }
    });

    scenarioController.start();
  }

}
