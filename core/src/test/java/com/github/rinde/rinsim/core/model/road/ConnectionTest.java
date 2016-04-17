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
package com.github.rinde.rinsim.core.model.road;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;

/**
 * @author Rinde van Lon
 *
 */
public class ConnectionTest {
  /**
   * Tests correct implementation of get length.
   */
  @Test
  public void testGetLength() {
    final Point a = new Point(0, 0);
    final Point b = new Point(10, 0);

    assertEquals(10, Connection.create(a, b).getLength(), GraphRoadModel.DELTA);
    final Connection<MultiAttributeData> conn = Connection.create(a, b,
      MultiAttributeData.builder()
        .setLength(12)
        .setMaxSpeed(1d)
        .build());
    assertEquals(12, conn.getLength(), GraphRoadModel.DELTA);

    final Connection<MultiAttributeData> conn2 = Connection.create(a, b,
      MultiAttributeData.builder()
        .setMaxSpeed(1d)
        .build());
    assertEquals(10, conn2.getLength(), GraphRoadModel.DELTA);
  }
}
