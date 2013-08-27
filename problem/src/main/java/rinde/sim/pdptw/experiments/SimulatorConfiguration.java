/**
 * 
 */
package rinde.sim.pdptw.experiments;

import rinde.sim.core.model.Model;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.Creator;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * This represents the 'solution'/approach to solving the problem.
 * 
 * A configuration should be used for only one simulation!
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface SimulatorConfiguration {

  /**
   * May be empty.
   * @return
   */
  ImmutableList<Model<?>> getModels();

  Creator<AddVehicleEvent> getVehicleCreator();

  Optional<Creator<AddDepotEvent>> getDepotCreator();

  Optional<Creator<AddParcelEvent>> getParcelCreator();

}
