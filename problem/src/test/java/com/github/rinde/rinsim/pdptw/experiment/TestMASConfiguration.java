package com.github.rinde.rinsim.pdptw.experiment;

import java.io.Serializable;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.pdptw.RandomVehicle;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.Creator;
import com.github.rinde.rinsim.scenario.AddDepotEvent;
import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class TestMASConfiguration implements MASConfiguration, Serializable {

  private final String name;

  TestMASConfiguration(String nm) {
    name = nm;
  }

  public static TestMASConfiguration create(String name) {
    return new TestMASConfiguration(name);
  }

  @Override
  public ImmutableList<? extends StochasticSupplier<? extends Model<?>>> getModels() {
    return ImmutableList.of();
  }

  @Override
  public Creator<AddVehicleEvent> getVehicleCreator() {
    return new Creator<AddVehicleEvent>() {
      @Override
      public boolean create(Simulator sim, AddVehicleEvent event) {
        return sim.register(new RandomVehicle(event.vehicleDTO, sim
            .getRandomGenerator().nextLong()));
      }
    };
  }

  @Override
  public Optional<? extends Creator<AddDepotEvent>> getDepotCreator() {
    return Optional.absent();
  }

  @Override
  public Optional<? extends Creator<AddParcelEvent>> getParcelCreator() {
    return Optional.absent();
  }

  @Override
  public String toString() {
    return name;
  }
}
