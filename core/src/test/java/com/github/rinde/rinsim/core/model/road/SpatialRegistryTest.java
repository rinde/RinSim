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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.geom.Point;

public class SpatialRegistryTest {

  String A = "A";
  String B = "B";
  String C = "C";
  String C2 = "C2";
  String D = "D";
  String E = "E";

  SpatialRegistry<String> reg;

  @Before
  public void setUp() {
    reg = MapSpatialRegistry.create();
  }

  @Test
  public void findNearestObjectsTest() {

    assertThat(reg.findNearestObjects(new Point(0, 0), 10)).isEmpty();

    reg.addAt(A, new Point(1, 0));

    assertThat(reg.findNearestObjects(new Point(0, 0), 1)).containsExactly(A);
    assertThat(reg.findNearestObjects(new Point(0, 0), 3)).containsExactly(A);

    reg.addAt(B, new Point(2, 0));
    reg.addAt(C, new Point(3, 0));
    reg.addAt(D, new Point(9, 0));
    assertThat(reg.findNearestObjects(new Point(4, 0), 1)).containsExactly(C);
    assertThat(reg.findNearestObjects(new Point(4, 0), 2))
      .containsExactly(C, B);
    reg.addAt(C2, new Point(3, 0));
    assertThat(reg.findNearestObjects(new Point(4, 0), 1)).containsExactly(C);

  }

  @Test
  public void findObjectsInRectTest() {
    assertThat(reg.findObjectsInRect(new Point(0, 0), new Point(1, 1)))
      .isEmpty();

    reg.addAt(A, new Point(1, 0));
    reg.addAt(B, new Point(0, 0));
    reg.addAt(C, new Point(1.1, 0));
    reg.addAt(D, new Point(.5, .5));
    reg.addAt(E, new Point(5, 5));
    assertThat(reg.findObjectsInRect(new Point(0, 0), new Point(1, 1)))
      .containsExactly(A, B, D);
  }

  @Test
  public void findObjectsWithinRadiusTest() {
    assertThat(reg.findObjectsWithinRadius(new Point(0, 0), 5)).isEmpty();

    reg.addAt(A, new Point(1, 0));
    reg.addAt(B, new Point(2, 0));
    reg.addAt(C, new Point(3, 0));
    reg.addAt(D, new Point(0, 4));
    reg.addAt(E, new Point(5, 5));

    assertThat(reg.findObjectsWithinRadius(new Point(0, 0), 1)).isEmpty();
    assertThat(reg.findObjectsWithinRadius(new Point(0, 0), 2))
      .containsExactly(A);
    assertThat(reg.findObjectsWithinRadius(new Point(0, 0), 3))
      .containsExactly(A, B);
    assertThat(reg.findObjectsWithinRadius(new Point(0, 0), 4))
      .containsExactly(A, B, C);
    assertThat(reg.findObjectsWithinRadius(new Point(0, 0), 5))
      .containsExactly(A, B, C, D);
    assertThat(reg.findObjectsWithinRadius(new Point(0, 0), 10))
      .containsExactly(A, B, C, D, E);

  }
}
