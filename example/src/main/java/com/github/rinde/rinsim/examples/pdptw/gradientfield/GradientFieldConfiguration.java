package com.github.rinde.rinsim.examples.pdptw.gradientfield;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.Creator;
import com.github.rinde.rinsim.pdptw.experiment.DefaultMASConfiguration;
import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * 
 * @author Rinde van Lon 
 */
public class GradientFieldConfiguration extends DefaultMASConfiguration {

  @Override
  public ImmutableList<? extends StochasticSupplier<? extends Model<?>>> getModels() {
    return ImmutableList.of(GradientModel.supplier());
  }

  @Override
  public Creator<AddVehicleEvent> getVehicleCreator() {
    return new Creator<AddVehicleEvent>() {
      @Override
      public boolean create(Simulator sim, AddVehicleEvent event) {
        return sim.register(new Truck(event.vehicleDTO));
      }
    };
  }

  @Override
  public Optional<? extends Creator<AddParcelEvent>> getParcelCreator() {
    return Optional.of(new Creator<AddParcelEvent>() {
      @Override
      public boolean create(Simulator sim, AddParcelEvent event) {
        // all parcels are accepted by default
        return sim.register(new GFParcel(event.parcelDTO));
      }
    });
  }

}
