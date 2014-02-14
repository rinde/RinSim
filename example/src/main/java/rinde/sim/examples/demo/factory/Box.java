package rinde.sim.examples.demo.factory;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.examples.demo.factory.AgvModel.BoxHandle;
import rinde.sim.util.TimeWindow;

class Box extends Parcel {
  final Point origin;
  final BoxHandle boxHandle;

  Box(Point o, Point d, long duration, BoxHandle bh) {
    super(d, duration, TimeWindow.ALWAYS, duration, TimeWindow.ALWAYS, 1);
    origin = o;
    boxHandle = bh;
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    pRoadModel.addObjectAt(this, origin);
  }
}
