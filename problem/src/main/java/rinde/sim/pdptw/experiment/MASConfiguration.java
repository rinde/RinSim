/**
 * 
 */
package rinde.sim.pdptw.experiment;

import rinde.sim.core.model.Model;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.Creator;
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
