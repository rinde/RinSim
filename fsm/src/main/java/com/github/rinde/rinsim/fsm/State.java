/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
 * @author Rinde van Lon
 * @param <T> Trigger type, see {@link StateMachine} for details.
 * @param <C> Context type, see {@link StateMachine} for details.
 */
public interface State<T, C> {

  /**
   * @return The name of the state.
   */
  String name();

  /**
   * Should handle the trigger. Allows the state to take control and react to
   * the trigger.
   * @param trigger The trigger that should be handled.
   * @param context The context of the state.
   * @return An trigger or <code>null</code>. If <code>null</code> is returned
   *         there will be no state transition. If the returned trigger is not
   *         supported by this state (as defined by the {@link StateMachine}) a
   *         {@link RuntimeException} will be thrown by the {@link StateMachine}
   *         .
   */
  @Nullable
  T handle(@Nullable T trigger, C context);

  /**
   * This method is called at the moment the {@link StateMachine} 'enters' this
   * state (i.e. during a state transition). It is called just before the
   * {@link #handle(Object, Object)} is called.
   * @param trigger The trigger that triggered the transition.
   * @param context The context of the state.
   */
  void onEntry(T trigger, C context);

  /**
   * This method is called at the moment the {@link StateMachine} 'exits' this
   * state (i.e. during a state transition).
   * @param trigger The trigger that triggered the transition.
   * @param context The context of the state.
   */
  void onExit(T trigger, C context);
}
