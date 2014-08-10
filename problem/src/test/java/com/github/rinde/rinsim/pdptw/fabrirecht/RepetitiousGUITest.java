/**
 * 
 */
package com.github.rinde.rinsim.pdptw.fabrirecht;

import java.io.File;
import java.io.IOException;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.pdptw.DefaultParcel;
import com.github.rinde.rinsim.core.pdptw.DefaultVehicle;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.Creator;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.DefaultUICreator;
import com.github.rinde.rinsim.pdptw.fabrirecht.FabriRechtParser;
import com.github.rinde.rinsim.pdptw.fabrirecht.FabriRechtScenario;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.Renderer;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.io.Files;

/**
 * Simplest example showing how the Fabri & Recht problem can be configured
 * using a custom vehicle.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class RepetitiousGUITest {

  public static void main(String[] args) throws IOException {
    for (int i = 0; i < 100; i++) {
      final FabriRechtScenario scenario = FabriRechtParser.fromJson(Files
          .toString(new File("files/test/fabri-recht/lc101.scenario"),
              Charsets.UTF_8), 8, 20);

      final DynamicPDPTWProblem problem = new DynamicPDPTWProblem(scenario, 123);
      problem.addCreator(AddVehicleEvent.class, new Creator<AddVehicleEvent>() {
        @Override
        public boolean create(Simulator sim, AddVehicleEvent event) {
          return sim.register(new Truck(event.vehicleDTO));
        }
      });
      final int iteration = i;

      problem.enableUI(new DefaultUICreator(problem, 15) {
        @Override
        public void createUI(Simulator sim) {
          try {
            initRenderers();
            View.create(sim).with(renderers.toArray(new Renderer[] {}))
                .setSpeedUp(speedup).enableAutoClose().enableAutoPlay().show();
          } catch (final Throwable e) {
            System.err.println("Crash occured at iteration " + iteration);
            e.printStackTrace();
            throw new RuntimeException("This is the end, resistance is futile.");
          }
        }
      });
      problem.simulate();
    }
  }
}

/**
 * This truck implementation only picks parcels up, it does not deliver them.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
class Truck extends DefaultVehicle {

  public Truck(VehicleDTO pDto) {
    super(pDto);
  }

  @Override
  protected void tickImpl(TimeLapse time) {
    final RoadModel rm = roadModel.get();
    final PDPModel pm = pdpModel.get();
    // we always go to the closest available parcel
    final DefaultParcel closest = (DefaultParcel) RoadModels.findClosestObject(
        rm.getPosition(this), rm, new Predicate<RoadUser>() {
          @Override
          public boolean apply(RoadUser input) {
            return input instanceof DefaultParcel
                && pm.getParcelState((DefaultParcel) input) == ParcelState.AVAILABLE;
          }
        });

    if (closest != null) {
      rm.moveTo(this, closest, time);
      if (rm.equalPosition(closest, this)
          && pm.getTimeWindowPolicy().canPickup(closest.getPickupTimeWindow(),
              time.getTime(), closest.getPickupDuration())) {
        pm.pickup(this, closest, time);
      }
    }
  }
}
