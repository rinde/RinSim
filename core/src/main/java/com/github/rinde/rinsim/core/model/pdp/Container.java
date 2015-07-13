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
package com.github.rinde.rinsim.core.model.pdp;

/**
 * Implementors of this interface can contain 'things', typically {@link Parcel}
 * objects. This interface is typically not used directly, two often used
 * implementations are {@link Vehicle} and {@link Depot}.
 * @author Rinde van Lon
 */
public interface Container extends PDPObject {

  /**
   * The returned value is treated as a constant (i.e. it is read only once).
   * @return The maximum capacity of the container.
   */
  double getCapacity();
}
