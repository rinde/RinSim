/**
 * 
 */
package rinde.sim.pdptw.experiment;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.model.Model;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.DynamicPDPTWProblem.Creator;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class DefaultMASConfiguration implements MASConfiguration {

  protected final long randomSeed;
  protected final RandomGenerator rng;

  protected DefaultMASConfiguration(long seed) {
    randomSeed = seed;
    rng = new MersenneTwister(randomSeed);
  }

  @Override
  public ImmutableList<? extends Model<?>> getModels() {
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
    return getClass().getSimpleName() + "-" + randomSeed;
  }
}
