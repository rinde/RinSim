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
package com.github.rinde.rinsim.core.model;

import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;

/**
 * A model builder that counts how often {@link #build(DependencyProvider)} is
 * called.
 * @author Rinde van Lon
 */
public class CountingModelBuilder
    extends AbstractModelBuilder<AbstractModelVoid, Void> {

  private int counter;

  /**
   * Construct a new instance.
   */
  public CountingModelBuilder() {}

  @Override
  public AbstractModelVoid build(DependencyProvider dependencyProvider) {
    counter++;
    return new NoOpModel();
  }

  /**
   * @return The number of times {@link #build(DependencyProvider)} has been
   *         called.
   */
  public int getCount() {
    return counter;
  }

  static class NoOpModel extends AbstractModelVoid {}
}
