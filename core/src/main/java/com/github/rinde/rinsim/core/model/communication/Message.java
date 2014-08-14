package com.github.rinde.rinsim.core.model.communication;

/**
 * @author Bartosz Michalik 
 * 
 */
public abstract class Message implements Cloneable {
  protected final CommunicationUser sender;

  public Message(CommunicationUser sender) {
    this.sender = sender;
  }

  @Override
  public Message clone() throws CloneNotSupportedException {
    return (Message) super.clone();
  }

  public CommunicationUser getSender() {
    return sender;
  }
}
