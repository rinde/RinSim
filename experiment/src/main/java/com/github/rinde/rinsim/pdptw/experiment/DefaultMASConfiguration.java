/**
 * 
 */
package com.github.rinde.rinsim.pdptw.experiment;

import java.io.Serializable;

import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.pdptw.common.DynamicPDPTWProblem.Creator;
import com.github.rinde.rinsim.scenario.AddDepotEvent;
import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.util.StochasticSupplier;
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
