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

/**
 * Communication API exposed to agent to allow them for communication.
 * @author Bartosz Michalik 
 * @since 2.0
 */
public interface CommunicationAPI {
  /**
   * Send the message to a given recipient. Message will be delivered with a
   * specific probability if recipient is within the range (see
   * {@link CommunicationUser} for details).
   * @param recipient
   * @param message
   */
  void send(CommunicationUser recipient, Message message);

  /**
   * Send the message to a given recipient. Message will be delivered with a
   * specific probability to all possible recipients within the range (see
   * {@link CommunicationUser} for details).
   * @param message
   */
  void broadcast(Message message);

  /**
   * Send the message to a given recipient. Message will be delivered with a
   * specific probability to all possible recipients within the range (see
   * {@link CommunicationUser} for details).
   * @param message
   * @param type type of recipient to deliver a message to
   */
  void broadcast(Message message, Class<? extends CommunicationUser> type);
}
