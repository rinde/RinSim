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

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.FakeDependencyProvider;
import com.github.rinde.rinsim.core.model.comm.CommModel.CommModelEvent;
import com.github.rinde.rinsim.core.model.comm.CommModel.EventTypes;
import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.time.TimeLapseFactory;
import com.github.rinde.rinsim.event.ListenerEventHistory;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.testutil.TestUtil;
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
    model = CommModel
      .builder()
      .withDefaultDeviceReliability(1.0)
      .build(fakeDependencies());

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
    final List<Message> msgs = agent1.device().getUnreadMessages();
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
    agent1.device().send(Contents.YO, agent2);
    assertTrue(agent2.device().getUnreadMessages().isEmpty());
    // send messages
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(agent1.device().getUnreadMessages().isEmpty());
    final List<Message> msgs = agent2.device().getUnreadMessages();
    assertEquals(1, msgs.size());
    assertFalse(msgs.get(0).toString().isEmpty());
    assertSame(Contents.YO, msgs.get(0).getContents());
    assertSame(agent1, msgs.get(0).getSender());
    assertTrue(agent2.device().getUnreadMessages().isEmpty());
  }

  /**
   * Test input validation.
   */
  @Test
  public void testSendDirectMsgInputValidation() {
    boolean fail = false;
    try {
      agent1.device().send(Contents.YO, agent1);
    } catch (final IllegalArgumentException e) {
      fail = true;
    }
    assertTrue(fail);
    fail = false;
    final Agent unknown = new Agent(new Point(0, 0));
    try {
      agent1.device().send(Contents.YO, unknown);
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

    unreliable.device().send(Contents.YO, agent1);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(agent1.device().getUnreadMessages().isEmpty());

    agent1.device().send(Contents.YO, unreliable);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(unreliable.device().getUnreadMessages().isEmpty());
  }

  /**
   * Test for send message with limited range.
   */
  @Test
  public void testSendDirectMsgWithRange() {
    final Agent ranged = new RangedAgent(new Point(0, 5), 5);
    model.register(ranged);
    // is possible, is within range
    ranged.device().send(Contents.YO, agent1);
    // not possible, too far
    ranged.device().send(Contents.YO, agent3);
    // receiving messages from outside range is possible
    agent3.device().send(Contents.YO, ranged);
    model.afterTick(TimeLapseFactory.create(0, 100));

    final Message m = agent1.device().getUnreadMessages().get(0);
    assertSame(ranged, m.getSender());
    assertTrue(agent3.device().getUnreadMessages().isEmpty());
    assertSame(agent3, ranged.device().getUnreadMessages().get(0)
      .getSender());
  }

  /**
   * Tests that sending an receiving of messages is different when the user has
   * no position.
   */
  @Test
  public void testSendAndReceiveWithoutPosition() {
    final Agent rangedNoPos = new RangedAgent(null, 5);
    final Agent ranged = new RangedAgent(new Point(1, 1), 5);
    final Agent noPos = new Agent(null);
    assertFalse(rangedNoPos.getPosition().isPresent());
    model.register(rangedNoPos);
    model.register(ranged);
    model.register(noPos);

    // rangedNoPos can not send direct msg, the message will stay in its outbox
    rangedNoPos.device().send(Contents.YO, agent1);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertEquals(0, agent1.device().getReceivedCount());
    assertEquals(0, agent1.device().getUnreadCount());
    assertEquals(1, rangedNoPos.device().getOutbox().size());

    // rangedNoPos can not broadcast, the message will stay in its outbox
    rangedNoPos.device().broadcast(Contents.YO);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertEquals(0, agent1.device().getReceivedCount());
    assertEquals(0, agent2.device().getReceivedCount());
    assertEquals(0, agent3.device().getReceivedCount());
    assertEquals(0, agent4.device().getReceivedCount());
    assertEquals(0, agent5.device().getReceivedCount());
    assertEquals(0, ranged.device().getReceivedCount());
    assertEquals(0, noPos.device().getReceivedCount());
    assertEquals(2, rangedNoPos.device().getOutbox().size());
    final List<Message> msgs = rangedNoPos.device().getOutbox();
    assertFalse(msgs.get(0).isBroadcast());
    assertTrue(msgs.get(1).isBroadcast());

    // rangedNoPos can receive a direct message send by agents with unlimited
    // range
    assertEquals(0, rangedNoPos.device().getReceivedCount());
    agent1.device().send(Contents.YO, rangedNoPos);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertEquals(1, rangedNoPos.device().getReceivedCount());

    // rangedNoPos can not receive a direct message send by agents with a
    // limited range
    ranged.device().send(Contents.YO, rangedNoPos);
    assertEquals(1, ranged.device().getOutbox().size());
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertEquals(1, rangedNoPos.device().getReceivedCount());

    // rangedNoPos has a new position, in the next tick all messages still in
    // the outbox are send.
    rangedNoPos.setPosition(new Point(0, 0));
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertEquals(2, agent1.device().getReceivedCount());
    assertEquals(0, agent2.device().getReceivedCount());
    assertEquals(0, agent3.device().getReceivedCount());
    assertEquals(0, agent4.device().getReceivedCount());
    assertEquals(1, agent5.device().getReceivedCount());
  }

  /**
   * Tests what happens if an agent no longer has a position.
   */
  @Test
  public void testSenderIsRemovedDuringSend() {
    final Agent a = new Agent(new Point(0, 0));
    final Agent b = new RangedAgent(new Point(0, 0), 5);
    model.register(a);
    model.register(b);

    // a is removed during the same tick as that it sends a message, this
    // doesn't matter since it has no range, therefore it sends anyway.
    a.device().broadcast(Contents.YO);
    a.setPosition(null);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertEquals(1, agent1.device().getReceivedCount());

    // b is removed during the same tick as that is sends a message, since it is
    // a ranged device the message is not sent and will stay in the outbox.
    b.device().broadcast(Contents.YO);
    b.setPosition(null);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertEquals(1, agent1.device().getReceivedCount());
    assertEquals(1, b.device().getOutbox().size());
  }

  /**
   * Tests simple broadcast where all agents receive the message.
   */
  @Test
  public void testBroadcast() {
    agent1.device().broadcast(Contents.HELLO_WORLD);

    assertTrue(agent1.device().getUnreadMessages().isEmpty());
    assertTrue(agent2.device().getUnreadMessages().isEmpty());
    assertTrue(agent3.device().getUnreadMessages().isEmpty());
    assertTrue(agent4.device().getUnreadMessages().isEmpty());
    assertTrue(agent5.device().getUnreadMessages().isEmpty());

    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(agent1.device().getUnreadMessages().isEmpty());
    final List<Message> msgs2 = agent2.device().getUnreadMessages();
    final List<Message> msgs3 = agent3.device().getUnreadMessages();
    final List<Message> msgs4 = agent4.device().getUnreadMessages();
    final List<Message> msgs5 = agent5.device().getUnreadMessages();

    assertEquals(1, msgs2.size());
    assertEquals(Contents.HELLO_WORLD, msgs2.get(0).getContents());
    assertEquals(agent1, msgs2.get(0).getSender());
    assertEquals(msgs2, msgs3);
    assertEquals(msgs2, msgs4);
    assertEquals(msgs2, msgs5);

    assertSame(msgs2.get(0), msgs3.get(0));
    assertSame(msgs2.get(0), msgs4.get(0));
    assertSame(msgs2.get(0), msgs5.get(0));

    assertTrue(agent1.device().getUnreadMessages().isEmpty());
    assertTrue(agent2.device().getUnreadMessages().isEmpty());
    assertTrue(agent3.device().getUnreadMessages().isEmpty());
    assertTrue(agent4.device().getUnreadMessages().isEmpty());
    assertTrue(agent5.device().getUnreadMessages().isEmpty());
  }

  /**
   * Tests that the messages appear in the device in the actual send ordering,
   * which is the order in which agents have been registered in the comm model
   * and not the order of broadcast invocation.
   */
  @Test
  public void testBroadcastReceiveOrdering() {
    agent4.device().broadcast(Contents.YO);
    agent1.device().broadcast(Contents.HELLO_WORLD);
    agent2.device().broadcast(Contents.YO);
    agent1.device().broadcast(Contents.HELLO_WORLD);
    agent3.device().broadcast(Contents.YO);
    agent4.device().broadcast(Contents.YO);
    model.afterTick(TimeLapseFactory.create(0, 100));

    final List<Message> msgs = agent5.device().getUnreadMessages();
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

    unreliable.device().broadcast(Contents.YO);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(agent1.device().getUnreadMessages().isEmpty());
    assertTrue(agent2.device().getUnreadMessages().isEmpty());

    agent1.device().broadcast(Contents.YO);
    model.afterTick(TimeLapseFactory.create(0, 100));
    assertTrue(unreliable.device().getUnreadMessages().isEmpty());
    assertEquals(1, agent2.device().getUnreadMessages().size());
  }

  /**
   * Test that a broadcast only ends up at agents within the range.
   */
  @Test
  public void testBroadcastWithRange() {
    final Agent ranged = new RangedAgent(new Point(0, 5), 5);
    model.register(ranged);
    ranged.device().broadcast(Contents.YO);
    model.afterTick(TimeLapseFactory.create(0, 100));

    assertFalse(agent1.device().getUnreadMessages().isEmpty());
    assertFalse(agent2.device().getUnreadMessages().isEmpty());
    assertTrue(agent3.device().getUnreadMessages().isEmpty());
    assertFalse(agent4.device().getUnreadMessages().isEmpty());
    assertTrue(agent5.device().getUnreadMessages().isEmpty());
  }

  /**
   * Tests that comm users should create a device.
   */
  @Test
  public void testIdleCommUser() {
    boolean fail = false;
    try {
      CommModel.builder().build(fakeDependencies())
        .register(new IdleCommUser());
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

  static DependencyProvider fakeDependencies() {
    return FakeDependencyProvider.builder()
      .add(RandomModel.create(), RandomProvider.class)
      .build();
  }

  static class RangedAgent extends Agent {
    final double range;

    RangedAgent(@Nullable Point p, double r) {
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
    Optional<Point> position;
    Optional<CommDevice> commDevice;
    double reliability;

    Agent(@Nullable Point p) {
      this(p, 1d);
    }

    Agent(@Nullable Point p, double r) {
      reliability = r;
      position = Optional.fromNullable(p);
      commDevice = Optional.absent();
    }

    void setPosition(@Nullable Point p) {
      position = Optional.fromNullable(p);
    }

    CommDevice device() {
      return commDevice.get();
    }

    @Override
    public Optional<Point> getPosition() {
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
    public Optional<Point> getPosition() {
      return Optional.of(new Point(0, 0));
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {}
  }

}
