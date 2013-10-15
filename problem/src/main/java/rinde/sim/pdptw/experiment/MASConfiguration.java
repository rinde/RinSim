/**
 * 
 */
package rinde.sim.pdptw.experiment;

import rinde.sim.core.model.Model;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.Creator;
import rinde.sim.util.SupplierRng;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * This represents the 'solution'/approach to solving the problem.
 * 
 * A configuration should be used for only one simulation, a single experiment!
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface MASConfiguration {

  /**
   * May be empty.
   * @return
   */
  ImmutableList<? extends SupplierRng<? extends Model<?>>> getModels();

  Creator<AddVehicleEvent> getVehicleCreator();

  Optional<? extends Creator<AddDepotEvent>> getDepotCreator();

  Optional<? extends Creator<AddParcelEvent>> getParcelCreator();

}
