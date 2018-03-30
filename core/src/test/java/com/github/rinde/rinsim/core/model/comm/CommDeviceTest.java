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
package com.github.rinde.rinsim.core.model.comm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/**
 * @author Tom Houben
 */
public class CommDeviceTest {

  static final CommUser[] COMMUSERS = {
    mock(CommUser.class),
    mock(CommUser.class),
    mock(CommUser.class)
  };

  CommDeviceBuilder[] builders;

  @Before
  public void setUp() {
    builders = new CommDeviceBuilder[COMMUSERS.length];
    final CommModel commModel = CommModel
      .builder()
      .build(CommModelTest.fakeDependencies());

    for (int i = 0; i < COMMUSERS.length; i++) {
      builders[i] = new CommDeviceBuilder(commModel,
        COMMUSERS[i]);
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBroadcastRangeTooLarge() {
    builders[0].setMaxRange(10);

    final CommDevice device = builders[0].build();
    device.broadcast(mock(MessageContents.class), 15);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBroadcastRangeNegative() {
    final CommDevice device = builders[0].build();
    device.broadcast(mock(MessageContents.class), -1);
  }

  @Test
  public void testBroadcastRangeWithoutMaxRange() {
    when(COMMUSERS[0].getPosition()).thenReturn(Optional.of(new Point(0, 0)));
    when(COMMUSERS[1].getPosition()).thenReturn(Optional.of(new Point(5, 0)));
    when(COMMUSERS[2].getPosition()).thenReturn(Optional.of(new Point(10, 0)));

    final CommDevice device1 = builders[0].build();
    final CommDevice device2 = builders[1].build();
    final CommDevice device3 = builders[2].build();

    assertEquals(0, device2.getReceivedCount());
    assertEquals(0, device3.getReceivedCount());

    device1.broadcast(mock(MessageContents.class), 7);
    device1.sendMessages();
    assertEquals(1, device2.getReceivedCount());
    assertEquals(0, device3.getReceivedCount());

  }

}
