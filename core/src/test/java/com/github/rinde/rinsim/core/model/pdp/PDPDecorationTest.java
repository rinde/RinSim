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
package com.github.rinde.rinsim.core.model.pdp;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

public class PDPDecorationTest {

  @Test
  public void decoration() {
    final PDPModel defaultPDPModel = DefaultPDPModel.builder()
        .build(mock(DependencyProvider.class));
    assertSame(defaultPDPModel, defaultPDPModel.self);

    final PDPModel forwardA = new ForwardingA(defaultPDPModel);
    assertSame(forwardA, defaultPDPModel.self);

    final PDPModel forwardB = new ForwardingB(forwardA);
    assertSame(forwardB, defaultPDPModel.self);

    final PDPModel forwardC = new ForwardingC(forwardB);
    assertSame(forwardC, forwardA.self);
    assertSame(forwardC, forwardB.self);
    assertSame(forwardC, forwardC.self);
    assertSame(forwardC, defaultPDPModel.self);
  }

  /**
   * Tests whether decorating an already initialized PDPModel fails correctly.
   */
  @SuppressWarnings("unused")
  @Test
  public void decorationFail() {
    final PDPModel defaultPDPModel = DefaultPDPModel.builder()
        .build(mock(DependencyProvider.class));
    assertSame(defaultPDPModel, defaultPDPModel.self);

    final PDPModel forwardA = new ForwardingA(defaultPDPModel);
    assertSame(forwardA, defaultPDPModel.self);

    forwardA.register(new Depot(new Point(0, 0)) {
      @Override
      public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}
    });

    boolean fail = false;
    try {
      new ForwardingA(forwardA);
    } catch (final IllegalStateException e) {
      fail = true;
    }
    assertTrue(fail);

    boolean fail2 = false;
    try {
      new ForwardingA(defaultPDPModel);
    } catch (final IllegalStateException e) {
      fail2 = true;
    }
    assertTrue(fail2);
  }

  static class ForwardingA extends ForwardingPDPModel {
    ForwardingA(PDPModel deleg) {
      super(deleg);
    }
  }

  static class ForwardingB extends ForwardingPDPModel {
    ForwardingB(PDPModel deleg) {
      super(deleg);
    }
  }

  static class ForwardingC extends ForwardingPDPModel {
    ForwardingC(PDPModel deleg) {
      super(deleg);
    }
  }

}
