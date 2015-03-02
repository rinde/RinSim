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
package com.github.rinde.rinsim.fsm;

import javax.annotation.Nullable;

/**
 * Defines a state in a state machine.
 * 
 * @param <E> Event type, see {@link StateMachine} for details.
 * @param <C> Context type, see {@link StateMachine} for details.
 * @author Rinde van Lon 
 */
public interface State<E, C> {

  /**
   * @return The name of the state.
   */
  String name();

  /**
   * Should handle the event. Allows the state to take control and react to the
   * event.
   * @param event The event that should be handled.
   * @param context The context of the state.
   * @return An event or <code>null</code>. If <code>null</code> is returned
   *         there will be no state transition. If the returned event is not
   *         supported by this state (as defined by the {@link StateMachine}) a
   *         {@link RuntimeException} will be thrown by the {@link StateMachine}
   *         .
   */
  @Nullable
  E handle(@Nullable E event, C context);

  /**
   * This method is called at the moment the {@link StateMachine} 'enters' this
   * state (i.e. during a state transition). It is called just before the
   * {@link #handle(Object, Object)} is called.
   * @param event The event that triggered the transition.
   * @param context The context of the state.
   */
  void onEntry(E event, C context);

  /**
   * This method is called at the moment the {@link StateMachine} 'exits' this
   * state (i.e. during a state transition).
   * @param event The event that triggered the transition.
   * @param context The context of the state.
   */
  void onExit(E event, C context);
}
