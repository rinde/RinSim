package com.github.rinde.rinsim.examples.demo.factory;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.examples.demo.factory.AgvModel.BoxHandle;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;

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
