/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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

import org.junit.Ignore;
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
import com.google.common.testing.EqualsTester;

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
      .withDistanceUnit(NonSI.LIGHT_YEAR)
      .withSpeedUnit(NonSI.C)
      .build(mock(DependencyProvider.class));

    assertThat(prm.getDistanceUnit()).isEqualTo(NonSI.LIGHT_YEAR);
    assertThat(prm.getSpeedUnit()).isEqualTo(NonSI.C);
  }

  /**
   * Tests that min and max points are correctly set (important because they are
   * both points).
   */
  @Test
  public void testPlaneRMB() {
    final PlaneRMB b = RoadModelBuilders.plane()
      .withMinPoint(new Point(1, 1))
      .withMaxPoint(new Point(2, 2));

    assertThat(b.getMin()).isEqualTo(new Point(1, 1));
    assertThat(b.getMax()).isEqualTo(new Point(2, 2));

    final PlaneRoadModel prm = b.build(mock(DependencyProvider.class));
    assertThat(prm.min).isEqualTo(b.getMin());
    assertThat(prm.max).isEqualTo(b.getMax());
  }

  /**
   * Tests that vehicle length and min distance are set correctly (important
   * because they are both doubles).
   */
  @Test
  @Ignore
  public void testCollisionGraphRMB() {
    final CollisionGraphRMB b = RoadModelBuilders
      .dynamicGraph(new ListenableGraph<>(
        new TableGraph<>()))
      .withCollisionAvoidance()
      .withVehicleLength(78d)
      .withMinDistance(3d);

    final double precision = 0.0000001;
    assertThat(b.getMinDistance()).isWithin(precision).of(3d);
    assertThat(b.getVehicleLength()).isWithin(precision).of(78d);

    final CollisionGraphRoadModel m =
      b.build(mock(DependencyProvider.class));
    assertThat(m.getMinConnLength()).isWithin(precision)
      .of(b.getMinDistance());
    assertThat(m.getVehicleLength()).isWithin(precision)
      .of(b.getVehicleLength());
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
        .withCollisionAvoidance();
    final CachedGraphRMB cach = staticGraph(new TableGraph<>())
      .withCache();

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
        new EqualsTester().addEqualityGroup(one).testEquals();
      }
    }

    assertThat(stat).isNotEqualTo(
      plane().withMaxPoint(new Point(7, 7)));
    assertThat(stat).isEqualTo(
      staticGraph(new MultimapGraph<>()));
    assertThat(plane).isEqualTo(RoadModelBuilders.plane());
    assertThat(plane).isNotEqualTo(
      RoadModelBuilders.plane().withDistanceUnit(SI.CENTIMETER));

    assertThat(coll).isNotEqualTo(
      dynamicGraph(new ListenableGraph<>(new TableGraph<>()))
        .withCollisionAvoidance()
        .withVehicleLength(7d));
    assertThat(coll).isNotEqualTo(
      dynamicGraph(new ListenableGraph<>(new TableGraph<>()))
        .withCollisionAvoidance()
        .withDistanceUnit(NonSI.YARD));

    assertThat(set).containsExactly(plane, stat, dynamic, coll,
      cach);
  }
}
