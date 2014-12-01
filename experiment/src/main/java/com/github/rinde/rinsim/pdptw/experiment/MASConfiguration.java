package com.github.rinde.rinsim.pdptw.experiment;

import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.Creator;
import com.github.rinde.rinsim.scenario.AddDepotEvent;
import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * This represents a configuration of a multi-agent system in a simulation.
 * Implementations should always be immutable.
 * @author Rinde van Lon 
 */
public interface MASConfiguration {

  /**
   * @return A possibly empty list of model suppliers.
   */
  ImmutableList<? extends StochasticSupplier<? extends Model<?>>> getModels();

  /**
   * @return A creator that creates vehicle agents.
   */
  Creator<AddVehicleEvent> getVehicleCreator();

  /**
   * @return A creator that creates depot agents.
   */
  Optional<? extends Creator<AddDepotEvent>> getDepotCreator();

  /**
   * @return A creator that creates parcel agents.
   */
  Optional<? extends Creator<AddParcelEvent>> getParcelCreator();
}
