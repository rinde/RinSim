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
package com.github.rinde.rinsim.core.model.communication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * A version optimized for broadcasting
 * 
 * @author Bartosz Michalik 
 * @since 2.0
 */
public class CommunicationModel2 extends CommunicationModel {

  private Multimap<CommunicationUser, SimpleEntry<Message, Class<? extends CommunicationUser>>> toBroadcast;

  private final Comparator<CommunicationUser> xComp = new Comparator<CommunicationUser>() {
    @Override
    public int compare(CommunicationUser o1, CommunicationUser o2) {
      double diff = o1.getPosition().x - o2.getPosition().x;
      if (diff != 0) {
        return diff > 0 ? 1 : -1;
      }

      return o1.hashCode() - o2.hashCode();
    }
  };

  private final Comparator<CommunicationUser> yComp = new Comparator<CommunicationUser>() {
    @Override
    public int compare(CommunicationUser o1, CommunicationUser o2) {
      double diff = o1.getPosition().y - o2.getPosition().y;
      if (diff != 0) {
        return diff > 0 ? 1 : -1;
      }

      return o1.hashCode() - o2.hashCode();
    }
  };

  public CommunicationModel2(RandomGenerator generator) {
    super(generator);
    toBroadcast = ArrayListMultimap.create();
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    broadcast();
    super.afterTick(timeLapse);
  }

  private void broadcast() {

    final ArrayList<CommunicationUser> xSorted = new ArrayList<CommunicationUser>(
        users);

    Collections.sort(xSorted, xComp);
    Multimap<CommunicationUser, SimpleEntry<Message, Class<? extends CommunicationUser>>> cache = toBroadcast;
    toBroadcast = ArrayListMultimap.create();

    for (CommunicationUser sender : cache.keySet()) {
      ArrayList<CommunicationUser> toCheck = select(xSorted, sender, true);

      CanCommunicate predicate = new CanCommunicate(sender);
      broadcast2(sender, cache.get(sender), predicate, toCheck);
    }
  }

  private ArrayList<CommunicationUser> select(
      final ArrayList<CommunicationUser> from, CommunicationUser sender,
      final boolean isX) {
    ArrayList<CommunicationUser> toCheck = new ArrayList<CommunicationUser>(
        1024);
    Comparator<CommunicationUser> c = null;
    double distance;
    if (isX) {
      c = xComp;
      distance = sender.getPosition().x - sender.getRadius();
    } else {
      c = yComp;
      distance = sender.getPosition().y - sender.getRadius();
    }
    int idx = Collections.binarySearch(from, sender, c);
    for (int i = idx; i >= 0; --i) {
      CommunicationUser user = from.get(i);
      double pos = (isX ? user.getPosition().x : user.getPosition().y);

      if (pos >= distance) {
        toCheck.add(user);
      } else {
        break;
      }
    }
    if (isX) {
      distance = sender.getPosition().x - sender.getRadius();
    } else {
      distance = sender.getPosition().y - sender.getRadius();
    }
    for (int i = idx + 1; i < from.size(); ++i) {
      CommunicationUser user = from.get(i);
      double pos = (isX ? user.getPosition().x : user.getPosition().y);
      if (pos <= distance) {
        toCheck.add(user);
      } else {
        break;
      }
    }
    return toCheck;
  }

  @Override
  public void broadcast(Message message) {
    toBroadcast
        .put(message.sender, new SimpleEntry<Message, Class<? extends CommunicationUser>>(
            message, null));
  }

  @Override
  public void broadcast(Message message, Class<? extends CommunicationUser> type) {
    toBroadcast
        .put(message.sender, new SimpleEntry<Message, Class<? extends CommunicationUser>>(
            message, type));
  }

  private void broadcast2(
      CommunicationUser sender,
      Collection<SimpleEntry<Message, Class<? extends CommunicationUser>>> collection,
      Predicate<CommunicationUser> predicate,
      ArrayList<CommunicationUser> toCheck) {
    if (toCheck.isEmpty()) {
      return;
    }

    ArrayList<CommunicationUser> toCommunicate = toCheck;

    if (toCheck.size() > 100) {
      Collections.sort(toCheck, yComp);
      toCommunicate = select(toCheck, sender, false);
      if (users.isEmpty()) {
        return;
      }
    }
    toCommunicate.remove(sender);

    HashSet<CommunicationUser> uSet = new HashSet<CommunicationUser>(
        toCommunicate.size() / 2);

    for (CommunicationUser u : toCommunicate) {
      if (predicate.apply(u)) {
        uSet.add(u);
      }
    }

    for (CommunicationUser u : uSet) {
      try {
        for (SimpleEntry<Message, Class<? extends CommunicationUser>> p : collection) {
          if (p.getValue() != null && !p.getValue().equals(u.getClass())) {
            continue;
          }
          sendQueue.add(SimpleEntry.entry(u, p.getKey().clone()));
        }
      } catch (CloneNotSupportedException e) {
        LOGGER.error("clonning exception for message", e);
      }
    }
  }

  /**
   * Check if an message from a given sender can be deliver to recipient
   * 
   * @see CanCommunicate#apply(CommunicationUser)
   * @author Bartosz Michalik 
   * @since 2.0
   */
  class CanCommunicate implements Predicate<CommunicationUser> {

    private final Class<? extends CommunicationUser> clazz;
    private final CommunicationUser sender;

    public CanCommunicate(CommunicationUser sender,
        Class<? extends CommunicationUser> clazz) {
      this.sender = sender;
      this.clazz = clazz;
    }

    public CanCommunicate(CommunicationUser sender) {
      this(sender, null);
    }

    @Override
    public boolean apply(CommunicationUser input) {
      if (input == null) {
        return false;
      }
      if (clazz != null && !clazz.equals(input.getClass())) {
        return false;
      }
      // if(input.equals(sender)) return false;
      final Point iPos = input.getPosition();

      double prob = input.getReliability() * sender.getReliability();
      double rand = generator.nextDouble();
      if (prob <= rand) {
        return false;
      }

      double minRadius = Math.min(input.getRadius(), sender.getRadius());
      Point sPos = sender.getPosition();
      return Point.distance(sPos, iPos) <= minRadius;

    }
  }
}
