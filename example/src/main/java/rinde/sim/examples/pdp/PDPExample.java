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
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
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
public final class PDPExample {

  private static final int NUM_DEPOTS = 0;
  private static final int NUM_TRUCKS = 10;
  private static final int NUM_PARCELS = 60;

  // time in ms
  private static final long SERVICE_DURATION = 60000;
  private static final double PARCEL_MAGNITUDE = 1d;
  private static final int TRUCK_CAPACITY = 10;
  private static final int DEPOT_CAPACITY = 100;

  // private static final String MAP_FILE =
  // "../core/files/maps/leuven-simple.dot";
  private static final String MAP_FILE = "../core/dot-files/leuven-simple.dot";// /Users/rindevanlon/Downloads/brussels-simple.dot";

  static final Graph<MultiAttributeData> graph = load();

  static Graph<MultiAttributeData> load() {
    try {
      return DotGraphSerializer.getMultiAttributeGraphSerializer(
          new SelfCycleFilter()).read(MAP_FILE);
    } catch (final FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private PDPExample() {}

  /**
   * Starts the {@link PDPExample}.
   * @param args
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static void main(String[] args) throws FileNotFoundException,
      IOException {

    final long endTime = args != null && args.length >= 1 ? Long
        .parseLong(args[0]) : Long.MAX_VALUE;

    // create a new simulator, load map of Leuven
    final RandomGenerator rng = new MersenneTwister(123);
    final Simulator simulator = new Simulator(rng, Measure.valueOf(1000L,
        SI.MILLI(SI.SECOND)));

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

    simulator.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse time) {
        if (time.getStartTime() > endTime) {
          simulator.stop();
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    final UiSchema uis = new UiSchema();
    uis.add(ExampleDepot.class, "/graphics/perspective/tall-building-64.png");
    uis.add(ExampleTruck.class, "/graphics/flat/taxi-32.png");
    uis.add(ExampleParcel.class, "/graphics/flat/person-red-32.png");
    View.create(simulator)
        .with(new GraphRoadModelRenderer(), new RoadUserRenderer(uis, false))
        .enableAutoClose().enableAutoPlay().setFullScreen().show();
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
