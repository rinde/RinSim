package rinde.sim.examples.factory;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.road.RoadModel;

import com.google.common.base.Optional;

class AGV extends Vehicle {
  private Optional<RoadModel> roadModel;
  private Optional<PDPModel> pdpModel;
  private Optional<Box> destination;
  private final RandomGenerator randomGenerator;
  private Optional<AgvModel> agvModel;

  AGV(RandomGenerator rng) {
    roadModel = Optional.absent();
    destination = Optional.absent();
    agvModel = Optional.absent();
    randomGenerator = rng;
    setCapacity(1);
  }

  @Override
  public double getSpeed() {
    return FactoryExample.AGV_SPEED;
  }

  @Override
  protected void tickImpl(TimeLapse time) {
    if (!time.hasTimeLeft()) {
      return;
    }
    if (!destination.isPresent()) {
      final Box closest = agvModel.get().nextDestination();
      if (closest != null) {
        destination = Optional.of(closest);
      }
    }

    if (destination.isPresent()) {
      if (roadModel.get().equalPosition(this, destination.get())) {
        pdpModel.get().pickup(this, destination.get(), time);
      } else if (pdpModel.get().getContents(this).contains(destination.get())) {
        if (roadModel.get().getPosition(this)
            .equals(destination.get().getDestination())) {
          pdpModel.get().deliver(this, destination.get(), time);
          destination = Optional.absent();
        } else {
          roadModel.get()
              .moveTo(this, destination.get().getDestination(), time);
        }
      } else {
        if (roadModel.get().containsObject(destination.get())) {
          roadModel.get().moveTo(this, destination.get(), time);
        } else {
          destination = Optional.absent();
        }
      }
    }
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    roadModel = Optional.of(pRoadModel);
    pdpModel = Optional.of(pPdpModel);
    pRoadModel.addObjectAt(this,
        roadModel.get().getRandomPosition(randomGenerator));
  }

  /**
   * Injection of AvgModel;
   * @param model Model to inject.
   */
  public void registerAgvModel(AgvModel model) {
    agvModel = Optional.of(model);
  }
}
