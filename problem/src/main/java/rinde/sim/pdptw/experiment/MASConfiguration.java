/**
 * 
 */
package rinde.sim.pdptw.experiment;

import rinde.sim.core.model.Model;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.Creator;
import rinde.sim.scenario.AddDepotEvent;
import rinde.sim.scenario.AddParcelEvent;
import rinde.sim.scenario.AddVehicleEvent;
import rinde.sim.util.StochasticSupplier;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * This represents a configuration of a multi-agent system in a simulation.
 * Implementations should always be immutable.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
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
