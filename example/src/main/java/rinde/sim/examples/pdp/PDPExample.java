/**
 * 
 */
package rinde.sim.examples.pdp;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.GraphRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class PDPExample {

  private static final int NUM_DEPOTS = 5;
  private static final int NUM_TRUCKS = 10;
  private static final int NUM_PARCELS = 30;

  // time in ms
  private static final long SERVICE_DURATION = 60000;
  private static final double PARCEL_MAGNITUDE = 10d;
  private static final int TRUCK_CAPACITY = 10;
  private static final int DEPOT_CAPACITY = 100;

  private static final String MAP_FILE = "../core/files/maps/leuven-simple.dot";

  private PDPExample() {}

  public static void main(String[] args) throws FileNotFoundException,
      IOException {
    // create a new simulator, load map of Leuven
    final RandomGenerator rng = new MersenneTwister(123);
    final Simulator simulator = new Simulator(rng, Measure.valueOf(1000L,
        SI.MILLI(SI.SECOND)));
    final Graph<MultiAttributeData> graph = DotGraphSerializer
        .getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(MAP_FILE);
    final RoadModel roadModel = new GraphRoadModel(graph);
    final PDPModel pdpModel = new PDPModel();
    simulator.register(roadModel);
    simulator.register(pdpModel);
    simulator.configure();

    for (int i = 0; i < NUM_DEPOTS; i++) {
      simulator.register(new ExampleDepot(roadModel.getRandomPosition(rng),
          DEPOT_CAPACITY));
    }
    for (int i = 0; i < NUM_TRUCKS; i++) {
      simulator.register(new ExampleTruck(roadModel.getRandomPosition(rng),
          TRUCK_CAPACITY));
    }
    for (int i = 0; i < NUM_PARCELS; i++) {
      simulator.register(new ExampleParcel(roadModel.getRandomPosition(rng),
          roadModel.getRandomPosition(rng), SERVICE_DURATION, SERVICE_DURATION,
          PARCEL_MAGNITUDE));
    }

    final UiSchema uis = new UiSchema();
    uis.add(ExampleDepot.class, "/graphics/perspective/tall-building-64.png");
    uis.add(ExampleTruck.class, "/graphics/flat/taxi-32.png");
    uis.add(ExampleParcel.class, "/graphics/flat/hailing-cab-32.png");
    View.startGui(simulator, 1, new GraphRoadModelRenderer(),
        new RoadUserRenderer(uis, false));
  }

  static class ExampleDepot extends Depot {
    ExampleDepot(Point position, double capacity) {
      setStartPosition(position);
      setCapacity(capacity);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
  }

  static class ExampleParcel extends Parcel {
    ExampleParcel(Point startPosition, Point pDestination,
        long pLoadingDuration, long pUnloadingDuration, double pMagnitude) {
      super(pDestination, pLoadingDuration, TimeWindow.ALWAYS,
          pUnloadingDuration, TimeWindow.ALWAYS, pMagnitude);
      setStartPosition(startPosition);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
  }
}
