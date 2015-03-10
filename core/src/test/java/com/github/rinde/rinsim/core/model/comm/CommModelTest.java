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
package com.github.rinde.rinsim.core.model.comm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.rinde.rinsim.core.TimeLapseFactory;
import com.github.rinde.rinsim.core.model.comm.CommModel.CommModelEvent;
import com.github.rinde.rinsim.core.model.comm.CommModel.EventTypes;
import com.github.rinde.rinsim.event.ListenerEventHistory;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TestUtil;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

/**
 * @author Rinde van Lon
 *
 */
public class CommModelTest {
  /**
   * For catching exceptions.
   */
  @Rule
  public ExpectedException thrown = ExpectedException.none();

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
        .setDefaultDeviceReliability(1.0)
        .setRandomGenerator(new MersenneTwister(123L))
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

    TestUtil.testEnum(CommModel.EventTypes.class);
    TestUtil.testEnum(CommModel.QoS.class);

  }

  enum Contents implements MessageContents {
    HELLO_WORLD, YO
  }

  /**
   * Test registration of object.
   */
  @Test
  public void testRegister() {
    assertTrue(model.contains(agent1));
    boolean fail = false;
    try {
      model.register(agent1);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Unregister is not supported.
   */
  @Test(expected = UnsupportedOperationException.class)
  public void testUnregister() {
    model.unregister(mock(CommUser.class));
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
    assertFalse(msgs.get(0).toString().isEmpty());
    assertSame(Contents.YO, msgs.get(0).getContents());
    assertSame(agent1, msgs.get(0).getSender());
    assertTrue(agent2.commDevice.get().getUnreadMessages().isEmpty());
  }

  /**
   * Test input validation.
   */
  @Test
  public void testSendDirectMsgInputValidation() {
    boolean fail = false;
    try {
      agent1.commDevice.get().send(Contents.YO, agent1);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
    fail = false;
    final Agent unknown = new Agent(new Point(0, 0));
    try {
      agent1.commDevice.get().send(Contents.YO, unknown);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
  }

  /**
   * Test for unreliable direct message.
   */
  @Test
  public void testSendDirectMsgUnreliable() {
    final Agent unreliable = new Agent(new Point(5, 5), 0);
    model.register(unreliable);

    unreliable.commDevice.get().send(Contents.YO, agent1);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(agent1.commDevice.get().getUnreadMessages().isEmpty());

    agent1.commDevice.get().send(Contents.YO, unreliable);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(unreliable.commDevice.get().getUnreadMessages().isEmpty());
  }

  /**
   * Test for send message with limited range.
   */
  @Test
  public void testSendDirectMsgWithRange() {
    final Agent ranged = new RangedAgent(new Point(0, 5), 5);
    model.register(ranged);
    // is possible, is within range
    ranged.commDevice.get().send(Contents.YO, agent1);
    // not possible, too far
    ranged.commDevice.get().send(Contents.YO, agent3);
    // receiving messages from outside range is possible
    agent3.commDevice.get().send(Contents.YO, ranged);
    model.afterTick(TimeLapseFactory.create(0, 100));

    final Message m = agent1.commDevice.get().getUnreadMessages().get(0);
    assertSame(ranged, m.getSender());
    assertTrue(agent3.commDevice.get().getUnreadMessages().isEmpty());
    assertSame(agent3, ranged.commDevice.get().getUnreadMessages().get(0)
        .getSender());
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
   * Tests broadcasting from and to unreliable agents.
   */
  @Test
  public void testBroadcastUnreliable() {
    final Agent unreliable = new Agent(new Point(5, 5), 0);
    model.register(unreliable);

    unreliable.commDevice.get().broadcast(Contents.YO);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(agent1.commDevice.get().getUnreadMessages().isEmpty());
    assertTrue(agent2.commDevice.get().getUnreadMessages().isEmpty());

    agent1.commDevice.get().broadcast(Contents.YO);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(unreliable.commDevice.get().getUnreadMessages().isEmpty());
    assertEquals(1, agent2.commDevice.get().getUnreadMessages().size());
  }

  /**
   * Test that a broadcast only ends up at agents within the range.
   */
  @Test
  public void testBroadcastWithRange() {
    final Agent ranged = new RangedAgent(new Point(0, 5), 5);
    model.register(ranged);
    ranged.commDevice.get().broadcast(Contents.YO);
    model.afterTick(TimeLapseFactory.create(0, 100));

    assertFalse(agent1.commDevice.get().getUnreadMessages().isEmpty());
    assertFalse(agent2.commDevice.get().getUnreadMessages().isEmpty());
    assertTrue(agent3.commDevice.get().getUnreadMessages().isEmpty());
    assertFalse(agent4.commDevice.get().getUnreadMessages().isEmpty());
    assertTrue(agent5.commDevice.get().getUnreadMessages().isEmpty());
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

  /**
   * Tests view on users and devices.
   */
  @Test
  public void testGetUsersAndDevices() {
    final List<CommUser> users = model.getUsersAndDevices().keySet().asList();
    assertSame(agent1, users.get(0));
    assertSame(agent2, users.get(1));
    assertSame(agent3, users.get(2));
    assertSame(agent4, users.get(3));
    assertSame(agent5, users.get(4));

    assertSame(users, model.getUsersAndDevices().keySet().asList());

    final CommUser agent6 = new RangedAgent(new Point(6, 6), 4);
    model.register(agent6);
    assertSame(agent6, model.getUsersAndDevices().keySet().asList().get(5));
  }

  /**
   * Test immutable view.
   */
  @Test
  public void testImmutableGetUsersAndDevices() {
    final List<CommUser> users = model.getUsersAndDevices().keySet().asList();
    thrown.expect(UnsupportedOperationException.class);
    users.clear();
  }

  /**
   * Test event dispatching.
   */
  @Test
  public void testEvent() {
    final ListenerEventHistory history = new ListenerEventHistory();
    model.getEventAPI().addListener(history, EventTypes.ADD_COMM_USER);

    final CommUser agent6 = new RangedAgent(new Point(6, 6), 4);
    model.register(agent6);

    assertEquals(1, history.getHistory().size());
    assertTrue(history.getHistory().get(0) instanceof CommModelEvent);
    final CommModelEvent event = (CommModelEvent) history.getHistory().get(0);
    assertSame(agent6, event.getUser());
    assertSame(model, event.getIssuer());
    assertSame(agent6,
        model.getUsersAndDevices().inverse().get(event.getDevice()));
  }

  static class RangedAgent extends Agent {
    final double range;

    RangedAgent(Point p, double r) {
      super(p);
      range = r;
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
      commDevice = Optional.of(
          builder.setReliability(reliability)
              .setMaxRange(range)
              .build());
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper("Agent")
          .add("position", position)
          .add("reliability", reliability)
          .add("range", range)
          .toString();
    }
  }

  static class Agent implements CommUser {
    final Point position;
    Optional<CommDevice> commDevice;
    double reliability;

    Agent(Point p) {
      this(p, 1d);
    }

    Agent(Point p, double r) {
      reliability = r;
      position = p;
      commDevice = Optional.absent();
    }

    @Override
    public Point getPosition() {
      return position;
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
      commDevice = Optional.of(
          builder.setReliability(reliability)
              .build());
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper("Agent")
          .add("position", position)
          .add("reliability", reliability)
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
