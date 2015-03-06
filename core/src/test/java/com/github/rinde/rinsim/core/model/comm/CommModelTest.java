/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.measure.unit.SI;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.core.TimeLapseFactory;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

/**
 * @author Rinde van Lon
 *
 */
public class CommModelTest {
  @SuppressWarnings("null")
  CommModel model;
  @SuppressWarnings("null")
  Agent agent1;
  @SuppressWarnings("null")
  Agent agent2;
  @SuppressWarnings("null")
  Agent agent3;
  @SuppressWarnings("null")
  Agent agent4;
  @SuppressWarnings("null")
  Agent agent5;

  /**
   * Set up a simple and reliable world.
   */
  @Before
  public void setUp() {
    model = CommModel.builder()
        .setGlobalReliability(1.0)
        .setDistanceUnit(SI.KILOMETER)
        .build();

    agent1 = new Agent(new Point(0, 0));
    agent2 = new Agent(new Point(0, 10));
    agent3 = new Agent(new Point(5, 10));
    agent4 = new Agent(new Point(5, 5));
    agent5 = new Agent(new Point(5, 0));
    model.register(agent1);
    model.register(agent2);
    model.register(agent3);
    model.register(agent4);
    model.register(agent5);

  }

  enum Contents implements MessageContents {
    HELLO_WORLD, YO
  }

  /**
   * Tests that the returned list is unmodifiable.
   */
  @Test
  public void testUnmodifiableUnreadMessages() {
    final List<Message> msgs = agent1.commDevice.get().getUnreadMessages();
    boolean fail = false;
    try {
      msgs.clear();
    } catch (final UnsupportedOperationException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Tests the correct sending and receiving of a direct message.
   */
  @Test
  public void testSendDirectMsg() {
    agent1.commDevice.get().send(Contents.YO, agent2);
    assertTrue(agent2.commDevice.get().getUnreadMessages().isEmpty());
    // send messages
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(agent1.commDevice.get().getUnreadMessages().isEmpty());
    final List<Message> msgs = agent2.commDevice.get().getUnreadMessages();
    assertEquals(1, msgs.size());
    assertSame(Contents.YO, msgs.get(0).getContents());
    assertSame(agent1, msgs.get(0).getSender());
    assertTrue(agent2.commDevice.get().getUnreadMessages().isEmpty());
  }

  /**
   * Tests simple broadcast where all agents receive the message.
   */
  @Test
  public void testBroadcast() {
    agent1.commDevice.get().broadcast(Contents.HELLO_WORLD);

    assertTrue(agent1.commDevice.get().getUnreadMessages().isEmpty());
    assertTrue(agent2.commDevice.get().getUnreadMessages().isEmpty());
    assertTrue(agent3.commDevice.get().getUnreadMessages().isEmpty());
    assertTrue(agent4.commDevice.get().getUnreadMessages().isEmpty());
    assertTrue(agent5.commDevice.get().getUnreadMessages().isEmpty());

    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(agent1.commDevice.get().getUnreadMessages().isEmpty());
    final List<Message> msgs2 = agent2.commDevice.get().getUnreadMessages();
    final List<Message> msgs3 = agent3.commDevice.get().getUnreadMessages();
    final List<Message> msgs4 = agent4.commDevice.get().getUnreadMessages();
    final List<Message> msgs5 = agent5.commDevice.get().getUnreadMessages();

    assertEquals(1, msgs2.size());
    assertEquals(Contents.HELLO_WORLD, msgs2.get(0).getContents());
    assertEquals(agent1, msgs2.get(0).getSender());
    assertEquals(msgs2, msgs3);
    assertEquals(msgs2, msgs4);
    assertEquals(msgs2, msgs5);

    assertSame(msgs2.get(0), msgs3.get(0));
    assertSame(msgs2.get(0), msgs4.get(0));
    assertSame(msgs2.get(0), msgs5.get(0));

    assertTrue(agent1.commDevice.get().getUnreadMessages().isEmpty());
    assertTrue(agent2.commDevice.get().getUnreadMessages().isEmpty());
    assertTrue(agent3.commDevice.get().getUnreadMessages().isEmpty());
    assertTrue(agent4.commDevice.get().getUnreadMessages().isEmpty());
    assertTrue(agent5.commDevice.get().getUnreadMessages().isEmpty());
  }

  /**
   * Tests that the messages appear in the device in the actual send ordering,
   * which is the order in which agents have been registered in the comm model
   * and not the order of broadcast invocation.
   */
  @Test
  public void testBroadcastReceiveOrdering() {
    agent4.commDevice.get().broadcast(Contents.YO);
    agent1.commDevice.get().broadcast(Contents.HELLO_WORLD);
    agent2.commDevice.get().broadcast(Contents.YO);
    agent1.commDevice.get().broadcast(Contents.HELLO_WORLD);
    agent3.commDevice.get().broadcast(Contents.YO);
    agent4.commDevice.get().broadcast(Contents.YO);
    model.afterTick(TimeLapseFactory.create(0, 100));

    final List<Message> msgs = agent5.commDevice.get().getUnreadMessages();
    assertSame(agent1, msgs.get(0).getSender());
    assertSame(agent1, msgs.get(1).getSender());
    assertSame(agent2, msgs.get(2).getSender());
    assertSame(agent3, msgs.get(3).getSender());
    assertSame(agent4, msgs.get(4).getSender());
    assertSame(agent4, msgs.get(5).getSender());
  }

  /**
   * Tests that comm users should create a device.
   */
  @Test
  public void testIdleCommUser() {
    boolean fail = false;
    try {
      CommModel.builder().build().register(new IdleCommUser());
    } catch (final IllegalStateException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  static class Agent implements CommUser {
    final Point position;
    Optional<CommDevice> commDevice;

    Agent(Point p) {
      position = p;
      commDevice = Optional.absent();
    }

    @Override
    public Point getPosition() {
      return position;
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
      commDevice = Optional.of(builder.build());
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper("Agent")
          .add("position", position)
          .toString();
    }
  }

  static class IdleCommUser implements CommUser {
    @Override
    public Point getPosition() {
      return new Point(0, 0);
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {}
  }
}
