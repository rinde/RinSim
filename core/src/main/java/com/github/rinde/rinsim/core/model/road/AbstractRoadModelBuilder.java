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
package com.github.rinde.rinsim.core.model.road;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;

/**
 * Abstract builder for constructing subclasses of {@link RoadModel}.
 *
 * @param <T> The type of the model that the builder is constructing.
 * @param <S> The builder type itself, necessary to make a inheritance-based
 *          builder.
 * @author Rinde van Lon
 */
public abstract class AbstractRoadModelBuilder<T extends RoadModel, S> extends
  AbstractModelBuilder<T, RoadUser> {

  private Unit<Length> distanceUnit;
  private Unit<Velocity> speedUnit;

  /**
   * Create instance.
   */
  protected AbstractRoadModelBuilder() {
    distanceUnit = SI.KILOMETER;
    speedUnit = NonSI.KILOMETERS_PER_HOUR;
  }

  /**
   * Should return the builder itself.
   * @return This.
   */
  protected abstract S self();

  /**
   * Sets the distance unit to for all dimensions. The default is
   * {@link SI#KILOMETER}.
   * @param unit The distance unit to set.
   * @return This, as per the builder pattern.
   */
  public S setDistanceUnit(Unit<Length> unit) {
    distanceUnit = unit;
    return self();
  }

  /**
   * Sets the speed unit to use for all speeds. The default is
   * {@link NonSI#KILOMETERS_PER_HOUR}.
   * @param unit The speed unit to set
   * @return This, as per the builder pattern.
   */
  public S setSpeedUnit(Unit<Velocity> unit) {
    speedUnit = unit;
    return self();
  }

  /**
   * @return the distanceUnit
   */
  public Unit<Length> getDistanceUnit() {
    return distanceUnit;
  }

  /**
   * @return the speedUnit
   */
  public Unit<Velocity> getSpeedUnit() {
    return speedUnit;
  }
}
