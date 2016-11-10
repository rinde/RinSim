package com.github.rinde.rinsim.core.model.road;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;

public class BrokenRoadModel extends GraphRoadModelImpl {
  public BrokenRoadModel(Graph<? extends ConnectionData> pGraph) {
    super(pGraph, RoadModelBuilders.staticGraph(pGraph));
  }

  @Override
  public boolean doRegister(RoadUser obj) {
    throw new RuntimeException("intended failure");
  }

  @Override
  public boolean unregister(RoadUser obj) {
    throw new RuntimeException("intended failure");
  }
}
