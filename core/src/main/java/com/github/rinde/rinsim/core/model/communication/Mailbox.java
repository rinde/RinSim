package com.github.rinde.rinsim.core.model.communication;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.util.Queue;
import java.util.Set;

/**
 * Simple mailbox with infinite capacity that can be used to serve
 * {@link CommunicationUser#receive(Message)} method.
 * 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
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
