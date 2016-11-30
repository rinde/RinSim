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
package com.github.rinde.rinsim.scenario.generator;

import static com.github.rinde.rinsim.util.StochasticSuppliers.constant;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.pdptw.common.ChangeConnectionSpeedEvent;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Utility class for creating {@link DynamicSpeedGenerator}s.
 * @author Vincent Van Gestel
 */
public final class DynamicSpeeds {

  static final int DEFAULT_NUM_OF_SHOCKWAVES = 1;
  static final double EIGHTY_PERCENT = 0.8d;
  static final double TWENTY_PERCENT = 0.2d;
  static final double TEN_KM_H = 0.002777777777777778d;
  static final double FIVE_KM = 5000d;
  static final long ONE_AND_A_HALF_HOUR = 5400000L;

  /**
   * The default number of shockwaves is 1.
   */
  static final StochasticSupplier<Integer> DEFAULT_NUMBER_OF_SHOCKWAVES =
    constant(DEFAULT_NUM_OF_SHOCKWAVES);

  static final Function<Double, Double> DEFAULT_BEHAVIOUR_FUNCTION =
    new Function<Double, Double>() {
      @Override
      public Double apply(@Nonnull Double input) {
        return Math.max(0,
          Math.min(EIGHTY_PERCENT / FIVE_KM * input + TWENTY_PERCENT, 1));
      }
    };
  /**
   * Linear function starting at 0.2d going to 1 over 1Km distance.
   */
  static final StochasticSupplier<Function<Double, Double>> DEFAULT_BEHAVIOUR =
    constant(DEFAULT_BEHAVIOUR_FUNCTION);

  /**
   * Start (0L).
   */
  static final StochasticSupplier<Long> DEFAULT_TIME = constant(0L);

  static final Function<Long, Double> DEFAULT_SPEED_FUNCTION =
    new Function<Long, Double>() {
      @Override
      public Double apply(Long input) {
        return TEN_KM_H;
      }
    };
  /**
   * Constant function of 0.0028 meters per milliseconds (10 Km/h).
   */
  static final StochasticSupplier<Function<Long, Double>> DEFAULT_RELATIVE_RECEDING_SPEED =
    constant(DEFAULT_SPEED_FUNCTION);
  static final StochasticSupplier<Function<Long, Double>> DEFAULT_RELATIVE_EXPANDING_SPEED =
    constant(DEFAULT_SPEED_FUNCTION);
  /**
   * 1.5 Hours.
   */
  static final StochasticSupplier<Long> DEFAULT_DURATION =
    constant(ONE_AND_A_HALF_HOUR);

  private static final DynamicSpeedGenerator ZERO_EVENT_GENERATOR =
    new DynamicSpeedGenerator() {
      @Override
      public ImmutableList<ChangeConnectionSpeedEvent> generate(long seed,
          long scenarioLength) {
        return ImmutableList.of();
      }
    };

  private DynamicSpeeds() {}

  /**
   * @return an immutable list of a single {@link DynamicSpeedGenerator}, which
   *         generates no events.
   */
  public static List<DynamicSpeedGenerator> zeroEvents() {
    return ImmutableList.of(ZERO_EVENT_GENERATOR);
  }

  /**
   * @return A newly constructed {@link Builder} for constructing
   *         {@link DynamicSpeedGenerator}s.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Generator of {@link ChangeConnectionSpeedEvent}s.
   * @author Vincent Van Gestel
   */
  public interface DynamicSpeedGenerator {
    /**
     * Should generate a list of {@link ChangeConnectionSpeedEvent}. Each event
     * indicates a change in maximum allowed speed on a connection.
     * @param seed The random seed to use for generating the vehicles.
     * @param scenarioLength The length of the scenario for which the events are
     *          generated.
     * @return A list of events.
     */
    ImmutableList<ChangeConnectionSpeedEvent> generate(long seed,
        long scenarioLength);
  }

  /**
   * A builder for constructing {@link DynamicSpeedGenerator}s.
   * @author Vincent Van Gestel
   */
  public static class Builder {
    Optional<Graph<MultiAttributeData>> graph;
    StochasticSupplier<Integer> numberOfShockwaves;
    Optional<StochasticSupplier<Connection<MultiAttributeData>>> startingConnectionSupplier;
    StochasticSupplier<Long> eventTimesSupplier;
    StochasticSupplier<Function<Double, Double>> behaviourSupplier;
    StochasticSupplier<Long> eventDurationSupplier;
    StochasticSupplier<Function<Long, Double>> expandingSpeedSupplier;
    StochasticSupplier<Function<Long, Double>> recedingSpeedSupplier;

    Builder() {
      numberOfShockwaves = DEFAULT_NUMBER_OF_SHOCKWAVES;
      startingConnectionSupplier = Optional.absent();
      eventTimesSupplier = DEFAULT_TIME;
      graph = Optional.absent();
      behaviourSupplier = DEFAULT_BEHAVIOUR;
      eventDurationSupplier = DEFAULT_DURATION;
      expandingSpeedSupplier = DEFAULT_RELATIVE_EXPANDING_SPEED;
      recedingSpeedSupplier = DEFAULT_RELATIVE_RECEDING_SPEED;
    }

    /**
     * Sets the number of shockwaves that are to be created by the generator.
     * Default value: 1. All values returned by the {@link StochasticSupplier}
     * must be greater than <code>0</code>.
     * @param num The supplier to draw numbers from.
     * @return This, as per the builder pattern.
     */
    public Builder numberOfShockwaves(StochasticSupplier<Integer> num) {
      numberOfShockwaves = num;
      return this;
    }

    /**
     * Sets the starting connections for the to be generated hotspots. By
     * default the start positions of any hotspot is a random connection on the
     * graph.
     * @param conn The supplier to draw connections from.
     * @return This, as per the builder pattern.
     */
    public Builder startConnections(
        StochasticSupplier<Connection<MultiAttributeData>> conn) {
      startingConnectionSupplier = Optional.of(conn);
      return this;
    }

    /**
     * Sets the start connections of all generated shockwaves to be random. This
     * is the default behavior.
     * @return This, as per the builder pattern.
     */
    public Builder randomStartConnections() {
      startingConnectionSupplier = Optional.absent();
      return this;
    }

    /**
     * Sets the graph to be used for the generation of events.
     * @param g The supplier from which to draw the graph.
     * @return This, as per the builder pattern.
     */
    public Builder withGraph(Graph<MultiAttributeData> g) {
      graph = Optional.of(g);
      return this;
    }

    /**
     * Vehicles Sets the behaviour for the shockwave. The behaviour indicates at
     * which capacity a connection can operate (driving speed relative to
     * maximal allowed speed on said connection), at every possible distance
     * from the original location.
     * @param behaviour The function describing the relation between distance
     *          travelled and speed drop off.
     * @return This, as per the builder pattern.
     */
    public Builder shockwaveBehaviour(
        StochasticSupplier<Function<Double, Double>> behaviour) {
      behaviourSupplier = behaviour;
      return this;
    }

    /**
     * Sets the function declaring the at which speed the shockwave propagates
     * through the graph relative to the time elapsed.
     * @param speed The function describing the relation between elapsed time of
     *          the shockwave to the speed at which it travels.
     * @return This, as per the builder pattern.
     */
    public Builder shockwaveExpandingSpeed(
        StochasticSupplier<Function<Long, Double>> speed) {
      expandingSpeedSupplier = speed;
      return this;
    }

    /**
     * Sets the function declaring the at which speed the shockwave recedes
     * through the graph relative to the time elapsed.
     * @param speed The function describing the relation between elapsed time of
     *          the shockwave to the speed at which it travels.
     * @return This, as per the builder pattern.
     */
    public Builder shockwaveRecedingSpeed(
        StochasticSupplier<Function<Long, Double>> speed) {
      recedingSpeedSupplier = speed;
      return this;
    }

    /**
     * Sets the duration of a shockwave before it starts receding. Default
     * value: 5400000L, which is 1.5 hours.
     * @param times The supplier from which to draw times.
     * @return This, as per the builder pattern.
     */
    public Builder shockwaveDurations(StochasticSupplier<Long> times) {
      eventDurationSupplier = times;
      return this;
    }

    /**
     * Sets the duration times of the total event generated by the generator.
     * Default value: 0, this means that the event will commence at the
     * beginning of the scenario.
     * @param times The supplier from which to draw times.
     * @return This, as per the builder pattern.
     */
    public Builder creationTimes(StochasticSupplier<Long> times) {
      eventTimesSupplier = times;
      return this;
    }

    /**
     * @return A newly constructed {@link DynamicSpeedGenerator} as specified by
     *         this builder.
     */
    public DynamicSpeedGenerator build() {
      return new DefaultDynamicSpeedGenerator(this);
    }
  }

  private static class DefaultDynamicSpeedGenerator
      implements DynamicSpeedGenerator {

    // Logger
    private static final Logger LOGGER =
      Logger.getLogger("DynamicSpeedGenerator");

    // Generator variables
    private final Optional<Graph<MultiAttributeData>> graph;
    private final StochasticSupplier<Integer> numberOfShockwaves;
    private final Optional<StochasticSupplier<Connection<MultiAttributeData>>> startingConnectionSupplier;
    private final StochasticSupplier<Long> eventTimesSupplier;
    private final StochasticSupplier<Function<Double, Double>> behaviourSupplier;
    private final StochasticSupplier<Long> eventDurationSupplier;
    private final StochasticSupplier<Function<Long, Double>> expandingSpeedSupplier;
    private final StochasticSupplier<Function<Long, Double>> recedingSpeedSupplier;
    private final RandomGenerator rng;

    // Shockwave variables
    private Map<Connection<MultiAttributeData>, List<ChangeConnectionSpeedEvent>> expansionMap;
    private Graph<MultiAttributeData> affectedGraph;
    private Set<Connection<MultiAttributeData>> leafNodes;

    private int iteration;

    DefaultDynamicSpeedGenerator(Builder b) {
      numberOfShockwaves = b.numberOfShockwaves;
      startingConnectionSupplier = b.startingConnectionSupplier;
      eventTimesSupplier = b.eventTimesSupplier;
      graph = b.graph;
      behaviourSupplier = b.behaviourSupplier;
      eventDurationSupplier = b.eventDurationSupplier;
      expandingSpeedSupplier = b.expandingSpeedSupplier;
      recedingSpeedSupplier = b.recedingSpeedSupplier;
      rng = new MersenneTwister();
    }

    @Override
    public ImmutableList<ChangeConnectionSpeedEvent> generate(long seed,
        long scenarioLength) {
      final ImmutableList.Builder<ChangeConnectionSpeedEvent> builder =
        ImmutableList.builder();

      final int numShockwaves = numberOfShockwaves.get(rng.nextLong());
      checkArgument(numShockwaves > 0,
        "The numberOfShockwaves supplier must generate values > 0, found %s.",
        numShockwaves);
      for (int i = 0; i < numShockwaves; i++) {
        iteration = i + 1;
        LOGGER.info(
          "~ Generating Shockwave " + iteration + " out of " + numShockwaves);
        final Connection<MultiAttributeData> conn;

        if (startingConnectionSupplier.isPresent()) {
          conn = startingConnectionSupplier.get().get(rng.nextLong());
        } else {
          conn = graph.get().getRandomConnection(rng);
        }

        // Emulate shockwave starting in origin
        final long startingTime = eventTimesSupplier.get(rng.nextLong());

        expansionMap = new HashMap<>();
        affectedGraph = new TableGraph<>();
        leafNodes = new HashSet<>();

        LOGGER.info("~ Expanding Shockwave " + iteration);

        // Breath First --
        final Queue<ShockwaveSimulation> shockwave = new LinkedList<>();
        shockwave.offer(
          new ShockwaveSimulation(shockwave, conn, conn, 0, startingTime, 0,
            scenarioLength));

        while (!shockwave.isEmpty()) {
          builder.addAll(shockwave.poll().simulateExpandingShockwave());
        }
        LOGGER.info("~ Receding Shockwave " + iteration);
        shockwave.offer(new ShockwaveSimulation(shockwave, conn, conn, 0,
          startingTime + eventDurationSupplier.get(rng.nextLong()),
          0,
          scenarioLength));

        while (!shockwave.isEmpty()) {
          builder.addAll(shockwave.poll().simulateForwardRecedingShockwave());
        }
        // -- Breath First

        // Depth First --
        // builder
        // .addAll(simulateExpandingShockwave(conn, conn, 0, startingTime, 0,
        // scenarioLength));
        // simulation of forward receding shockwave
        // LOGGER.info("~ Receding Shockwave " + iteration);
        // builder.addAll(simulateForwardRecedingShockwave(conn, 0,
        // startingTime + eventDurationSupplier.get(rng.nextLong()),
        // 0,
        // scenarioLength));
        // simulation of expanding shockwave created graph and saved leaf nodes
        // for (final Connection<MultiAttributeData> lconn : leafNodes) {
        // builder.addAll(simulateBackwardsRecedingShockwave(lconn,
        // startingTime + eventDurationSupplier.get(rng.nextLong()),
        // scenarioLength));
        // }
        // -- Depth First
      }
      return builder.build();
    }

    // Code used for depth first, probably not going to be used
    // /**
    // *
    // * @param origin The previous connection
    // * @param conn The current connection
    // * @param relTimestamp The time of the shockwave after travelling origin
    // * (before travelling current conn)
    // * @param actualTimestamp The time of the simulation before travelling
    // over
    // * current conn
    // * @param distance Total distance travelled
    // * @param scenarioLength Total duration of the scenario
    // * @return the simulated shockwave
    // */
    // private List<ChangeConnectionSpeedEvent> simulateExpandingShockwave(
    // Connection<MultiAttributeData> origin,
    // Connection<MultiAttributeData> conn, long relTimestamp,
    // long actualTimestamp, double distance, long scenarioLength) {
    // // LOGGER.info("~~ Expansion " + iteration + ": handling connection from
    // "
    // // + conn.from() + " to " + conn.to() + " at distance " + distance);
    //
    // final List<ChangeConnectionSpeedEvent> events = new ArrayList<>();
    // final double factor =
    // behaviourSupplier.get(rng.nextLong())
    // .apply(distance + conn.getLength() / 2);
    // final double speed =
    // expandingSpeedSupplier.get(rng.nextLong()).apply(relTimestamp);
    //
    // long next_rel_timestamp = 0L;
    // long next_actual_timestamp = 0L;
    //
    // if (speed != 0) {
    // next_rel_timestamp = (long) (relTimestamp + conn.getLength() / speed);
    // next_actual_timestamp =
    // (long) (actualTimestamp + conn.getLength() / speed);
    // }
    //
    // // Check Stop conditions
    // // Stop if shockwave has no effect (factor == 1)
    // // OR if shockwave burned out (speed == 0)
    // // OR if shockwave duration reached
    // // OR if next step if after scenario boundary
    // if (factor == 1 || speed == 0
    // || next_rel_timestamp >= eventDurationSupplier
    // .get(rng.nextLong())
    // || next_actual_timestamp >= scenarioLength) {
    // leafNodes.add(origin); // Origin was last affected node
    // // LOGGER.info("~~ Expansion " + iteration + ": stop condition
    // // reached");
    // return events; // Stop
    // }
    //
    // // Add conn
    // final ChangeConnectionSpeedEvent newEvent =
    // ChangeConnectionSpeedEvent.create(next_actual_timestamp,
    // conn, factor);
    // events.add(newEvent);
    //
    // // Keep track of events linked to connections
    // if (expansionMap.containsKey(conn)) {
    // // Cycle, add to existing list
    // final List<ChangeConnectionSpeedEvent> eventList =
    // expansionMap.get(conn);
    // eventList.add(newEvent);
    // expansionMap.put(conn, eventList);
    // } else {
    // // New connection
    // expansionMap.put(conn, Lists.newArrayList(newEvent));
    // affectedGraph.addConnection(conn);
    // }
    //
    // // Branch out
    // final Collection<Point> next_froms = graph.get()
    // .getIncomingConnections(conn.from());
    // // Check dead end -> no need to continue branching
    // if (next_froms.isEmpty() || next_froms.size() == 1
    // && next_froms.iterator().next().equals(conn.to())) {
    // leafNodes.add(conn);
    // // LOGGER.info("~~ Expansion " + iteration + ": dead end reached");
    // return events;
    // }
    // // LOGGER.info("~~ Expansion " + iteration + ": Branching Factor " +
    // // next_froms.size());
    // for (final Point next_from : next_froms) {
    // if (next_from.equals(conn.to())) {
    // // No backtracking in the other direction
    // continue;
    // }
    // final Connection<MultiAttributeData> next_conn =
    // graph.get().getConnection(next_from, conn.from());
    // events.addAll(simulateExpandingShockwave(conn, next_conn,
    // next_rel_timestamp, next_actual_timestamp,
    // distance + conn.getLength(), scenarioLength));
    // }
    // // LOGGER.info("~~ Expansion " + iteration + ": Closed Branch");
    // return events;
    // }
    //
    // /**
    // *
    // * @param conn The connection being lifted
    // * @param actualTimestamp The timestamp before lifting event
    // * @param scenarioLength The total duration of the scenario
    // * @return
    // */
    // private List<ChangeConnectionSpeedEvent>
    // simulateForwardRecedingShockwave(
    // Connection<MultiAttributeData> conn, long relTimestamp,
    // long actualTimestamp, double distance, long scenarioLength) {
    // final List<ChangeConnectionSpeedEvent> events = new ArrayList<>();
    // final double factor = 1 /
    // behaviourSupplier.get(rng.nextLong())
    // .apply(distance + conn.getLength() / 2);
    // final double speed =
    // recedingSpeedSupplier.get(rng.nextLong()).apply(relTimestamp);
    //
    // long next_rel_timestamp = 0L;
    // long next_actual_timestamp = 0L;
    //
    // if (speed != 0) {
    // next_rel_timestamp = (long) (relTimestamp + conn.getLength() / speed);
    // next_actual_timestamp =
    // (long) (actualTimestamp + conn.getLength() / speed);
    // }
    //
    // // Check Stop conditions
    // // Stop if connection wasn't previously affected
    // // OR if shockwave burned out (speed == 0)
    // // OR if next step if after scenario boundary
    // if (!affectedGraph.hasConnection(conn) || speed == 0
    // || next_actual_timestamp >= scenarioLength) {
    // return events; // Stop
    // }
    //
    // // Remove conn
    // final ChangeConnectionSpeedEvent newEvent =
    // ChangeConnectionSpeedEvent.create(next_actual_timestamp,
    // conn, factor);
    // events.add(newEvent);
    //
    // // Remove track of events linked to connections
    // if (expansionMap.get(conn).size() > 1) {
    // // Cycle, remove only 1
    // final List<ChangeConnectionSpeedEvent> eventList =
    // expansionMap.get(conn);
    // eventList.remove(0);
    // expansionMap.put(conn, eventList);
    // } else {
    // // Last occurrence
    // expansionMap.remove(conn);
    // affectedGraph.removeConnection(conn.from(), conn.to());
    // }
    //
    // // Branch out
    // final Collection<Point> next_froms = ImmutableList.copyOf(
    // affectedGraph.getIncomingConnections(conn.from()));
    // // Check dead end -> no need to continue branching
    // if (next_froms.isEmpty()) {
    // return events;
    // }
    // for (final Point next_from : next_froms) {
    // final Connection<MultiAttributeData> next_conn =
    // graph.get().getConnection(next_from, conn.from());
    // events.addAll(
    // simulateForwardRecedingShockwave(next_conn, next_rel_timestamp,
    // next_actual_timestamp,
    // distance + conn.getLength(), scenarioLength));
    // }
    //
    // return events;
    // }

    class ShockwaveSimulation {

      private final Queue<ShockwaveSimulation> shockwave;
      private final Connection<MultiAttributeData> origin;
      private final Connection<MultiAttributeData> conn;
      private final long relTimestamp;
      private final long actualTimestamp;
      private final double distance;
      private final long scenarioLength;

      ShockwaveSimulation(Queue<ShockwaveSimulation> shockwaveQueue,
          Connection<MultiAttributeData> originConn,
          Connection<MultiAttributeData> currentConn, long relativeTimestamp,
          long currentTimestamp, double totalDistance,
          long totalScenarioLength) {
        this.shockwave = shockwaveQueue;
        this.origin = originConn;
        this.conn = currentConn;
        this.relTimestamp = relativeTimestamp;
        this.actualTimestamp = currentTimestamp;
        this.distance = totalDistance;
        this.scenarioLength = totalScenarioLength;
      }

      /**
       * Simulate the expanding shockwave. All events directly caused by the
       * parameters stored in the current simulation step are returned. If other
       * events still need to be calculated, then they are added to the
       * shockwave queue.
       * @param origin The previous connection
       * @param conn The current connection
       * @param relTimestamp The time of the shockwave after travelling origin
       *          (before travelling current conn)
       * @param actualTimestamp The time of the simulation before travelling
       *          over current conn
       * @param distance Total distance travelled
       * @param scenarioLength Total duration of the scenario
       * @return the events caused by this iteration of expansion.
       */
      private List<ChangeConnectionSpeedEvent> simulateExpandingShockwave() {
        // LOGGER.info("~~ Expansion " + iteration + ": handling connection from
        // "
        // + conn.from() + " to " + conn.to() + " at distance " + distance);

        final List<ChangeConnectionSpeedEvent> events = new ArrayList<>();
        @Nonnull
        final Double factor =
          behaviourSupplier.get(rng.nextLong())
            .apply(distance + conn.getLength() / 2);
        @Nonnull
        final Double speed =
          expandingSpeedSupplier.get(rng.nextLong()).apply(relTimestamp);

        long nextRelTimestamp = 0L;
        long nextActualTimestamp = 0L;

        if (speed != 0) {
          nextRelTimestamp =
            (long) (relTimestamp + conn.getLength() / speed);
          nextActualTimestamp =
            (long) (actualTimestamp + conn.getLength() / speed);
        }

        // Check Stop conditions
        // Stop if shockwave has no effect (factor == 1)
        // OR if shockwave burned out (speed == 0)
        // OR if shockwave duration reached
        // OR if next step if after scenario boundary
        // OR if cycle???
        if (factor == 1 || speed == 0
          || nextRelTimestamp >= eventDurationSupplier
            .get(rng.nextLong())
          || nextActualTimestamp >= scenarioLength
          || expansionMap.containsKey(conn)) {
          // Origin was last affected node
          leafNodes.add(origin);
          // LOGGER.info("~~ Expansion " + iteration + ": stop condition
          // reached");
          // Stop
          return events;
        }

        // Add conn
        final ChangeConnectionSpeedEvent newEvent =
          ChangeConnectionSpeedEvent.create(nextActualTimestamp,
            conn, factor);
        events.add(newEvent);

        // Keep track of events linked to connections
        if (expansionMap.containsKey(conn)) {
          // Cycle, add to existing list
          final List<ChangeConnectionSpeedEvent> eventList =
            expansionMap.get(conn);
          eventList.add(newEvent);
          expansionMap.put(conn, eventList);
        } else {
          // New connection
          expansionMap.put(conn, Lists.newArrayList(newEvent));
          affectedGraph.addConnection(conn);
        }

        // Branch out
        final Collection<Point> nextFroms = graph.get()
          .getIncomingConnections(conn.from());
        // Check dead end -> no need to continue branching
        if (nextFroms.isEmpty() || nextFroms.size() == 1
          && nextFroms.iterator().next().equals(conn.to())) {
          leafNodes.add(conn);
          // LOGGER.info("~~ Expansion " + iteration + ": dead end reached");
          return events;
        }
        // LOGGER.info("~~ Expansion " + iteration + ": Branching Factor " +
        // next_froms.size());
        for (final Point nextFrom : nextFroms) {
          if (nextFrom.equals(conn.to())) {
            // No backtracking in the other direction
            continue;
          }
          final Connection<MultiAttributeData> nextConn =
            graph.get().getConnection(nextFrom, conn.from());
          shockwave.offer(new ShockwaveSimulation(shockwave, conn, nextConn,
            nextRelTimestamp, nextActualTimestamp,
            distance + conn.getLength(), scenarioLength));
        }
        // LOGGER.info("~~ Expansion " + iteration + ": Closed Branch");
        return events;
      }

      /**
       * Simulate the receding shockwave. All events directly caused by the
       * parameters stored in the current simulation step are returned. If other
       * events still need to be calculated, then they are added to the
       * shockwave queue.
       * @param conn The connection being lifted
       * @param actualTimestamp The timestamp before lifting event
       * @param scenarioLength The total duration of the scenario
       * @return the events caused by this iteration of the receding shockwave.
       */
      private List<ChangeConnectionSpeedEvent> simulateForwardRecedingShockwave() {
        final List<ChangeConnectionSpeedEvent> events = new ArrayList<>();
        @Nonnull
        final Double factorInverse = behaviourSupplier.get(rng.nextLong())
          .apply(distance + conn.getLength() / 2);
        final Double factor = 1 / factorInverse;
        @Nonnull
        final Double speed =
          recedingSpeedSupplier.get(rng.nextLong()).apply(relTimestamp);

        long nextRelTimestamp = 0L;
        long nextActualTimestamp = 0L;

        if (speed != 0) {
          nextRelTimestamp =
            (long) (relTimestamp + conn.getLength() / speed);
          nextActualTimestamp =
            (long) (actualTimestamp + conn.getLength() / speed);
        }

        // Check Stop conditions
        // Stop if connection wasn't previously affected
        // OR if shockwave burned out (speed == 0)
        // OR if next step if after scenario boundary
        if (!affectedGraph.hasConnection(conn) || speed == 0
          || nextActualTimestamp >= scenarioLength) {
          // Stop
          return events;
        }

        // Remove conn
        final ChangeConnectionSpeedEvent newEvent =
          ChangeConnectionSpeedEvent.create(nextActualTimestamp,
            conn, factor);
        events.add(newEvent);

        // Remove track of events linked to connections
        if (expansionMap.get(conn).size() > 1) {
          // Cycle, remove only 1
          final List<ChangeConnectionSpeedEvent> eventList =
            expansionMap.get(conn);
          eventList.remove(0);
          expansionMap.put(conn, eventList);
        } else {
          // Last occurrence
          expansionMap.remove(conn);
          affectedGraph.removeConnection(conn.from(), conn.to());
        }

        // Branch out
        final Collection<Point> nextFroms = ImmutableList.copyOf(
          affectedGraph.getIncomingConnections(conn.from()));
        // Check dead end -> no need to continue branching
        if (nextFroms.isEmpty()) {
          return events;
        }
        for (final Point nextFrom : nextFroms) {
          final Connection<MultiAttributeData> nextConn =
            graph.get().getConnection(nextFrom, conn.from());
          shockwave.offer(
            new ShockwaveSimulation(shockwave, nextConn, nextConn,
              nextRelTimestamp,
              nextActualTimestamp,
              distance + conn.getLength(), scenarioLength));
        }
        return events;
      }
    }
  }
}
