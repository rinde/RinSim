/**
 * 
 */
package rinde.sim.examples.pdp;

import java.util.Collection;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.road.RoadModel;

import com.google.common.base.Optional;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
class ExampleTruck extends Vehicle {

  private static final double SPEED = 1000d;

  private Optional<RoadModel> roadModel;
  private Optional<PDPModel> pdpModel;
  private Optional<Parcel> curr;

  ExampleTruck(Point startPosition, double capacity) {
    setStartPosition(startPosition);
    setCapacity(capacity);
    roadModel = Optional.absent();
    pdpModel = Optional.absent();
    curr = Optional.absent();
  }

  @Override
  public double getSpeed() {
    return SPEED;
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  protected void tickImpl(TimeLapse time) {
    if (pdpModel.get().getContents(this).isEmpty()) {
      final Collection<Parcel> parcels = pdpModel.get().getParcels(
          ParcelState.AVAILABLE);
      if (!parcels.isEmpty() && !curr.isPresent()) {
        double dist = Double.POSITIVE_INFINITY;
        for (final Parcel p : parcels) {
          final double d = Point.distance(roadModel.get().getPosition(this),
              roadModel.get().getPosition(p));
          if (d < dist) {
            dist = d;
            curr = Optional.of(p);
          }
        }
      }

      if (curr.isPresent() && roadModel.get().containsObject(curr.get())) {
        roadModel.get().moveTo(this, curr.get(), time);

        if (roadModel.get().equalPosition(this, curr.get())) {
          pdpModel.get().pickup(this, curr.get(), time);
        }
      } else {
        curr = Optional.absent();
      }
    } else {
      roadModel.get().moveTo(this, curr.get().getDestination(), time);
      if (roadModel.get().getPosition(this).equals(curr.get().getDestination())) {
        pdpModel.get().deliver(this, curr.get(), time);
      }
    }
  }

  @Override
  public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    roadModel = Optional.of(pRoadModel);
    pdpModel = Optional.of(pPdpModel);
  }
}
