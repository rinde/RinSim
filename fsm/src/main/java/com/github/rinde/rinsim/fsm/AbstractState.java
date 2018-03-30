/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
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
 * @author Rinde van Lon
 * @param <T> The trigger type, see {@link StateMachine} for more information.
 * @param <C> The context type, see {@link StateMachine} for more information.
 */
public abstract class AbstractState<T, C> implements State<T, C> {

  @Override
  public String name() {
    return getClass().getName();
  }

  @Nullable
  @Override
  public abstract T handle(@Nullable T trigger, C context);

  @Override
  public void onEntry(T trigger, C context) {}

  @Override
  public void onExit(T trigger, C context) {}

}
