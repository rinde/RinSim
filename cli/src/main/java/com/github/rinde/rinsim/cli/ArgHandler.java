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
package com.github.rinde.rinsim.cli;

import com.google.common.base.Optional;

/**
 * Implementations should handle the activation of an option.
 * @param <S> The type of subject this handler expects.
 * @param <V> The type of argument this handler expects.
 * @author Rinde van Lon
 */
public interface ArgHandler<S, V> {
  /**
   * Is called when an option is activated.
   * @param subject The subject of the handler.
   * @param argument The argument that is passed to the option.
   */
  void execute(S subject, Optional<V> argument);
}
