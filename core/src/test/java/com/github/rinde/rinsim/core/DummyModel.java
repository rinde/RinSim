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
package com.github.rinde.rinsim.core;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.SimulatorTest.DummyObject;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;

class DummyModel extends AbstractModel<DummyObject> {

  private final Set<DummyObject> objs;

  DummyModel() {
    objs = new LinkedHashSet<>();
  }

  @Override
  public boolean register(DummyObject element) {
    return objs.add(element);
  }

  @Override
  public boolean unregister(DummyObject element) {
    return objs.remove(element);
  }

  public Set<DummyObject> getRegisteredObjects() {
    return Collections.unmodifiableSet(objs);
  }

  @Override
  public String toString() {
    return "DummyModel" + Integer.toHexString(hashCode());
  }

  public static Builder builder() {
    return new Builder();
  }

  static class Builder extends AbstractModelBuilder<DummyModel, DummyObject> {
    @Override
    public DummyModel build(DependencyProvider dependencyProvider) {
      return new DummyModel();
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return other != null && other.getClass() == getClass();
    }
  }
}
