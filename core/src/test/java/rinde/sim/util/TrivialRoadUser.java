package rinde.sim.util;

import javax.measure.Measure;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;

import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;

/**
 * 
 * Ignores the model registration.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 */
public class TrivialRoadUser implements MovingRoadUser {

  private RoadModel model;
  private static final Measure<Double, Velocity> SPEED = Measure
      .valueOf(1d, NonSI.KILOMETERS_PER_HOUR);

  public RoadModel getRoadModel() {
    return model;
  }

  @Override
  public void initRoadUser(RoadModel m) {
    model = m;
  }

  @Override
  public Measure<Double, Velocity> getSpeed() {
    return SPEED;
  }
}
