/**
 * 
 */
package rinde.sim.examples.factory;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.LengthData;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.GraphRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class FactoryExample {

  /**
   * @param args
   */
  public static void main(String[] args) {

    final Graph<LengthData> graph = new MultimapGraph<LengthData>();

    Graphs.addBiPath(graph, new Point(1, 1), new Point(1, 10),
        new Point(5, 10), new Point(10, 10), new Point(10, 8),
        new Point(10, 1), new Point(5, 1), new Point(1, 1));

    Graphs.addBiPath(graph, new Point(5, 10), new Point(5, 6), new Point(4, 6),
        new Point(4, 8), new Point(5, 8), new Point(5, 1), new Point(5, 0.5),
        new Point(12, 0.5), new Point(12, 1), new Point(12, 2), new Point(11.5,
            2), new Point(11.5, 4), new Point(12, 4), new Point(12, 8),
        new Point(10, 8), new Point(5, 8));

    Graphs.addBiPath(graph, new Point(10, 1), new Point(12, 1));
    Graphs.addBiPath(graph, new Point(12, 2), new Point(12, 4));

    final RandomGenerator rng = new MersenneTwister(123);
    final Simulator simulator = new Simulator(rng, Measure.valueOf(1000L,
        SI.MILLI(SI.SECOND)));
    final RoadModel roadModel = new GraphRoadModel(graph);
    final PDPModel pdpModel = new PDPModel();
    simulator.register(roadModel);
    simulator.register(pdpModel);
    simulator.configure();

    final UiSchema uis = new UiSchema();
    // uis.add(ExampleDepot.class,
    // "/graphics/perspective/tall-building-64.png");
    // uis.add(ExampleTruck.class, "/graphics/flat/taxi-32.png");
    // uis.add(ExampleParcel.class, "/graphics/flat/hailing-cab-32.png");
    View.startGui(simulator, 1, new GraphRoadModelRenderer(1, true),
        new RoadUserRenderer(uis, false));
  }
}
