/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.core.model.rand;

import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.auto.value.AutoValue;

/**
 * The random model provides a centralized mechanism for distributing random
 * values throughout an application. The model can be used by implementing the
 * {@link RandomUser} interface. The model allows to set a master seed that is
 * used for all random numbers. Sometimes it is preferable to have different
 * {@link RandomGenerator} instances for different parts of the code to make
 * sure they are independent of each other. The {@link RandomProvider} that is
 * injected into a {@link RandomUser} provides several options for this use
 * case. A builder can be obtained via {@link #builder()}.
 * <p>
 * <b>Model properties</b>
 * <ul>
 * <li><i>Associated type:</i> {@link RandomUser}.</li>
 * <li><i>Provides:</i> {@link RandomProvider}.</li>
 * <li><i>Dependencies:</i> none.</li>
 * </ul>
 * See {@link ModelBuilder} for more information about model properties.
 *
 * @author Rinde van Lon
 * @see RandomUser
 * @see RandomProvider
 */
public class RandomModel extends AbstractModel<RandomUser> {
  /**
   * The default random seed: 123.
   */
  public static final long DEFAULT_SEED = 123L;

  final RandomGenerator masterRandomGenerator;
  final Map<Class<?>, RandomGenerator> classRngMap;

  RandomModel(RandomGenerator rng) {
    masterRandomGenerator = new UnmodifiableRandomGenerator(rng);
    classRngMap = new LinkedHashMap<>();
  }

  @Override
  public boolean register(RandomUser element) {
    final RngProvider provider = new RngProvider();
    element.setRandomGenerator(provider);
    provider.invalidate();
    return true;
  }

  @Override
  @Nonnull
  public <U> U get(Class<U> clazz) {
    if (clazz == RandomProvider.class) {
      return clazz.cast(new RngProvider());
    }
    throw new IllegalArgumentException();
  }

  @Override
  public boolean unregister(RandomUser element) {
    return true;
  }

  /**
   * @return A new {@link RandomModel} that uses a {@link MersenneTwister} with
   *         seed: {@link RandomModel#DEFAULT_SEED}.
   */
  @CheckReturnValue
  public static Builder builder() {
    return Builder.create(DEFAULT_SEED);
  }

  /**
   * A builder for {@link RandomModel}. Instances can be obtained via
   * {@link RandomModel#builder()}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends AbstractModelBuilder<RandomModel, RandomUser>
      implements Serializable {

    static final StochasticSupplier<MersenneTwister> DEFAULT_RNG =
      StochasticSuppliers.mersenneTwister();
    private static final long serialVersionUID = 7985638617806912711L;

    Builder() {
      setProvidingTypes(RandomProvider.class);
    }

    abstract long seed();

    abstract StochasticSupplier<RandomGenerator> rngSupplier();

    /**
     * Returns a copy of this builder with the specified seed.
     * @param seed The random seed.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withSeed(long seed) {
      return create(seed, rngSupplier());
    }

    /**
     * Returns a copy of this builder with the specified random generator
     * supplier.
     * @param supplier The supplier of random generators.
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withRandomGenerator(
        StochasticSupplier<? extends RandomGenerator> supplier) {
      return create(seed(), supplier);
    }

    @Override
    public RandomModel build(DependencyProvider modelProvider) {
      return new RandomModel(rngSupplier().get(seed()));
    }

    static Builder create(long seed) {
      return create(seed, DEFAULT_RNG);
    }

    @SuppressWarnings("unchecked")
    static Builder create(long seed,
        StochasticSupplier<? extends RandomGenerator> ss) {
      return new AutoValue_RandomModel_Builder(seed,
        (StochasticSupplier<RandomGenerator>) ss);
    }
  }

  class RngProvider implements RandomProvider {
    boolean used;

    RngProvider() {
      used = false;
    }

    void stateCheck() {
      checkState(!used, "Can be used only once.");
      invalidate();
    }

    void invalidate() {
      used = true;
    }

    @Override
    public long getSeed() {
      stateCheck();
      return masterRandomGenerator.nextLong();
    }

    @Override
    public RandomGenerator masterInstance() {
      stateCheck();
      return masterRandomGenerator;
    }

    @Override
    public RandomGenerator newInstance() {
      stateCheck();
      return new MersenneTwister(masterRandomGenerator.nextLong());
    }

    @Override
    public RandomGenerator sharedInstance(Class<?> clazz) {
      stateCheck();
      if (!classRngMap.containsKey(clazz)) {
        final RandomGenerator rng = new UnmodifiableRandomGenerator(
          new MersenneTwister(masterRandomGenerator.nextLong()));
        classRngMap.put(clazz, rng);
        return rng;
      }
      return classRngMap.get(clazz);
    }
  }
}
