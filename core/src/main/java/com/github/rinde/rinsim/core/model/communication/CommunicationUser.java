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

import javax.annotation.Nullable;

import com.github.rinde.rinsim.geom.Point;

/**
 * Defines the interface of the agent that wants to communicate.
 * @author Bartosz Michalik 
 * 
 */
public interface CommunicationUser {

  /**
   * Provide communication API that allows for communication with other object
   * in the simulator. Method is a callback for the registration of the object
   * in {@link CommunicationModel}
   * @param api the access to the communication infrastructure
   */
  void setCommunicationAPI(CommunicationAPI api);

  /**
   * Get position. The position is required to determine the entities you can
   * communicate with
   * @return positing on the communication user or <code>null</code> if object
   *         is not positioned
   */
  @Nullable
  Point getPosition();

  /**
   * Get the distance in which you want to communicate.
   * @return The distance.
   */
  double getRadius();

  /**
   * Get the connection reliability. This is probability (0,1] that the message
   * is send/received. When two entities communicate the probability of message
   * delivery is a product of their reliability.
   * @return The reliability.
   */
  double getReliability();

  /**
   * Receive the message. Multiple messages might be delivered during one tick
   * of the simulator. The simple implementation of handling multiple messages
   * is provided in {@link Mailbox}
   * @param message delivered
   */
  void receive(Message message);
}
