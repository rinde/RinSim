/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.pdptw.common.ChangeConnectionSpeedEvent;
import com.github.rinde.rinsim.scenario.generator.DynamicSpeeds.DynamicSpeedGenerator;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Test for {@link DynamicSpeeds}.
 * @author Vincent Van Gestel
 *
 */
public class DynamicSpeedsTest {

  static final double DELTA = 0.0001;
  static final double TEN_KM_H_IN_M_MILLIS = 0.002777777777777778d;

  @SuppressWarnings("null")
  Graph<MultiAttributeData> graph;

  @SuppressWarnings("null")
  DynamicSpeeds.Builder builder;

  long seed;
  long scenarioLength;

  final long MINUTE = 60000;

  final Function<Double, Double> HALF = new Function<Double, Double>() {

    @Override
    public Double apply(@SuppressWarnings("null") Double t) {
      return 0.5;
    }
  };

  final Function<Double, Double> LINEAR_DESCENDING =
    new Function<Double, Double>() {

      @Override
      public Double apply(@Nonnull Double input) {
        return Math.max(0, Math.min(0.8d / 1000d * input + 0.2d, 1));
      }
    };

  final Function<Long, Double> TEN_KM_H = new Function<Long, Double>() {
    @Override
    public Double apply(@SuppressWarnings("null") Long input) {
      return TEN_KM_H_IN_M_MILLIS;
    }
  };

  final Function<Long, Double> FROM_TEN_KM_H_DESCENDING =
    new Function<Long, Double>() {
      @Override
      public Double apply(@Nonnull Long input) {
        return Math.max(0,
          TEN_KM_H_IN_M_MILLIS - TEN_KM_H_IN_M_MILLIS * input / (6 * MINUTE));
      }
    };

  final Function<Long, Double> FROM_FIVE_KM_H_DESCENDING =
    new Function<Long, Double>() {
      @Override
      public Double apply(@Nonnull Long input) {
        return Math.max(0,
          TEN_KM_H_IN_M_MILLIS / 2
            - TEN_KM_H_IN_M_MILLIS / 2 * input / (6 * MINUTE));
      }
    };

  @Before
  public void setUp() throws Exception {
    graph = new TableGraph<>();
    builder = DynamicSpeeds.builder()
      .numberOfShockwaves(StochasticSuppliers.constant(1));
    seed = 123;
    scenarioLength = 20 * MINUTE;
  }

  /**
   * Test a simple straight line for correct ordering, timing and factor
   * adjustment
   */
  @SuppressWarnings("unchecked")
  @Test
  public void straightLineShockwaveTest() {
    Point A, B, C, D, E;
    A = new Point(0, 0);
    B = new Point(0, 250);
    C = new Point(0, 500);
    D = new Point(0, 750);
    E = new Point(0, 1000);

    Connection<MultiAttributeData> connA, connB, connC, connD;
    connA = Connection.create(A, B,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connB = Connection.create(B, C,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connC = Connection.create(C, D,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connD = Connection.create(D, E,
      MultiAttributeData.builder().setMaxSpeed(100).build());

    graph.addConnections(Lists.newArrayList(connA, connB, connC, connD));

    final DynamicSpeedGenerator gen = builder.withGraph(graph)
      .startConnections(StochasticSuppliers.constant(connD))
      .shockwaveDurations(StochasticSuppliers.constant(10 * MINUTE))
      .shockwaveBehaviour(StochasticSuppliers.constant(HALF))
      .shockwaveExpandingSpeed(StochasticSuppliers.constant(TEN_KM_H))
      .shockwaveRecedingSpeed(StochasticSuppliers.constant(TEN_KM_H))
      .build();

    final List<ChangeConnectionSpeedEvent> events = Lists.newArrayList(
      gen.generate(123, scenarioLength));

    assertEquals(8, events.size());

    /**
     * Shockwave travels at 10 Km/h, the total distance if 1 Km. every 1.5
     * minute, a connection is crossed
     */
    final long[] timings = new long[] {(long) (1.5 * MINUTE), 3 * MINUTE,
      (long) (4.5 * MINUTE), 6 * MINUTE,
      (long) (11.5 * MINUTE), 13 * MINUTE, (long) (14.5 * MINUTE), 16 * MINUTE};
    /**
     * Expanding shockwave halves the speed, receding shockwave doubles
     */
    final double[] factors = new double[] {0.5, 0.5, 0.5, 0.5, 2, 2, 2, 2};
    /**
     * Shockwave moves from connD -> connA
     */
    final List<Connection<MultiAttributeData>> conns =
      new LinkedList<>();
    conns.add(connD);
    conns.add(connC);
    conns.add(connB);
    conns.add(connA);
    conns.add(connD);
    conns.add(connC);
    conns.add(connB);
    conns.add(connA);

    for (int i = 0; i < events.size(); i++) {
      final ChangeConnectionSpeedEvent event = events.get(i);
      assertEquals(conns.get(i).from(), event.getFrom());
      assertEquals(conns.get(i).to(), event.getTo());
      assertEquals(timings[i], event.getTime());
      assertEquals(factors[i], event.getFactor(), DELTA);
    }
  }

  /**
   * Tests if behaviour is applied correctly using linear functions, shockwave
   * dies out before reaching final connection
   */
  @SuppressWarnings("unchecked")
  @Test
  public void behaviourDistanceTest() {
    Point A, B, C, D, E, F;
    A = new Point(0, 0);
    B = new Point(0, 250);
    C = new Point(0, 500);
    D = new Point(0, 750);
    E = new Point(0, 1000);
    F = new Point(0, 1250);

    Connection<MultiAttributeData> connA, connB, connC, connD, connE;
    connA = Connection.create(A, B,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connB = Connection.create(B, C,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connC = Connection.create(C, D,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connD = Connection.create(D, E,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connE = Connection.create(E, F,
      MultiAttributeData.builder().setMaxSpeed(100).build());

    graph.addConnections(Lists.newArrayList(connA, connB, connC, connD, connE));

    final DynamicSpeedGenerator gen = builder.withGraph(graph)
      .startConnections(StochasticSuppliers.constant(connE))
      .shockwaveDurations(StochasticSuppliers.constant(10 * MINUTE))
      .shockwaveBehaviour(StochasticSuppliers.constant(LINEAR_DESCENDING))
      .shockwaveExpandingSpeed(StochasticSuppliers.constant(TEN_KM_H))
      .shockwaveRecedingSpeed(StochasticSuppliers.constant(TEN_KM_H))
      .build();

    final List<ChangeConnectionSpeedEvent> events = Lists.newArrayList(
      gen.generate(123, scenarioLength));

    assertEquals(8, events.size());

    /**
     * Expanding shockwave behaves like a linear function starting at 0.2 moving
     * up to 1 (after 1000 meters)
     */
    final double[] factors =
      new double[] {0.3, 0.5, 0.7, 0.9, 3.333333333, 2, 1.428571429, 1.111111};
    /**
     * Shockwave moves from connE -> connA
     */
    final List<Connection<MultiAttributeData>> conns =
      new LinkedList<>();
    conns.add(connE);
    conns.add(connD);
    conns.add(connC);
    conns.add(connB);
    conns.add(connE);
    conns.add(connD);
    conns.add(connC);
    conns.add(connB);

    for (int i = 0; i < events.size(); i++) {
      final ChangeConnectionSpeedEvent event = events.get(i);
      assertEquals(conns.get(i).from(), event.getFrom());
      assertEquals(conns.get(i).to(), event.getTo());
      assertEquals(factors[i], event.getFactor(), DELTA);
    }
  }

  /**
   * Tests the speed of the shockwave given a linear function, giving correct
   * timings and dying out
   */
  @SuppressWarnings("unchecked")
  @Test
  public void shockwaveSpeedTest() {
    Point A, B, C, D, E;
    A = new Point(0, 0);
    B = new Point(0, 250);
    C = new Point(0, 500);
    D = new Point(0, 750);
    E = new Point(0, 1000);

    Connection<MultiAttributeData> connA, connB, connC, connD;
    connA = Connection.create(A, B,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connB = Connection.create(B, C,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connC = Connection.create(C, D,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connD = Connection.create(D, E,
      MultiAttributeData.builder().setMaxSpeed(100).build());

    graph.addConnections(Lists.newArrayList(connA, connB, connC, connD));

    final DynamicSpeedGenerator gen = builder.withGraph(graph)
      .startConnections(StochasticSuppliers.constant(connD))
      .shockwaveDurations(StochasticSuppliers.constant(10 * MINUTE))
      .shockwaveBehaviour(StochasticSuppliers.constant(HALF))
      .shockwaveExpandingSpeed(
        StochasticSuppliers.constant(FROM_TEN_KM_H_DESCENDING))
      .shockwaveRecedingSpeed(
        StochasticSuppliers.constant(FROM_FIVE_KM_H_DESCENDING))
      .build();

    final List<ChangeConnectionSpeedEvent> events = Lists.newArrayList(
      gen.generate(123, scenarioLength));

    assertEquals(5, events.size());

    /**
     * Shockwave speed starts at 10 Km/h and decreases linearly to 0 after 6
     * minutes, receding speed starts at 5 Km/h. Both wouldn't have enough speed
     * left to complete their final connection, but due to discretization, they
     * can
     */
    final long[] timings =
      new long[] {(long) (1.5 * MINUTE), (long) (3.5 * MINUTE),
        (long) (7.1 * MINUTE),
        13 * MINUTE, 19 * MINUTE};
    /**
     * Shockwave moves from connD -> connA
     */
    final List<Connection<MultiAttributeData>> conns =
      new LinkedList<>();
    conns.add(connD);
    conns.add(connC);
    conns.add(connB);
    conns.add(connD);
    conns.add(connC);

    for (int i = 0; i < events.size(); i++) {
      final ChangeConnectionSpeedEvent event = events.get(i);
      assertEquals(conns.get(i).from(), event.getFrom());
      assertEquals(conns.get(i).to(), event.getTo());
      assertEquals(timings[i], event.getTime());
    }
  }

  /**
   * Tests timing related boundaries (shockwave duration/scenario boundary)
   */
  @SuppressWarnings("unchecked")
  @Test
  public void shockwaveBoundaryTest() {
    Point A, B, C, D, E;
    A = new Point(0, 0);
    B = new Point(0, 250);
    C = new Point(0, 500);
    D = new Point(0, 750);
    E = new Point(0, 1000);

    Connection<MultiAttributeData> connA, connB, connC, connD;
    connA = Connection.create(A, B,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connB = Connection.create(B, C,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connC = Connection.create(C, D,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connD = Connection.create(D, E,
      MultiAttributeData.builder().setMaxSpeed(100).build());

    graph.addConnections(Lists.newArrayList(connA, connB, connC, connD));

    final DynamicSpeedGenerator gen = builder.withGraph(graph)
      .startConnections(StochasticSuppliers.constant(connD))
      .shockwaveDurations(StochasticSuppliers.constant(5 * MINUTE))
      .shockwaveBehaviour(StochasticSuppliers.constant(HALF))
      .shockwaveExpandingSpeed(
        StochasticSuppliers.constant(TEN_KM_H))
      .shockwaveRecedingSpeed(
        StochasticSuppliers.constant(TEN_KM_H))
      .build();

    scenarioLength = 10 * MINUTE;

    final List<ChangeConnectionSpeedEvent> events = Lists.newArrayList(
      gen.generate(123, scenarioLength));

    assertEquals(6, events.size());

    /**
     * Shockwave speed remains at 10 Km/h, but duration limits shockwave to 5
     * minutes and total scenario limits receding shockwave to 10 minutes
     */
    final long[] timings = new long[] {(long) (1.5 * MINUTE), 3 * MINUTE,
      (long) (4.5 * MINUTE),
      (long) (6.5 * MINUTE), 8 * MINUTE, (long) (9.5 * MINUTE)};
    /**
     * Shockwave moves from connD -> connA
     */
    final List<Connection<MultiAttributeData>> conns =
      new LinkedList<>();
    conns.add(connD);
    conns.add(connC);
    conns.add(connB);
    conns.add(connD);
    conns.add(connC);
    conns.add(connB);

    for (int i = 0; i < events.size(); i++) {
      final ChangeConnectionSpeedEvent event = events.get(i);
      assertEquals(conns.get(i).from(), event.getFrom());
      assertEquals(conns.get(i).to(), event.getTo());
      assertEquals(timings[i], event.getTime());
    }
  }

  /**
   * Tests behaviour of shockwaves without cycles in the expansion
   */
  @SuppressWarnings("unchecked")
  @Test
  public void shockwaveCycleTest() {
    Point A, B, C, D, E, Cprime, Dprime;
    A = new Point(0, 0);
    B = new Point(0, 250);
    C = new Point(0, 500);
    D = new Point(0, 750);
    E = new Point(0, 1000);
    Cprime = new Point(1, 500);
    Dprime = new Point(1, 750);

    Connection<MultiAttributeData> connA, connB, connC, connD;
    Connection<MultiAttributeData> connCprime, connDprime, connDD;
    connA = Connection.create(A, B,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connB = Connection.create(B, C,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connC = Connection.create(C, D,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connD = Connection.create(D, E,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connCprime = Connection.create(Cprime, D,
      MultiAttributeData.builder().setLength(250).setMaxSpeed(100).build());
    connDprime = Connection.create(Dprime, Cprime,
      MultiAttributeData.builder().setLength(250).setMaxSpeed(100).build());
    connDD = Connection.create(D, Dprime,
      MultiAttributeData.builder().setLength(250).setMaxSpeed(100).build());

    graph.addConnections(Lists.newArrayList(connA, connB, connC, connD));
    graph.addConnections(Lists.newArrayList(connCprime, connDprime, connDD));

    final DynamicSpeedGenerator gen = builder.withGraph(graph)
      .startConnections(StochasticSuppliers.constant(connD))
      .shockwaveDurations(StochasticSuppliers.constant(10 * MINUTE))
      .shockwaveBehaviour(StochasticSuppliers.constant(HALF))
      .shockwaveExpandingSpeed(StochasticSuppliers.constant(TEN_KM_H))
      .shockwaveRecedingSpeed(StochasticSuppliers.constant(TEN_KM_H))
      .build();

    final List<ChangeConnectionSpeedEvent> events = Lists.newArrayList(
      gen.generate(123, scenarioLength));

    assertEquals(14, events.size());

    /**
     * Shockwave travels at 10 Km/h, the total distance if 1 Km. every 1.5
     * minute, a connection is crossed. After connD, it branches into a cycle
     */
    final long[] timings =
      new long[] {(long) (1.5 * MINUTE), 3 * MINUTE, 3 * MINUTE,
        (long) (4.5 * MINUTE), (long) (4.5 * MINUTE), 6 * MINUTE, 6 * MINUTE,

        (long) (11.5 * MINUTE), 13 * MINUTE, 13 * MINUTE,
        (long) (14.5 * MINUTE), (long) (14.5 * MINUTE), 16 * MINUTE,
        16 * MINUTE};
    /**
     * Expanding shockwave halves the speed, receding shockwave doubles
     */
    final double[] factors =
      new double[] {0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 2, 2, 2, 2, 2, 2, 2};

    for (int i = 0; i < events.size(); i++) {
      final ChangeConnectionSpeedEvent event = events.get(i);
      assertEquals(timings[i], event.getTime());
      assertEquals(factors[i], event.getFactor(), DELTA);
    }
  }

  /**
   * Tests branching in the context of bidirectional roads
   */
  @SuppressWarnings("unchecked")
  @Test
  public void shockwaveBidirectionalTest() {
    Point A, B;
    A = new Point(0, 0);
    B = new Point(0, 250);

    Connection<MultiAttributeData> connA, connB;
    connA = Connection.create(A, B,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    connB = Connection.create(B, A,
      MultiAttributeData.builder().setMaxSpeed(100).build());
    graph.addConnections(Lists.newArrayList(connA, connB));

    final DynamicSpeedGenerator gen = builder.withGraph(graph)
      .startConnections(StochasticSuppliers.constant(connA))
      .shockwaveDurations(StochasticSuppliers.constant(10 * MINUTE))
      .shockwaveBehaviour(StochasticSuppliers.constant(HALF))
      .shockwaveExpandingSpeed(StochasticSuppliers.constant(TEN_KM_H))
      .shockwaveRecedingSpeed(StochasticSuppliers.constant(TEN_KM_H))
      .build();

    final List<ChangeConnectionSpeedEvent> events = Lists.newArrayList(
      gen.generate(123, scenarioLength));

    assertEquals(2, events.size());

    final long[] timings =
      new long[] {(long) (1.5 * MINUTE), (long) (11.5 * MINUTE)};
    /**
     * Expanding shockwave halves the speed, receding shockwave doubles
     */
    final double[] factors = new double[] {0.5, 2};
    /**
     * Shockwave moves from connD -> connA
     */
    final List<Connection<MultiAttributeData>> conns =
      new LinkedList<>();
    conns.add(connA);
    conns.add(connA);

    for (int i = 0; i < events.size(); i++) {
      final ChangeConnectionSpeedEvent event = events.get(i);
      assertEquals(conns.get(i).from(), event.getFrom());
      assertEquals(conns.get(i).to(), event.getTo());
      assertEquals(timings[i], event.getTime());
      assertEquals(factors[i], event.getFactor(), DELTA);
    }
  }

}
