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
package com.github.rinde.rinsim.core.model.road;

import static com.github.rinde.rinsim.core.model.road.RoadModelBuilders.dynamicGraph;
import static com.github.rinde.rinsim.core.model.road.RoadModelBuilders.plane;
import static com.github.rinde.rinsim.core.model.road.RoadModelBuilders.staticGraph;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.junit.Test;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.CachedGraphRMB;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.CollisionGraphRMB;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.DynamicGraphRMB;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.PlaneRMB;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.StaticGraphRMB;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.MultimapGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.testutil.TestUtil;

/**
 * @author Rinde van Lon
 *
 */
public class RoadModelBuildersTest {

  /**
   * Tests that units are correctly set.
   */
  @Test
  public void testAbstractRMBUnits() {
    final PlaneRoadModel prm = RoadModelBuilders.plane()
      .setDistanceUnit(NonSI.LIGHT_YEAR)
      .setSpeedUnit(NonSI.C)
      .build(mock(DependencyProvider.class));

    assertThat(prm.getDistanceUnit()).isEqualTo(NonSI.LIGHT_YEAR);
    assertThat(prm.getSpeedUnit()).isEqualTo(NonSI.C);
  }

  /**
   * Tests that equals is correctly implemented.
   */
  @Test
  public void testEquals() {
    TestUtil.testPrivateConstructor(RoadModelBuilders.class);

    final PlaneRMB plane = plane();
    final StaticGraphRMB stat = staticGraph(new TableGraph<>());
    final DynamicGraphRMB dynamic = dynamicGraph(new ListenableGraph<>(
      new TableGraph<>()));
    final CollisionGraphRMB coll = dynamicGraph(
      new ListenableGraph<>(new TableGraph<>()))
      .avoidCollisions();
    final CachedGraphRMB cach = staticGraph(new TableGraph<>())
      .useCache();

    final List<?> list = asList(plane, stat, dynamic, coll, cach);
    final Set<Object> set = new LinkedHashSet<>();
    for (final Object one : list) {
      for (final Object another : list) {
        set.add(one);
        set.add(another);
        if (one != another) {
          assertThat(one).isNotEqualTo(another);
          assertThat(another).isNotEqualTo(one);
        } else {
          assertThat(one).isEqualTo(another);
          assertThat(another).isEqualTo(one);
        }
        assertThat(one).isEqualTo(one);
      }
    }

    assertThat(stat).isNotEqualTo(
      plane().setMaxPoint(new Point(7, 7)));
    assertThat(stat).isEqualTo(
      staticGraph(new MultimapGraph<>()));
    assertThat(plane).isEqualTo(RoadModelBuilders.plane());
    assertThat(plane).isNotEqualTo(
      RoadModelBuilders.plane().setDistanceUnit(SI.CENTIMETER));

    assertThat(coll).isNotEqualTo(
      dynamicGraph(new ListenableGraph<>(new TableGraph<>()))
        .avoidCollisions()
        .setVehicleLength(7d)
      );
    assertThat(coll).isNotEqualTo(
      dynamicGraph(new ListenableGraph<>(new TableGraph<>()))
        .avoidCollisions()
        .setDistanceUnit(NonSI.YARD)
      );

    assertThat((Iterable<?>) set).containsExactly(plane, stat, dynamic, coll,
      cach);
  }
}
