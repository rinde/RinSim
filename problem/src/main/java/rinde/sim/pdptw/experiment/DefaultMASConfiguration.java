/**
 * 
 */
package rinde.sim.pdptw.experiment;

import java.io.Serializable;

import rinde.sim.core.model.Model;
import rinde.sim.core.pdptw.AddDepotEvent;
import rinde.sim.core.pdptw.AddParcelEvent;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.Creator;
import rinde.sim.util.StochasticSupplier;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of {@link MASConfiguration} which implements most method using
 * default values.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public abstract class DefaultMASConfiguration implements MASConfiguration,
    Serializable {
  private static final long serialVersionUID = 4815504615843930209L;

  @Override
  public ImmutableList<? extends StochasticSupplier<? extends Model<?>>> getModels() {
    return ImmutableList.of();
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
    return getClass().getSimpleName();
  }
}
