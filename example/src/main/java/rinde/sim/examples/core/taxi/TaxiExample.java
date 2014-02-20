/**
 * 
 */
package rinde.sim.examples.core.taxi;

import static com.google.common.collect.Maps.newHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.MultiAttributeData;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.DefaultPDPModel;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.event.Listener;
import rinde.sim.examples.core.taxi.TaxiRenderer.Language;
import rinde.sim.serializers.DotGraphSerializer;
import rinde.sim.serializers.SelfCycleFilter;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.GraphRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;
import rinde.sim.util.TimeWindow;

/**
 * Example showing a fleet of taxis that have to pickup and transport customers
 * around the city of Leuven.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class TaxiExample {

  private static final int NUM_DEPOTS = 1;
  private static final int NUM_TAXIS = 20;
  private static final int NUM_CUSTOMERS = 30;

  // time in ms
  private static final long SERVICE_DURATION = 60000;
  private static final int TAXI_CAPACITY = 10;
  private static final int DEPOT_CAPACITY = 100;

  private static final String MAP_FILE = "/data/maps/leuven-simple.dot";
  private static final Map<String, Graph<?>> GRAPH_CACHE = newHashMap();

  static Graph<MultiAttributeData> load(String name) {
    try {
      return DotGraphSerializer.getMultiAttributeGraphSerializer(
          new SelfCycleFilter()).read(
          TaxiExample.class.getResourceAsStream(name));
    } catch (final FileNotFoundException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private TaxiExample() {}

  /**
   * Starts the {@link TaxiExample}.
   * @param args
   */
  public static void main(String[] args) {
    final long endTime = args != null && args.length >= 1 ? Long
        .parseLong(args[0]) : Long.MAX_VALUE;

    final String graphFile = args != null && args.length >= 2 ? args[1]
        : MAP_FILE;
    run(endTime, graphFile, null /* new Display() */, null, null);
    // System.exit(0);
  }

  public static Simulator run(final long endTime, String graphFile,
      @Nullable Display display, @Nullable Monitor m, @Nullable Listener list) {

    final Display d = Display.getDefault();
    final Rectangle rect;
    if (m != null) {
      rect = m.getClientArea();
    } else {
      rect = d.getPrimaryMonitor().getClientArea();
    }

    Graph<?> g;
    if (GRAPH_CACHE.containsKey(graphFile)) {
      g = GRAPH_CACHE.get(graphFile);
    } else {
      g = load(graphFile);
      GRAPH_CACHE.put(graphFile, g);
    }

    // create a new simulator
    final RandomGenerator rng = new MersenneTwister(123);
    final Simulator simulator = new Simulator(rng, Measure.valueOf(1000L,
        SI.MILLI(SI.SECOND)));

    // use map of leuven
    final RoadModel roadModel = new GraphRoadModel(g);
    final DefaultPDPModel pdpModel = new DefaultPDPModel();

    // configure simulator with models
    simulator.register(roadModel);
    simulator.register(pdpModel);
    simulator.configure();

    // add depots, taxis and parcels to simulator
    for (int i = 0; i < NUM_DEPOTS; i++) {
      simulator.register(new TaxiBase(roadModel.getRandomPosition(rng),
          DEPOT_CAPACITY));
    }
    for (int i = 0; i < NUM_TAXIS; i++) {
      simulator.register(new Taxi(roadModel.getRandomPosition(rng),
          TAXI_CAPACITY));
    }
    for (int i = 0; i < NUM_CUSTOMERS; i++) {
      simulator.register(new Customer(roadModel.getRandomPosition(rng),
          roadModel.getRandomPosition(rng), SERVICE_DURATION, SERVICE_DURATION,
          1 + rng.nextInt(3)));
    }

    simulator.addTickListener(new TickListener() {
      @Override
      public void tick(TimeLapse time) {
        if (time.getStartTime() > endTime) {
          simulator.stop();
        } else if (rng.nextDouble() < .007) {
          simulator.register(new Customer(
              roadModel.getRandomPosition(rng), roadModel
                  .getRandomPosition(rng), SERVICE_DURATION, SERVICE_DURATION,
              1 + rng.nextInt(3)));
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });

    final UiSchema uis = new UiSchema();
    uis.add(TaxiBase.class, "/graphics/perspective/tall-building-64.png");
    uis.add(Taxi.class, "/graphics/flat/taxi-32.png");
    uis.add(Customer.class, "/graphics/flat/person-red-32.png");
    final View.Builder view = View.create(simulator)
        .with(new GraphRoadModelRenderer())
        .with(new RoadUserRenderer(uis, false))
        .with(new TaxiRenderer(Language.ENGLISH))
        .setTitleAppendix("Taxi Demo");

    if (m != null && list != null) {
      view.displayOnMonitor(m)
          .setSpeedUp(4)
          .setResolution(m.getClientArea().width, m.getClientArea().height)
          .setDisplay(display)
          .setCallback(list)
          .setAsync()
          .enableAutoPlay()
          .enableAutoClose();
    }

    view.show();
    return simulator;
  }

  /**
   * A customer with very permissive time windows.
   */
  static class Customer extends Parcel {
    Customer(Point startPosition, Point pDestination,
        long pLoadingDuration, long pUnloadingDuration, double pMagnitude) {
      super(pDestination, pLoadingDuration, TimeWindow.ALWAYS,
          pUnloadingDuration, TimeWindow.ALWAYS, pMagnitude);
      setStartPosition(startPosition);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
  }

  // currently has no function
  static class TaxiBase extends Depot {
    TaxiBase(Point position, double capacity) {
      setStartPosition(position);
      setCapacity(capacity);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
  }
}
