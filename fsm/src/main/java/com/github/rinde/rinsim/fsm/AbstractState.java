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
package com.github.rinde.rinsim.fsm;

import javax.annotation.Nullable;

/**
 * Default empty implementation of state. Subclasses only need to implement the
 * {@link AbstractState#handle(Object, Object)} method.
 * @param <E> The event type, see {@link StateMachine} for more information.
 * @param <C> The context type, see {@link StateMachine} for more information.
 * @author Rinde van Lon 
 */
public abstract class AbstractState<E, C> implements State<E, C> {

  @Override
  public String name() {
    return getClass().getName();
  }

  @Nullable
  @Override
  public abstract E handle(@Nullable E event, C context);

  @Override
  public void onEntry(E event, C context) {}

  @Override
  public void onExit(E event, C context) {}

}
