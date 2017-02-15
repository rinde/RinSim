package com.github.rinde.rinsim.core.model.road;

import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

public abstract class AbstractModelSnapshot implements RoadModelSnapshot {

  final Unit<Length> distanceUnit;

  AbstractModelSnapshot(Unit<Length> modelDistanceUnit) {
    distanceUnit = modelDistanceUnit;
  }
}
