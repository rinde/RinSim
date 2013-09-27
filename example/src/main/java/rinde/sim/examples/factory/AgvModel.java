package rinde.sim.examples.factory;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.Simulator.SimulatorEventType;
import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.ModelProvider;
import rinde.sim.core.model.ModelReceiver;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.PDPModelEvent;
import rinde.sim.core.model.pdp.PDPModel.PDPModelEventType;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

class AgvModel implements TickListener, ModelReceiver, Model<AGV>,
    SimulatorUser, Listener {

  Optional<RoadModel> rm;
  Optional<SimulatorAPI> simulator;
  final RandomGenerator rng;
  Set<Point> occupiedPositions;
  ImmutableList<ImmutableList<Point>> points;
  int currentBox;

  double xMax = 0;
  double yMax = 0;

  List<BoxHandle> boxes;
  final List<Point> border;

  AgvModel(RandomGenerator r, ImmutableList<ImmutableList<Point>> ps,
      ImmutableList<Point> b) {
    rm = Optional.absent();
    rng = r;
    occupiedPositions = newLinkedHashSet();
    points = ps;
    currentBox = 0;
    boxes = newArrayList();
    border = b;
  }

  @Override
  public void tick(TimeLapse timeLapse) {}

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  public void registerModelProvider(ModelProvider mp) {
    rm = Optional.fromNullable(mp.getModel(RoadModel.class));
    Optional
        .fromNullable(mp.getModel(PDPModel.class))
        .get()
        .getEventAPI()
        .addListener(this, PDPModelEventType.END_DELIVERY,
            PDPModelEventType.END_PICKUP);
  }

  @Override
  public void setSimulator(SimulatorAPI api) {
    simulator = Optional.of(api);
    simulator.get().getEventAPI()
        .addListener(this, SimulatorEventType.CONFIGURED);
  }

  void init() {
    if (simulator.isPresent() && rm.isPresent()) {
      int max = 0;
      for (final List<Point> ps : points) {
        max = Math.max(max, ps.size());
      }
      final int num = max;
      for (int i = 0; i < num; i++) {
        final long duration = DoubleMath.roundToLong(
            (FactoryExample.SERVICE_DURATION / 2d)
                + (rng.nextDouble() * FactoryExample.SERVICE_DURATION),
            RoundingMode.CEILING);

        final Point rnd = rndBorder();
        Point dest;
        if (i >= points.get(0).size()) {
          dest = rndBorder();
        } else {
          dest = points.get(0).get(i);
          occupiedPositions.add(dest);
        }

        final BoxHandle bh = new BoxHandle(i);
        final Box b = new Box(rnd, dest, duration, bh);
        bh.box = b;

        boxes.add(bh);
        simulator.get().register(boxes.get(boxes.size() - 1).box);
      }
    }
  }

  @Override
  public boolean register(AGV element) {
    element.registerAgvModel(this);
    return true;
  }

  @Override
  public boolean unregister(AGV element) {
    return false;
  }

  @Override
  public Class<AGV> getSupportedType() {
    return AGV.class;
  }

  @Override
  public void handleEvent(Event e) {
    if (e.getEventType() == SimulatorEventType.CONFIGURED) {
      init();
    } else {
      final PDPModelEvent event = (PDPModelEvent) e;
      if (e.getEventType() == PDPModelEventType.END_PICKUP) {
        occupiedPositions.remove(((Box) event.parcel).origin);
      }
      if (e.getEventType() == PDPModelEventType.END_DELIVERY) {
        final long duration = DoubleMath.roundToLong(
            (FactoryExample.SERVICE_DURATION / 2d)
                + (rng.nextDouble() * FactoryExample.SERVICE_DURATION),
            RoundingMode.CEILING);
        simulator.get().unregister(event.parcel);

        final BoxHandle bh = ((Box) event.parcel).boxHandle;
        bh.wordIndex = (bh.wordIndex + 1) % points.size();

        Point dest;
        if (bh.index >= points.get(bh.wordIndex).size()) {
          dest = rndBorder();
        } else {
          dest = points.get(bh.wordIndex).get(bh.index);
          occupiedPositions.add(dest);
        }

        final Box b = new Box(event.parcel.getDestination(), dest, duration, bh);
        bh.box = b;

        simulator.get().register(b);
      }
    }
  }

  Point rndBorder() {
    return border.get(rng.nextInt(border.size()));
  }

  Point rnd() {
    Point p;
    do {
      p = rm.get().getRandomPosition(rng);
    } while (occupiedPositions.contains(p));
    occupiedPositions.add(p);
    return p;
  }

  Box nextDestination() {
    final Box b = boxes.get(currentBox % boxes.size()).box;
    currentBox++;
    return b;
  }

  class BoxHandle {
    int wordIndex;
    final int index;
    Box box;

    BoxHandle(int i) {
      index = i;
      wordIndex = 0;
    }
  }
}
