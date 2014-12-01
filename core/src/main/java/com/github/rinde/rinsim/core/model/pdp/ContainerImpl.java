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
package com.github.rinde.rinsim.core.model.pdp;

import static com.google.common.base.Preconditions.checkState;

/**
 * Default implementation of the {@link Container} interface.
 * @author Rinde van Lon 
 */
public abstract class ContainerImpl extends PDPObjectImpl implements Container {

  private double capacity;

  /**
   * Sets the capacity. This must be set before the object is registered in its
   * model.
   * @param pCapacity The capacity to use.
   */
  protected final void setCapacity(double pCapacity) {
    checkState(!isRegistered(), "capacity must be set before object is registered, it can not be changed afterwards.");
    capacity = pCapacity;
  }

  @Override
  public final double getCapacity() {
    return capacity;
  }
}
