/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.AbstractModel;
import com.github.rinde.rinsim.core.DependencyProvider;
import com.github.rinde.rinsim.core.Model;
import com.github.rinde.rinsim.core.ModelBuilder;
import com.github.rinde.rinsim.core.ModelBuilder.AbstractModelBuilder;
import com.google.common.base.Supplier;

/**
 * The random model provides a centralized mechanism for distributing random
 * values throughout an application. The model can be used by implementing the
 * {@link RandomUser} interface. The model allows to set a master seed that is
 * used for all random numbers. Sometimes it is preferable to have different
 * {@link RandomGenerator} instances for different parts of the code to make
 * sure they are independent of each other. The {@link RandomProvider} that is
 * injected into a {@link RandomUser} provides several options for this use
 * case. Obtaining instances can be done via the static creation methods:
 * <ul>
 * <li>{@link #create()}</li>
 * <li>{@link #create(long)}</li>
 * <li>{@link #create(RandomGenerator)}</li>
 * </ul>
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
   * @return A new {@link RandomModel} with the {@link RandomModel#DEFAULT_SEED}
   *         .
   */
  public static RandomModel create() {
    return create(DEFAULT_SEED);
  }

  /**
   * Creates a new {@link RandomModel} using a {@link MersenneTwister} and the
   * specified seed.
   * @param seed The seed to use.
   * @return A new instance.
   */
  public static RandomModel create(long seed) {
    return create(new MersenneTwister(seed));
  }

  /**
   * Creates a new {@link RandomModel} using the specified
   * {@link RandomGenerator}.
   * @param rng The generator to use.
   * @return A new instance.
   */
  public static RandomModel create(RandomGenerator rng) {
    return new RandomModel(rng);
  }

  /**
   * Creates a new {@link RandomModel} {@link Supplier} using the specified
   * {@link RandomGenerator}.
   * @param rng The generator to use.
   * @return A new supplier instance.
   */
  public static Supplier<RandomModel> supplier(RandomGenerator rng) {
    return new RandomModelSupplier(rng);
  }

  public static ModelBuilder<RandomUser> builder(RandomGenerator rng) {
    return new RandomModelSupplier(rng);
  }

  static class RandomModelSupplier extends AbstractModelBuilder<RandomUser>
    implements Supplier<RandomModel> {
    private final RandomGenerator r;

    RandomModelSupplier(RandomGenerator rng) {
      super(RandomProvider.class);
      r = rng;
    }

    @Override
    public RandomModel get() {
      return new RandomModel(r);
    }

    @Override
    public Model<RandomUser> build(DependencyProvider modelProvider) {
      return new RandomModel(r);
    }

    @Override
    public String toString() {
      return "RandomModelBuilder";
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
