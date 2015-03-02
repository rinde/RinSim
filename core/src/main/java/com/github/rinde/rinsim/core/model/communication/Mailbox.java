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
package com.github.rinde.rinsim.core.model.communication;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Queue;
import java.util.Set;

/**
 * Simple mailbox with infinite capacity that can be used to serve
 * {@link CommunicationUser#receive(Message)} method.
 * 
 * @author Bartosz Michalik 
 * @since 2.0
 */
public class Mailbox {
  protected Set<Message> box;

  public Mailbox() {
    box = newLinkedHashSet();
  }

  /**
   * Insert a msg in the mailbox. If the message is already present in the box
   * it is not inserted again.
   */
  public void receive(Message msg) {
    if (msg == null) {
      throw new NullPointerException();
    }
    if (box.contains(msg)) {
      return;
    }
    box.add(msg);
  }

  /**
   * Getting messages empties the mailbox
   * 
   */
  public Queue<Message> getMessages() {
    final Set<Message> messages = box;
    box = newLinkedHashSet();
    return newLinkedList(messages);
  }
}
