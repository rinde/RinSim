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
package com.github.rinde.rinsim.scenario.generator;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.AddDepotEvent;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.collect.ImmutableList;

/**
 * Utility class for creating {@link DepotGenerator}s.
 * @author Rinde van Lon 
 */
public final class Depots {
  private static final DepotGenerator SINGLE_CENTERED_DEPOT_GENERATOR = new DepotGenerator() {
    @Override
    public Iterable<? extends AddDepotEvent> generate(long seed, Point center) {
      return ImmutableList.of(new AddDepotEvent(-1, center));
    }
  };

  private Depots() {}

  /**
   * @return A {@link DepotGenerator} that creates a single
   *         {@link AddDepotEvent} that places the depot at the center of the
   *         area.
   */
  public static DepotGenerator singleCenteredDepot() {
    return SINGLE_CENTERED_DEPOT_GENERATOR;
  }

  /**
   * @return A new builder for creating arbitrarily complex
   *         {@link DepotGenerator}s.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Generator of {@link AddDepotEvent}s.
   * @author Rinde van Lon 
   */
  public interface DepotGenerator {
    /**
     * Should create a {@link AddDepotEvent} for each required depot.
     * @param seed The random seed to use for generating the depots.
     * @param center The center of the area as defined by the context (usually a
     *          scenario generator).
     * @return The list of events.
     */
    Iterable<? extends AddDepotEvent> generate(long seed, Point center);
  }

  /**
   * Builder for creating {@link DepotGenerator}s.
   * @author Rinde van Lon 
   */
  public static class Builder {
    StochasticSupplier<Point> positions;
    StochasticSupplier<Integer> numberOfDepots;
    StochasticSupplier<Long> times;

    Builder() {
      positions = StochasticSuppliers.constant(new Point(0d, 0d));
      numberOfDepots = StochasticSuppliers.constant(1);
      times = StochasticSuppliers.constant(-1L);
    }

    /**
     * Set where the positions of the depots should be coming from.
     * @param ps The supplier to use for points.
     * @return This, as per the builder pattern.
     */
    public Builder positions(StochasticSupplier<Point> ps) {
      positions = ps;
      return this;
    }

    /**
     * Sets the number of depots that the {@link DepotGenerator} should
     * generate. This number is {@link StochasticSupplier} itself meaning that it can
     * be drawn from a random distribution.
     * @param nd The number of depots.
     * @return This, as per the builder pattern.
     */
    public Builder numerOfDepots(StochasticSupplier<Integer> nd) {
      numberOfDepots = nd;
      return this;
    }

    /**
     * Sets the event times that will be used for the creation of the depots.
     * @param ts The event times.
     * @return This, as per the builder pattern.
     */
    public Builder times(StochasticSupplier<Long> ts) {
      times = ts;
      return this;
    }

    /**
     * @return Creates a new {@link DepotGenerator} based on this builder.
     */
    public DepotGenerator build() {
      return new MultiDepotGenerator(this);
    }
  }

  private static class MultiDepotGenerator implements DepotGenerator {
    private final StochasticSupplier<Point> positions;
    private final StochasticSupplier<Integer> numberOfDepots;
    private final StochasticSupplier<Long> times;
    private final RandomGenerator rng;

    MultiDepotGenerator(Builder b) {
      positions = b.positions;
      numberOfDepots = b.numberOfDepots;
      times = b.times;
      rng = new MersenneTwister();
    }

    @Override
    public Iterable<? extends AddDepotEvent> generate(long seed, Point center) {
      rng.setSeed(seed);
      final int num = numberOfDepots.get(rng.nextLong());
      final ImmutableList.Builder<AddDepotEvent> builder = ImmutableList
          .builder();
      for (int i = 0; i < num; i++) {
        final long time = times.get(rng.nextLong());
        final Point position = positions.get(rng.nextLong());
        builder.add(new AddDepotEvent(time, position));
      }
      return builder.build();
    }
  }
}
